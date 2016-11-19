package godoy;

public class HammingWindowFunction implements WindowFunction {

    private final double[] scalars;
    
    private static final double PI = Math.PI, ALPHA = 0.54, BETA = 1 - ALPHA;
    
    public HammingWindowFunction(int size) {
        scalars = new double[size];
        for (int i = 0; i < size; i++) {
            scalars[i] = ALPHA - BETA * Math.cos((2 * PI * i) / (size - 1));
        }        
    }
    
    public void applyWindow(double[] data) {
        if (data.length != scalars.length) {
            throw new IllegalArgumentException(
                    "Falsche Arraygröße (erwartet: " + scalars.length +
                    "; gegeben: " + data.length + ")");
        }
        for (int i = 0; i < data.length; i++) {
            data[i] *= scalars[i];
        }
    }

}
