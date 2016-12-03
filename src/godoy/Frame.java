package godoy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jtransforms.fft.DoubleFFT_1D;

/**
 * A frame of audio data, represented in the frequency domain. The specific
 * frequency components of this frame are modifiable.
 */
public class Frame {    
    private double[] data, allSamples;
    
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
    
    private final double windowDuration; //s
    
    private int samplesPerWindow;
    
    private double timePosition;
    
    private double pitch;
    
    private int samplesPerPeriod;
    
    private double secondSpectrumOffset;
    
    private static final double DB_REFERENCE = 10;
    
    public Frame(double[] timeData, double pitch, float sampleRate) {

        int frameSize = timeData.length;
        this.pitch = pitch;
        
        windowDuration = Math.max((1 / pitch) / 3, 0.0025); //GODOY

        allSamples = timeData.clone();
        
        samplesPerWindow = (int)(sampleRate * windowDuration);
        
        samplesPerPeriod = (int)((1 / pitch) * sampleRate);
        
        int samplesPerWindowHalf = (int)(samplesPerWindow / 2);
        
        /* FFT für das gesamte Frame */
        DoubleFFT_1D dct = getDctInstance(frameSize);
        
        double[] wholeFrameSamples = timeData.clone();
        windowFuncWholeFrame = new HammingWindowFunction(frameSize);
        windowFuncWholeFrame.applyWindow(wholeFrameSamples);
        
        double[] wholeFrameFFT = wholeFrameSamples.clone();       
        dct.realForward(wholeFrameFFT); //Nur die Hälfte der FFT, negative Nyqist-Frequenz nicht dabei
        
        double[] wholeFrameSpectrum = new double[(int)(wholeFrameFFT.length / 2)];
        
        for (int ffti = 0; ffti < wholeFrameSpectrum.length; ffti++) {
        	 double spectralValue = Math.sqrt(Math.pow(wholeFrameFFT[ffti * 2], 2) + Math.pow(wholeFrameFFT[ffti * 2 + 1], 2));
        	 //in dB umrechnen
        	 spectralValue = 20.0 * Math.log10(spectralValue / DB_REFERENCE);
        	 wholeFrameSpectrum[ffti] = spectralValue;
        }
        
        this.wholeFrameSpectrum = wholeFrameSpectrum;
        
        /* Das erste lokale Maximum finden - ENVELOPE - Todo. */        
        
//      int samplesPerPeriod = (int)((1 / pitch) * sampleRate);
//        
//      if (samplesPerPeriod > frameSize) {
//      	samplesPerPeriod = frameSize;
//      }                
//      
//      double maxSampleInPeriod = Double.NEGATIVE_INFINITY;
//      int positionOfLocalMaximum = -1;
//      
//      for (int i = 0; i < samplesPerPeriod; i++) {
//      	if (allSamples[i] > maxSampleInPeriod) {
//      		maxSampleInPeriod = allSamples[i];
//      		positionOfLocalMaximum = i;
//      	}
//      }
//        
        //Um welchen Teil der Periode soll das zweite Spektrum verschoben werden?
        secondSpectrumOffset = 0.5;
        
        /* FFT für Snapshots */

    	/* Nächste 2-er-Potenz finden für die FFT */
    	int powerOfTwoExpI = 0;
    	while (samplesPerWindow > Math.pow(2, powerOfTwoExpI)) {
    		powerOfTwoExpI++;
    	}
    	
    	int snapshotLengthZeroPadded = (int)(Math.pow(2, powerOfTwoExpI));
    	
        DoubleFFT_1D fftSnapshot = getDctInstance(snapshotLengthZeroPadded);
        
        // Fensterfunktion erzeugen
        windowFuncSnapshot = new HammingWindowFunction(snapshotLengthZeroPadded);                   
        
        for (int i = samplesPerWindowHalf; i < frameSize - samplesPerWindowHalf - (int)((double)secondSpectrumOffset * samplesPerPeriod); i++) {
        	double[] snapshot1 = new double[samplesPerWindow],
        			 snapshot2 = new double[samplesPerWindow];
        	
        	for (int j = 0; j < samplesPerWindow; j++) {
        		snapshot1[j] = allSamples[i - samplesPerWindowHalf + j];
        		snapshot2[j] = allSamples[i - samplesPerWindowHalf + j + (int)((double)secondSpectrumOffset * samplesPerPeriod)];
        	}        	

        	double[] snapshot1ZeroPadded = new double[snapshotLengthZeroPadded],
       			 snapshot2ZeroPadded = new double[snapshotLengthZeroPadded];
        	
        	for (int zi = 0; zi < snapshotLengthZeroPadded; zi++) {
        		snapshot1ZeroPadded[zi] = zi < samplesPerWindow ? snapshot1[zi] : 0;
        		snapshot2ZeroPadded[zi] = zi < samplesPerWindow ? snapshot2[zi] : 0;
        	}
        	
        	windowFuncSnapshot.applyWindow(snapshot1ZeroPadded);
        	windowFuncSnapshot.applyWindow(snapshot2ZeroPadded);
        	
        	snapshots1ByOffset.put(i - samplesPerWindowHalf, snapshot1ZeroPadded);
        	snapshots2ByOffset.put(i - samplesPerWindowHalf, snapshot2ZeroPadded);        	
        	
        	double[] snapshot1fft = snapshot1ZeroPadded.clone(),
        			 snapshot2fft = snapshot2ZeroPadded.clone();
        	
        	fftSnapshot.realForward(snapshot1fft);
        	fftSnapshot.realForward(snapshot2fft);
        	
        	double[] snapshot1Spectrum = new double[(int)(snapshot1fft.length / 2)],
        			 snapshot2Spectrum = new double[(int)(snapshot2fft.length / 2)];
        	
        	for (int ffti = 0; ffti < snapshot1Spectrum.length; ffti++) {
            	 double spectralValue1 = Math.sqrt(Math.pow(snapshot1fft[ffti * 2], 2) + Math.pow(snapshot1fft[ffti * 2 + 1], 2));
            	 //in dB umrechnen
            	 spectralValue1 = 20.0 * Math.log10(spectralValue1 / DB_REFERENCE);
            	 snapshot1Spectrum[ffti] = spectralValue1;
            	 
            	 double spectralValue2 = Math.sqrt(Math.pow(snapshot2fft[ffti * 2], 2) + Math.pow(snapshot2fft[ffti * 2 + 1], 2));
            	 //in dB umrechnen
            	 spectralValue2 = 20.0 * Math.log10(spectralValue2 / DB_REFERENCE);
            	 snapshot2Spectrum[ffti] = spectralValue2;
            }
        	
        	spectrum1sByOffset.put(i - samplesPerWindowHalf, snapshot1Spectrum);
        	spectrum2sByOffset.put(i - samplesPerWindowHalf, snapshot2Spectrum);
        }
         
//        
//        double openPhaseMarginOffset = 0.22222222;
//                
//        //Nur verarbeiten, wenn eine ganze Periode im Frame
//        if (positionOfLocalMaximum >= samplesPerPeriod * openPhaseMarginOffset && positionOfLocalMaximum + samplesPerPeriod < allSamples.length) {
//
//        	//Die Variable "samples" repräsentiert nun eine ganze Periode, verschoben um openPhaseMarginOffset nach links
//        	for (int i = 0; i < samplesPerPeriod; i++) {                 	
//            	samples[i] = allSamples[i + positionOfLocalMaximum - (int)(openPhaseMarginOffset * samplesPerPeriod)];              	
//            }
//        	
//        	//Mit wie vielen Nullen soll gepaddet werden? Die Zahl gibt an, wie viel mal die eigentliche Anzahl Samples erhöht werden soll    
//            int zeroPaddingLengthFactor = 0;            
//            
//            // Zur Anzahl der Samples passende Instanz der FFT-Funktion
//            DoubleFFT_1D dct = getDctInstance(samplesPerWindow * zeroPaddingLengthFactor);
//
//            // Fensterfunktion erzeugen
//            WindowFunction windowFunc = new HammingWindowFunction(samplesPerWindow * zeroPaddingLengthFactor);            
//                        
//            // Um welchen Teil der Periode soll das zweite Spektrum verschoben werden?
//            double secondSpectrumOffset = 0.4;
//            
//            // Die Analyse erfolgt für alle Samples von j=0 bis j = samplesPerPeriod * 
//            for (int j = 0; j < samplesPerPeriod * secondSpectrumOffset - samplesPerWindow; j++) {
//            	double[] samples1 = new double[samplesPerWindow], 
//            	 		 samples2 = new double[samplesPerWindow];
//            	
//            	for (int z = 0; z < samplesPerWindow; z++) {
//            		samples1[z] = samples[z + j];            		
//            		samples2[z] = samples[z + j + (int)(samplesPerPeriod * secondSpectrumOffset)];
//            	}         
//            	
//            	//Zero-Padding vor oder nach Fensterfunktion?
//            	double[] samples1ZeroPadded = new double[samplesPerWindow * zeroPaddingLengthFactor],
//            			 samples2ZeroPadded = new double[samplesPerWindow * zeroPaddingLengthFactor];
//            	
//            	for (int z = 0; z < samplesPerWindow * zeroPaddingLengthFactor; z++) {
//            		if (z < samplesPerWindow) {
//            			samples1ZeroPadded[z] = samples1[z];
//            			samples2ZeroPadded[z] = samples2[z];
//            		}
//            		else {
//            			samples1ZeroPadded[z] = 0;
//            			samples2ZeroPadded[z] = 0;
//            		}
//            	}
//            
//            	windowFunc.applyWindow(samples1ZeroPadded);
//            	windowFunc.applyWindow(samples2ZeroPadded);
//            	
//            	sample1sByOffset.put(j, samples1ZeroPadded);
//            	sample2sByOffset.put(j, samples2ZeroPadded);
//            	
//            	double[] spectrum1 = samples1ZeroPadded.clone();
//            	double[] spectrum2 = samples2ZeroPadded.clone();
//            	
//            	// Transformieren
//            	dct.realForward(spectrum1);
//            	dct.realForward(spectrum2);
//            	
//            	// Das Spektrum aus den Daten extrahieren
//            	int sizeOfSpectrum = spectrum1.length % 2 == 0 ? /* gerade */ spectrum1.length / 2 : /* ungerade */ (spectrum1.length - 1) / 2;
//            	
//            	double[] calculatedSpectrum1 = new double[sizeOfSpectrum - 1],
//            			 calculatedSpectrum2 = new double[sizeOfSpectrum - 1];
//            	
//            	for (int s = 1; s < sizeOfSpectrum; s++) {
//            		double re1 = spectrum1[2 * s],
//            			   im1 = spectrum1[2 * s + 1],
//            			   mag1 = Math.sqrt(re1 * re1 + im1 * im1),
//            			   re2 = spectrum2[2 * s],
//                    	   im2 = spectrum2[2 * s + 1],
//                    	   mag2 = Math.sqrt(re2 * re2 + im2 * im2);
//            		
//            		calculatedSpectrum1[s - 1] = mag1;
//            		calculatedSpectrum2[s - 1] = mag2;
//            			   
//            	}            	
//
//            	//Nur interessierende Frequenzen beachten
//            	ArrayList<Double> relevantSpectrums1 = new ArrayList<Double>(),
//            			   		  relevantSpectrums2 = new ArrayList<Double>();
//            	
//            	int minFrequency = 3000, //Hz
//            		maxFrequency = 8000, //Hz          
//            		k = 1,
//            		currentFrequency = (int)(1.0 * Clip.getClassSamplingRate() / samples1ZeroPadded.length);    
//            	
//            	double basisLevel = 100.0;
//            	
//            	while (currentFrequency < maxFrequency) {
//            		if (currentFrequency > minFrequency) {
//            			//in dB umrechnen            			       
//            			double cs1 = 20.0 * Math.log10(calculatedSpectrum1[k - 1] / basisLevel), 
//            				   cs2 = 20.0 * Math.log10(calculatedSpectrum2[k - 1] / basisLevel);            			
//            			
//            			relevantSpectrums1.add(cs1);
//            			relevantSpectrums2.add(cs2);
//            			
//            			//System.out.println("f=" + currentFrequency + " Hz, cs1=" + cs1 + " dB, cs2=" + cs2 + " dB");
//            		}
//            		k++;
//            		currentFrequency += (int)(Clip.getClassSamplingRate() / samples1ZeroPadded.length);
//            	}
//            	
//            	calculatedSpectrum1 = new double[relevantSpectrums1.size()];
//            	calculatedSpectrum2 = new double[relevantSpectrums2.size()];
//            	
//            	int sz = 0;
//            	
//            	for (double rs : relevantSpectrums1) {
//            		calculatedSpectrum1[sz] = rs;
//            		sz++;
//            	}
//            	
//            	sz = 0;
//            	
//            	for (double rs : relevantSpectrums2) {
//            		calculatedSpectrum2[sz] = rs;
//            		sz++;
//            	}
//            	
//            	spectrum1sByOffset.put(j, calculatedSpectrum1);
//            	spectrum2sByOffset.put(j, calculatedSpectrum2);
//            }            
//            
//        }
        
    }

    private static DoubleFFT_1D getDctInstance(int frameSize) {
    	DoubleFFT_1D dct = dctInstances.get(frameSize);
        if (dct == null) {
            dct = new DoubleFFT_1D(frameSize);
            dctInstances.put(frameSize, dct);
        }
        return dct;
    }
       
    public int getLength() {
        return data.length;
    }
 
    public double getData(int idx) {
        return data[idx];
    }
        
    /* ANALYSE DES VOLLSTÄNDIGEN FRAMES */
    public double[] getAllSamples() {
    	return allSamples;
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

}