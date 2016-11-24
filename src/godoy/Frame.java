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
    
    private final double windowDuration = 0.001; //s
    
    private double timePosition;
    
    private double pitch;
    
    public Frame(double[] timeData, double pitch, float sampleRate) {

        int frameSize = timeData.length;
        this.pitch = pitch;

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
                
        //Nur verarbeiten, wenn eine ganze Periode im Frame
        if (positionOfLocalMaximum + samplesPerPeriod < allSamples.length) {

        	//Die Variable "samples" repräsentiert nun eine ganze Periode
        	for (int i = 0; i < samplesPerPeriod; i++) {                 	
            	samples[i] = allSamples[i + positionOfLocalMaximum];        	
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
            
//            	windowFunc.applyWindow(samples1);
//            	windowFunc.applyWindow(samples2);
            	
            	sample1sByOffset.put(j, samples1);
            	sample2sByOffset.put(j, samples2);
            }            

//            
//            this.windowFunc = windowFunc;
//            
//            windowFunc.applyWindow(samples);
    //
//            // Transformieren
//            dct.realForward(samples);

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            
            
            
//            
//            data = new double[samples.length];
//            for (int i = 0; i < data.length; i++) {
//                data[i] = timeData[i];
//                min = Math.min(data[i], min);
//                max = Math.max(data[i], max);
//            }
            
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
    
    public void setTimePosition(double tp) {
    	timePosition = tp;
    }
    
    public double getTimePosition() {
    	return timePosition;
    }

}
