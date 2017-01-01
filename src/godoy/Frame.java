package godoy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jtransforms.fft.DoubleFFT_1D;
import org.jtransforms.dct.DoubleDCT_1D;

/**
 * A frame of audio data, represented in the frequency domain. The specific
 * frequency components of this frame are modifiable.
 */
public class Frame {    
	
	public static final int KEEP_MFFC_COEFFICIENTS = 18;
	
    private double[] allSamples;
    
    private int[] periodStartingPoints;
    
    /**
     * Optimierung: ein Map mit DCT-Instanzen und der Länge, die sie verarbeiten können
     */
    private static Map<Integer, DoubleFFT_1D> dctInstances = new HashMap<Integer, DoubleFFT_1D>();
    
    private WindowFunction windowFuncSnapshot;
    
    private Map<Integer, double[]> snapshots1ByOffset = new HashMap<Integer, double[]>();
    private Map<Integer, double[]> snapshots2ByOffset = new HashMap<Integer, double[]>();
    
    private Map<Integer, double[]> spectrum1sByOffset = new HashMap<Integer, double[]>();
    private Map<Integer, double[]> spectrum2sByOffset = new HashMap<Integer, double[]>();
    
    private WindowFunction windowFuncWholeFrame;
    private double[] wholeFrameSpectrum;
    private double[] envelope;
    
    private final double windowDuration; //s
    
    private int samplesPerWindow;
    
    private double timePosition;
    
    private double pitch;
    
    private int samplesPerPeriod;
    
    private double secondSpectrumOffset;
    
    float sampleRate;
    
    private static final double DB_REFERENCE = 1E3;
    
