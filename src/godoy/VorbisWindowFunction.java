package godoy;

public class VorbisWindowFunction implements WindowFunction {

    private final double[] scalars;
    
    private static final double PI = Math.PI;
    
    public VorbisWindowFunction(int size) {
        scalars = new double[size];
        for (int i = 0; i < size; i++) {
            double xx = Math.sin((PI / (2.0 * size)) * (2.0 * i));
            scalars[i] = Math.sin((PI / 2.0) * (xx * xx));
        }        
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
