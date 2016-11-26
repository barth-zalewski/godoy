package godoy;

import java.util.HashMap;
import java.util.Map;

import org.jtransforms.fft.DoubleFFT_1D;

/**
 * A frame of audio data, represented in the frequency domain. The specific
 * frequency components of this frame are modifiable.
 */
public class Frame {    
    private double[] data, samples, allSamples;

    /**
     * Optimierung: ein Map mit DCT-Instanzen und der Länge, die sie verarbeiten können
     */
    private static Map<Integer, DoubleFFT_1D> dctInstances = new HashMap<Integer, DoubleFFT_1D>();
    
    private Map<Integer, double[]> sample1sByOffset = new HashMap<Integer, double[]>();
    private Map<Integer, double[]> sample2sByOffset = new HashMap<Integer, double[]>();
    
    private Map<Integer, double[]> spectrum1sByOffset = new HashMap<Integer, double[]>();
    private Map<Integer, double[]> spectrum2sByOffset = new HashMap<Integer, double[]>();
    
    private final double windowDuration; //s
    
    private double timePosition;
    
    private double pitch;
    
    public Frame(double[] timeData, double pitch, float sampleRate) {

        int frameSize = timeData.length;
        this.pitch = pitch;
        
        windowDuration = (1 / pitch) / 3; //GODOY

        double[] allSamples = timeData.clone();
        
        int samplesPerWindow = (int)(sampleRate * windowDuration);
        
        /* Das erste lokale Maximum finden */
        int samplesPerPeriod = (int)((1 / pitch) * sampleRate);
        
        if (samplesPerPeriod > frameSize) {
        	samplesPerPeriod = frameSize;
        }                
        
        double maxSampleInPeriod = Double.NEGATIVE_INFINITY;
        int positionOfLocalMaximum = -1;
        
        for (int i = 0; i < samplesPerPeriod; i++) {
        	if (allSamples[i] > maxSampleInPeriod) {
        		maxSampleInPeriod = allSamples[i];
        		positionOfLocalMaximum = i;
        	}
        }
        
        samples = new double[samplesPerPeriod];
        this.allSamples = allSamples;
        
        double openPhaseMarginOffset = 0.33333333333;
                
        //Nur verarbeiten, wenn eine ganze Periode im Frame
        if (positionOfLocalMaximum >= samplesPerPeriod * openPhaseMarginOffset && positionOfLocalMaximum + samplesPerPeriod < allSamples.length) {

        	//Die Variable "samples" repräsentiert nun eine ganze Periode, verschoben um openPhaseMarginOffset nach links
        	for (int i = 0; i < samplesPerPeriod; i++) {                 	
            	samples[i] = allSamples[i + positionOfLocalMaximum - (int)(openPhaseMarginOffset * samplesPerPeriod)];              	
            }
            
            // Zur Anzahl der Samples passende Instanz der FFT-Funktion
            DoubleFFT_1D dct = getDctInstance(samplesPerWindow);

            /* Fensterfunktion anwenden */

            // Fensterfunktion erzeugen
            WindowFunction windowFunc = new HammingWindowFunction(samplesPerWindow);            
                        
            // Die Analyse erfolgt für alle Samples von j=0 bis j = samplesPerPeriod / 2
            for (int j = 0; j < samplesPerPeriod / 2 - samplesPerWindow; j++) {
            	double[] samples1 = new double[samplesPerWindow], 
            	 		 samples2 = new double[samplesPerWindow];
            	
            	for (int z = 0; z < samplesPerWindow; z++) {
            		samples1[z] = samples[z + j];            		
            		samples2[z] = samples[z + j + (int)(samplesPerPeriod / 2)];
            	}                        
            
            	windowFunc.applyWindow(samples1);
            	windowFunc.applyWindow(samples2);
            	
            	sample1sByOffset.put(j, samples1);
            	sample2sByOffset.put(j, samples2);
            	
            	double[] spectrum1 = samples1.clone();
            	double[] spectrum2 = samples2.clone();
            	
            	// Transformieren
            	dct.realForward(spectrum1);
            	dct.realForward(spectrum2);
            	
            	// Das Spektrum aus den Daten extrahieren
            	int sizeOfSpectrum = samples1.length % 2 == 0 ? /* gerade */ samples1.length / 2 : /* ungerade */ (samples1.length - 1) / 2;
            	
            	double[] calculatedSpectrum1 = new double[sizeOfSpectrum - 1],
            			 calculatedSpectrum2 = new double[sizeOfSpectrum - 1];
            	
            	for (int s = 1; s < sizeOfSpectrum; s++) {
            		double re1 = spectrum1[2 * s],
            			   im1 = spectrum1[2 * s + 1],
            			   mag1 = Math.sqrt(re1 * re1 + im1 * im1),
            			   re2 = spectrum2[2 * s],
                    	   im2 = spectrum2[2 * s + 1],
                    	   mag2 = Math.sqrt(re2 * re2 + im2 * im2);
            		
            		calculatedSpectrum1[s - 1] = mag1;
            		calculatedSpectrum2[s - 1] = mag2;
            			   
            	}
            	
            	//In dB umrechnen
            	double basisLevel = 0.00002;
            	
            	for (int z = 0; z < calculatedSpectrum1.length; z++) {
            		calculatedSpectrum1[z] = 20 * Math.log10(calculatedSpectrum1[z] / basisLevel);
            		calculatedSpectrum2[z] = 20 * Math.log10(calculatedSpectrum2[z] / basisLevel);
            	}
            	
            	spectrum1sByOffset.put(j, calculatedSpectrum1);
            	spectrum2sByOffset.put(j, calculatedSpectrum2);
            }            
            
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
    
    public double[] getSamples() {
    	return samples;
    }
    
    public double[] getAllSamples() {
    	return allSamples;
    }
    
    public Map<Integer, double[]> getWindowedSamples1() {
    	return sample1sByOffset;    	
    }
    
    public Map<Integer, double[]> getWindowedSamples2() {
    	return sample2sByOffset;    	
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

}
