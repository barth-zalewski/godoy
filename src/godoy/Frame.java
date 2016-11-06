package godoy;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_1D;

/**
 * A frame of audio data, represented in the frequency domain. The specific
 * frequency components of this frame are modifiable.
 */
public class Frame {
    
    private static final Logger logger = Logger.getLogger(Frame.class.getName());

    /**
     * Array of spectral data.
     */
    private double[] data;

    /**
     * Maps frame size to the DCT instance that handles that size.
     */
    private static Map<Integer, DoubleDCT_1D> dctInstances = new HashMap<Integer, DoubleDCT_1D>();
    
    private final WindowFunction windowFunc;
    
    public Frame(double[] timeData, WindowFunction windowFunc) {
        this.windowFunc = windowFunc;
        int frameSize = timeData.length;
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
    
    /**
     * Returns the length of this frame, in samples.
     * @return
     */
    public int getLength() {
        return data.length;
    }
    
    /**
     * Returns the idx'th real component of this frame's spectrum.
     */
    public double getReal(int idx) {
        return data[idx];
    }

    /**
     * Returns the idx'th imaginary component of this frame's spectrum.
     */
    public double getImag(int idx) {
        return 0.0;
    }

    /**
     * Sets the real component at idx. This method sets the new actual value,
     * although it may make sense to provide another method that scales the existing
     * value.
     * 
     * @param idx The index to modify
     * @param d The new value
     */
    public void setReal(int idx, double d) {
        data[idx] = d;
    }

    /**
     * Returns the time-domain representation of this frame. Unless the spectral
     * data of this frame has been modified, the returned array will be very
     * similar to the array given in the constructor. Even if the spectral data
     * has been modified, the length of the returned array will have the same
     * length as the original array given in the constructor.
     */
    public double[] asTimeData() {
        double[] timeData = new double[data.length];
        System.arraycopy(data, 0, timeData, 0, data.length);
        DoubleDCT_1D dct = getDctInstance(data.length);
        dct.inverse(timeData, true);
        windowFunc.applyWindow(timeData);
        return timeData;
    }

}