    public Frame(double[] timeData, double pitch, float sampleRate, double secondSpectrumOffset) {

        int frameSize = timeData.length;
        this.pitch = pitch;
        this.secondSpectrumOffset = secondSpectrumOffset;
        this.sampleRate = sampleRate;
        
        windowDuration = Math.max((1 / pitch) / 3, 0.0025); //GODOY
        
        allSamples = timeData.clone();
        
        samplesPerWindow = (int)(sampleRate * windowDuration);
        
        samplesPerPeriod = (int)((1 / pitch) * sampleRate);
        
        int samplesPerWindowHalf = (int)(samplesPerWindow / 2);
        
        double[] wholeFrameSamples = timeData.clone();

        /* Envelope */
        envelope = wholeFrameSamples.clone();
        EnvelopeDetector envelopeDetector = new EnvelopeDetector();
        
        envelopeDetector.getEnvelope(envelope);        
        
        windowFuncWholeFrame = new HammingWindowFunction(frameSize);
        windowFuncWholeFrame.applyWindow(wholeFrameSamples);
        
        /* FFT für das gesamte Frame */
        int fftSizeFrame = Utils.nextPowerOfTwo(frameSize);
        DoubleFFT_1D fft = getFFTInstance(fftSizeFrame);        
        
        double[] wholeFrameFFT = new double[fftSizeFrame * 2];
        
        /* In komplexe Samples konvertieren (interleaved, Imaginärteil = 0) */
        for (int wt = 0; wt < wholeFrameSamples.length; wt++) {
        	wholeFrameFFT[wt * 2] = wholeFrameSamples[wt];
        }
        fft.complexForward(wholeFrameFFT); 
        
        double[] wholeFrameSpectrum = new double[fftSizeFrame];        
        
        for (int ffti = 0; ffti < wholeFrameSpectrum.length; ffti++) {
        	 double spectralValue = Math.sqrt(Math.pow(wholeFrameFFT[ffti * 2], 2) + Math.pow(wholeFrameFFT[ffti * 2 + 1], 2));        	 
        	 wholeFrameSpectrum[ffti] = spectralValue;
        }
        
        this.wholeFrameSpectrum = wholeFrameSpectrum;
        
        /* Lokale Maxima finden */
        periodStartingPoints = new int[allSamples.length]; //1 wenn ein lokales Maximum, sonst 0
        int samplesPerPeriod = (int)((1 / pitch) * sampleRate);
        
        if (samplesPerPeriod > frameSize) {
        	samplesPerPeriod = frameSize;
        }                
        
        int currentSampleForMaxima = 0;
        
        while (currentSampleForMaxima < allSamples.length) {
        	double localMaximum = Double.NEGATIVE_INFINITY;
        	int foundPositionOfLocalMaximum = -1;
        	for (int zmx = currentSampleForMaxima; zmx < Math.min(currentSampleForMaxima + samplesPerPeriod * 0.8, envelope.length); zmx++) { //- 20, um nicht aus Versehen die nächste Periode zu erfassen
        		if (envelope[zmx] > localMaximum) {
        			foundPositionOfLocalMaximum = zmx;
        			localMaximum = envelope[zmx];
        		}
        	}
        	periodStartingPoints[foundPositionOfLocalMaximum] = 1;
        	currentSampleForMaxima = foundPositionOfLocalMaximum + (int)(samplesPerPeriod * 0.8);
        }
      
        /* FFT für Snapshots */
    	int snapshotLengthZeroPadded = 512; //(int)(Math.pow(2, powerOfTwoExpI + 2));
    	
        DoubleFFT_1D fftSnapshot = getFFTInstance(snapshotLengthZeroPadded);
        
        // Fensterfunktion erzeugen
        windowFuncSnapshot = new HammingWindowFunction(samplesPerWindow);                   
        
        for (int i = samplesPerWindowHalf; i < frameSize - samplesPerWindowHalf - (int)((double)secondSpectrumOffset * samplesPerPeriod); i++) {
        	double[] snapshot1 = new double[samplesPerWindow],
        			 snapshot2 = new double[samplesPerWindow];
        	
        	for (int j = 0; j < samplesPerWindow; j++) {
        		snapshot1[j] = allSamples[i - samplesPerWindowHalf + j];
        		snapshot2[j] = allSamples[i - samplesPerWindowHalf + j + (int)((double)secondSpectrumOffset * samplesPerPeriod)];        		
        	}             	
        	
        	windowFuncSnapshot.applyWindow(snapshot1);
        	windowFuncSnapshot.applyWindow(snapshot2);

        	double[] snapshot1ZeroPadded = new double[snapshotLengthZeroPadded],
       			 snapshot2ZeroPadded = new double[snapshotLengthZeroPadded];
        	
        	int maxZiFromLeft =  (snapshotLengthZeroPadded - samplesPerWindow) / 2;
        	int minZiFromRight = maxZiFromLeft + samplesPerWindow;
        	
        	for (int zi = 0; zi < snapshotLengthZeroPadded; zi++) {
        		snapshot1ZeroPadded[zi] = zi > maxZiFromLeft && zi < minZiFromRight ? snapshot1[zi - maxZiFromLeft] : 0;
        		snapshot2ZeroPadded[zi] = zi > maxZiFromLeft && zi < minZiFromRight ? snapshot2[zi - maxZiFromLeft] : 0;
        	}        	
        	
        	snapshots1ByOffset.put(i - samplesPerWindowHalf, snapshot1ZeroPadded);
        	snapshots2ByOffset.put(i - samplesPerWindowHalf, snapshot2ZeroPadded);        	
        	
        	double[] snapshot1fft = snapshot1ZeroPadded.clone(),
        			 snapshot2fft = snapshot2ZeroPadded.clone();
        	
        	fftSnapshot.realForward(snapshot1fft);
        	fftSnapshot.realForward(snapshot2fft);
        	
        	double[] snapshot1Spectrum = new double[(int)(snapshot1fft.length / 2)],
        			 snapshot2Spectrum = new double[(int)(snapshot2fft.length / 2)];
        	
        	for (int ffti = 0; ffti < snapshot1Spectrum.length; ffti++) {        		
        		 double spectralValue1, spectralValue2;
        		 /* Die ersten beiden Zahlen sind besonders */
//        		 if (ffti == 0) {
//        			 spectralValue1 = 1; //Gleichanteil 
//        			 spectralValue2 = 1;
//        		 }
//        		 else {
        			 spectralValue1 = Math.sqrt(Math.pow(snapshot1fft[ffti * 2], 2) + Math.pow(snapshot1fft[ffti * 2 + 1], 2));
        			 spectralValue2 = Math.sqrt(Math.pow(snapshot2fft[ffti * 2], 2) + Math.pow(snapshot2fft[ffti * 2 + 1], 2));
//        		 }
        		 
            	 //in dB umrechnen
            	 //spectralValue1 = 20.0 * Math.log10(spectralValue1 / DB_REFERENCE);
            	 snapshot1Spectrum[ffti] = spectralValue1;
            	   
            	 //spectralValue2 = 20.0 * Math.log10(spectralValue2 / DB_REFERENCE);
            	 snapshot2Spectrum[ffti] = spectralValue2;            	 

            }
        	
        	spectrum1sByOffset.put(i - samplesPerWindowHalf, snapshot1Spectrum);
        	spectrum2sByOffset.put(i - samplesPerWindowHalf, snapshot2Spectrum);
        }        
        
    }

    private static DoubleFFT_1D getFFTInstance(int frameSize) {
    	DoubleFFT_1D dct = dctInstances.get(frameSize);
        if (dct == null) {
            dct = new DoubleFFT_1D(frameSize);
            dctInstances.put(frameSize, dct);
        }
        return dct;
    }
       
    /* ANALYSE DES VOLLSTÄNDIGEN FRAMES */
    public double[] getAllSamples() {
    	return allSamples;
    }
    
