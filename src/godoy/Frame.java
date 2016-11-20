package godoy;

import java.util.HashMap;
import java.util.Map;

import org.jtransforms.dct.DoubleDCT_1D;;

/**
 * A frame of audio data, represented in the frequency domain. The specific
 * frequency components of this frame are modifiable.
 */
public class Frame {    
    private double[] data, samples;

    /**
     * Optimierung: ein Map mit DCT-Instanzen und der Länge, die sie verarbeiten können
     */
    private static Map<Integer, DoubleDCT_1D> dctInstances = new HashMap<Integer, DoubleDCT_1D>();
    
    private final WindowFunction windowFunc;
    
    private double timePosition;
    
    public Frame(double[] timeData, WindowFunction windowFunc) {
        this.windowFunc = windowFunc;
        int frameSize = timeData.length;

        samples = timeData.clone();
 
        DoubleDCT_1D dct = getDctInstance(frameSize);

        // in place window
        windowFunc.applyWindow(timeData);

        // in place transform: timeData becomes frequency data
        dct.forward(timeData, true);

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        
        data = new double[frameSize];
        for (int i = 0; i < data.length; i++) {
            data[i] = timeData[i];
            min = Math.min(data[i], min);
            max = Math.max(data[i], max);
        }
        
    }

    private static DoubleDCT_1D getDctInstance(int frameSize) {
        DoubleDCT_1D dct = dctInstances.get(frameSize);
        if (dct == null) {
            dct = new DoubleDCT_1D(frameSize);
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
    
    public void setTimePosition(double tp) {
    	timePosition = tp;
    }
    
    public double getTimePosition() {
    	return timePosition;
    }

}
