package godoy;

import java.util.Arrays;
import java.util.logging.Logger;

public class VorbisWindowFunction implements WindowFunction {

    private static final Logger logger = Logger.getLogger(VorbisWindowFunction.class.getName());
    
    private final double[] scalars;
    
    private static final double PI = Math.PI;
    
    public VorbisWindowFunction(int size) {
        scalars = new double[size];
        for (int i = 0; i < size; i++) {

            // This is the real vorbis one, but it's designed for MDCT where
            // the output array is half the size of the input array
            // double xx = Math.sin( (PI/(2.0*size)) * (i + 0.5) );
            
            double xx = Math.sin((PI / (2.0 * size)) * (2.0 * i));
            scalars[i] = Math.sin((PI / 2.0) * (xx * xx));
        }
        logger.finest(String.format("VorbisWindowFunction scalars (size=%d): %s\n", scalars.length, Arrays.toString(scalars)));
    }
    
    public void applyWindow(double[] data) {
        if (data.length != scalars.length) {
            throw new IllegalArgumentException(
                    "Falsche Arraygr��e (erwartet: " + scalars.length +
                    "; gegeben: " + data.length + ")");
        }
        for (int i = 0; i < data.length; i++) {
            data[i] *= scalars[i];
        }
    }

}