    public double[] getEnvelope() {
    	return envelope;
    }
    
    public double[] getWholeFrameSpectrum() {
    	return wholeFrameSpectrum;
    }
    
    public WindowFunction getWindowFuncWholeFrame() {
    	return windowFuncWholeFrame;
    }
    
    
    /* ULTRAKURZZEITANALYSE */
    public Map<Integer, double[]> getSnapshots1() {
    	return snapshots1ByOffset;    	
    }
    
    public Map<Integer, double[]> getSnapshots2() {
    	return snapshots2ByOffset;    	
    }
    
    public Map<Integer, double[]> getSpectrums1() {
    	return spectrum1sByOffset;    	
    }
    
    public Map<Integer, double[]> getSpectrums2() {
    	return spectrum2sByOffset;    	
    }
    
    public void setTimePosition(double tp) {
    	timePosition = tp;
    }
    
    public double getTimePosition() {
    	return timePosition;
    }  
    
    public int getSamplesPerWindow() {
    	return samplesPerWindow;
    }
    
    public int getSamplesPerPeriod() {
    	return samplesPerPeriod;
    }
    
    public double getSecondSpectrumOffset() {
    	return secondSpectrumOffset;
    }
    
    public WindowFunction getWindowFuncSnapshot() {
    	return windowFuncSnapshot;
    }
    
    public int[] getPeriodStartingPoints() {
    	return periodStartingPoints;
    }
    
    public double getPitch() {
    	return pitch;
    }
    
    /* Analyse / Erkennung */
    public ArrayList<double[]> getDCTCoeffiencts() {
    	ArrayList<double[]> ret = new ArrayList<double[]>();
    	
    	int samplesAfterPeriodStart = (int)(samplesPerPeriod * godoy.T_ANALYSIS_OFFSET);    	
    	for (int psp = 0; psp < periodStartingPoints.length; psp++) {
    		if (periodStartingPoints[psp] == 1) {    		    	
				double[] spectrum1 = spectrum1sByOffset.get(psp + samplesAfterPeriodStart),
						 spectrum2 = spectrum2sByOffset.get(psp + samplesAfterPeriodStart);
				
				if (spectrum1 == null) {
					break;
				}
				
				ArrayList<Double> spectralDifferences = new ArrayList<Double>();
				
				/* Spektrale Differenz an einem Periodenanfangszeitpunkt berechnen */
				for (int s = 0; s < spectrum1.length; s++) {		
					double frequency = (double)s * 0.5 * Clip.getClassSamplingRate() / spectrum1.length;
					if (frequency > godoy.MINIMAL_RELEVANT_FREQUENCY && frequency < godoy.MAXIMAL_RELEVANT_FREQUENCY) {
						spectralDifferences.add(spectrum1[s] - spectrum2[s]);
					}
				}
				
				double[] spectralDifferencesArray = new double[spectralDifferences.size()];
				
				for (int d = 0; d < spectralDifferences.size(); d++) {
					spectralDifferencesArray[d] = spectralDifferences.get(d);
				}
				
				double[] sdaDCT = spectralDifferencesArray.clone();
				DoubleDCT_1D dct = new DoubleDCT_1D(sdaDCT.length);
				
				dct.forward(sdaDCT, true);
				
				double[] sdaDCTShortened = new double[godoy.NUMBER_FIRST_DCT_COEFFICIENTS_FOR_CHARACTERISTICS_VECTOR];
				
				for (int dd = 0; dd < Math.min(sdaDCTShortened.length, sdaDCT.length); dd++) {
					sdaDCTShortened[dd] = sdaDCT[dd];
				}
				
				ret.add(sdaDCTShortened);			
    		}
    	}
    	
    	return ret;
    }

    public ArrayList<double[]> getDiffValues() {
    	ArrayList<double[]> ret = new ArrayList<double[]>();
    	
    	int samplesAfterPeriodStart = (int)(samplesPerPeriod * godoy.T_ANALYSIS_OFFSET);    	
    	for (int psp = 0; psp < periodStartingPoints.length; psp++) {
    		if (periodStartingPoints[psp] == 1) {    		    	
				double[] spectrum1 = spectrum1sByOffset.get(psp + samplesAfterPeriodStart),
						 spectrum2 = spectrum2sByOffset.get(psp + samplesAfterPeriodStart);
				
				if (spectrum1 == null) {
					break;
				}
				
				ArrayList<Double> spectralDifferences = new ArrayList<Double>();
				
				/* Spektrale Differenz an einem Periodenanfangszeitpunkt berechnen */
				for (int s = 0; s < spectrum1.length; s++) {		
					double frequency = (double)s * 0.5 * Clip.getClassSamplingRate() / spectrum1.length;
					if (frequency > godoy.MINIMAL_RELEVANT_FREQUENCY && frequency < godoy.MAXIMAL_RELEVANT_FREQUENCY) {
						spectralDifferences.add(spectrum1[s] - spectrum2[s]);
					}
				}
				
				double[] spectralDifferencesArray = new double[spectralDifferences.size()];
				
				for (int d = 0; d < spectralDifferences.size(); d++) {
					spectralDifferencesArray[d] = spectralDifferences.get(d);
				}
				
				ret.add(spectralDifferencesArray);			
    		}
    	}
    	
    	return ret;
    }

