package godoy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

            int zeroPaddingLengthFactor = 3;
            
            /* Fensterfunktion anwenden */

            // Fensterfunktion erzeugen
            WindowFunction windowFunc = new HammingWindowFunction(samplesPerWindow * zeroPaddingLengthFactor);            
                        
            // Die Analyse erfolgt für alle Samples von j=0 bis j = samplesPerPeriod / 2
            for (int j = 0; j < samplesPerPeriod / 2 - samplesPerWindow; j++) {
            	double[] samples1 = new double[samplesPerWindow], 
            	 		 samples2 = new double[samplesPerWindow];
            	
            	for (int z = 0; z < samplesPerWindow; z++) {
            		samples1[z] = samples[z + j];            		
            		samples2[z] = samples[z + j + (int)(samplesPerPeriod / 2)];
            	}         
            	
            	//Zero-Padding vor oder nach Fensterfunktion?
            	double[] samples1ZeroPadded = new double[samplesPerWindow * zeroPaddingLengthFactor],
            			 samples2ZeroPadded = new double[samplesPerWindow * zeroPaddingLengthFactor];
            	
            	for (int z = 0; z < samplesPerWindow * zeroPaddingLengthFactor; z++) {
            		if (z < samplesPerWindow) {
            			samples1ZeroPadded[z] = samples1[z];
            			samples2ZeroPadded[z] = samples2[z];
            		}
            		else {
            			samples1ZeroPadded[z] = 0;
            			samples2ZeroPadded[z] = 0;
            		}
            	}
            
            	windowFunc.applyWindow(samples1ZeroPadded);
            	windowFunc.applyWindow(samples2ZeroPadded);
            	
            	sample1sByOffset.put(j, samples1ZeroPadded);
            	sample2sByOffset.put(j, samples2ZeroPadded);
            	
            	double[] spectrum1 = samples1ZeroPadded.clone();
            	double[] spectrum2 = samples2ZeroPadded.clone();
            	
            	// Transformieren
            	dct.realForward(spectrum1);
            	dct.realForward(spectrum2);
            	
            	// Das Spektrum aus den Daten extrahieren
            	int sizeOfSpectrum = spectrum1.length % 2 == 0 ? /* gerade */ spectrum1.length / 2 : /* ungerade */ (spectrum1.length - 1) / 2;
            	
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

            	//Nur interessierende Frequenzen beachten
            	ArrayList<Double> relevantSpectrums1 = new ArrayList<Double>(),
            			   		  relevantSpectrums2 = new ArrayList<Double>();
            	
            	int minFrequency = 1000, //Hz
            		maxFrequency = 6000, //Hz          
            		k = 1,
            		currentFrequency = (int)(1.0 * Clip.getClassSamplingRate() / samples1ZeroPadded.length);    
            	
            	double basisLevel = 20.0;
            	
            	while (currentFrequency < maxFrequency) {
            		if (currentFrequency > minFrequency) {
            			//in dB umrechnen            			       
            			double cs1 = 20.0 * Math.log10(calculatedSpectrum1[k - 1] / basisLevel), 
            				   cs2 = 20.0 * Math.log10(calculatedSpectrum2[k - 1] / basisLevel);            			
            			
            			relevantSpectrums1.add(cs1);
            			relevantSpectrums2.add(cs2);
            			
            			System.out.println("f=" + currentFrequency + " Hz, cs1=" + cs1 + " dB, cs2=" + cs2 + " dB");
            		}
            		k++;
            		currentFrequency += (int)(Clip.getClassSamplingRate() / samples1ZeroPadded.length);
            	}
            	
            	calculatedSpectrum1 = new double[relevantSpectrums1.size()];
            	calculatedSpectrum2 = new double[relevantSpectrums2.size()];
            	
            	int sz = 0;
            	
            	for (double rs : relevantSpectrums1) {
            		calculatedSpectrum1[sz] = rs;
            		sz++;
            	}
            	
            	sz = 0;
            	
            	for (double rs : relevantSpectrums2) {
            		calculatedSpectrum2[sz] = rs;
            		sz++;
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
