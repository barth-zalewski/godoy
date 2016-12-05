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
    
    private static final double DB_REFERENCE = 0.001;
    
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
        	
        	for (int zi = 0; zi < snapshotLengthZeroPadded; zi++) {
        		snapshot1ZeroPadded[zi] = zi < samplesPerWindow ? snapshot1[zi] : 0;
        		snapshot2ZeroPadded[zi] = zi < samplesPerWindow ? snapshot2[zi] : 0;
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
        		 if (i - samplesPerWindowHalf == 0 || i - samplesPerWindowHalf == 1)
        		 System.out.println("freq=" + (ffti * Clip.getClassSamplingRate() / snapshot1fft.length) + " Hz, re=" + snapshot1fft[ffti * 2] + ", im=" + snapshot1fft[ffti * 2 + 1] + ", mag=" + Math.sqrt(Math.pow(snapshot1fft[ffti * 2], 2) + Math.pow(snapshot1fft[ffti * 2 + 1], 2)));
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