    public ArrayList<double[]> getPeaksCoordinates(int percentage) {
    	double p = percentage / 100;
    	
        int samplesAfterPeriodStart = (int)(p * samplesPerPeriod);
        
        /* 0: pitch, 1: percentage, 2: frequency of peak, 3: peak size */
        ArrayList<double[]> ret = new ArrayList<double[]>();
        ArrayList<double[]> all = new ArrayList<double[]>();
        
        double[] sums = new double[4];
        
        for (int psp = 0; psp < periodStartingPoints.length; psp++) {
    		if (periodStartingPoints[psp] == 1) {    		    	
				double[] spectrum1 = spectrum1sByOffset.get(psp + samplesAfterPeriodStart),
						 spectrum2 = spectrum2sByOffset.get(psp + samplesAfterPeriodStart);
				
				if (spectrum1 == null) {
					break;
				}
				
				/* Spektrale Differenz an einem Periodenanfangszeitpunkt berechnen */
				double frequencyOfPeak = 0, maxDiff = Double.NEGATIVE_INFINITY;
				
				for (int s = 0; s < spectrum1.length; s++) {		
					double frequency = (double)s * 0.5 * Clip.getClassSamplingRate() / spectrum1.length;
					if (frequency > godoy.MINIMAL_RELEVANT_FREQUENCY && frequency < godoy.MAXIMAL_RELEVANT_FREQUENCY) {
						double spectralDiff = spectrum1[s] - spectrum2[s];
						if (spectralDiff > maxDiff) {
							maxDiff = spectralDiff;
							frequencyOfPeak = frequency;
						}
					}
				}
				
				double[] coordinates = new double[4];
				coordinates[0] = pitch;
				coordinates[1] = percentage;
				coordinates[2] = frequencyOfPeak;
				coordinates[3] = maxDiff;
				
				sums[0] += 100.0 * pitch;
				sums[1] += 0.5 * percentage;
				sums[2] += 100.0 * frequencyOfPeak;
				sums[3] += 0.5 * maxDiff;
				
				//System.out.println("pitch=" + pitch + ",perc=" + percentage + ",f=" + frequencyOfPeak + ",diff=" + maxDiff);
								
				all.add(coordinates);			
    		}
    	}
        
        double[] res = { sums[0] / all.size(), sums[1] / all.size(), sums[2] / all.size(), sums[3] / all.size() };
        ret.add(res);
        
        return ret;
    }
    
    public ArrayList<double[]> getMFCCCoeffiencts() {
    	ArrayList<double[]> ret = new ArrayList<double[]>();
    	
    	/* Schritt 1 + 2: DFT des ganzen Frames ist schon gegeben, wir machen daraus ein "Periodogram estimate" */
    	double[] periodogramEstimate = new double[wholeFrameSpectrum.length];
    	
    	for (int wfs = 0; wfs < wholeFrameSpectrum.length; wfs++) {
    		periodogramEstimate[wfs] = (1.0 / wholeFrameSpectrum.length) * Math.pow(wholeFrameSpectrum[wfs], 2); // (1 / N) * S_i ^2
    	}
    	
    	/* Schritt 3: Mel-Filterbank */
    	MelFilterbank melFilterbank = new MelFilterbank(periodogramEstimate); /* Rein: Periodogramm der Länge = FFT-Länge */
    	double[] melTransformedData = melFilterbank.transform(); /* Raus: Spektrum bearbeitet mit der Mel-Filterbank der Länge <Anzahl Filterbanken> */
    	
    	
    	/* Schritt 4: Logarithmierung */
    	for (int m = 0; m < melTransformedData.length; m++) {
    		melTransformedData[m] = Math.log10(melTransformedData[m]);
    	}    	
    	
    	/* Schritt 5: DCT (Cepstrum) */
    	DoubleDCT_1D dct = new DoubleDCT_1D(melTransformedData.length);
    	
    	dct.forward(melTransformedData, true);
    	
    	double[] finalMFCC = new double[KEEP_MFFC_COEFFICIENTS];    	
    	
    	for (int i = 0; i < finalMFCC.length; i++) {
    		finalMFCC[i] = melTransformedData[i];
    	}    	
    	
    	/* Nur ein Merkmalvektor pro Frame */
    	ret.add(finalMFCC);
    	
    	return ret;
    }
}