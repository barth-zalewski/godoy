package godoy;

public class MelFilterbank {
	/* Einstellungen */
	public static final int LOWER_FREQUENCY = 0,
							UPPER_FREQUENCY = 8000,
							NUMBER_OF_FILTERBANKS = 26;
	
	double[] data;
	int[] f;
	
	public MelFilterbank(double[] data) {
		double[] shortenedData = new double[data.length / 2];
		for (int i = 0; i < shortenedData.length; i++) { //Negative Frequenzen verwerfen
			shortenedData[i] = data[i];
		}
		this.data = shortenedData;		
	}
	
	public double[] transform() {
		/* 1. Vektor m in Mel erzeugen */
		double lowerMel = frequencyToMel(LOWER_FREQUENCY),
			   upperMel = frequencyToMel(UPPER_FREQUENCY);
		
		double step = (upperMel - lowerMel) / (NUMBER_OF_FILTERBANKS + 1);
		
		double[] h /* in Mel */ = new double[NUMBER_OF_FILTERBANKS + 2];
		h[0] = lowerMel;
		h[h.length - 1] = upperMel;
		
		for (int i = 0; i < NUMBER_OF_FILTERBANKS; i++) {
			h[i + 1] = h[i] + step;
		}
		
		/* 2. Vektor zurück in Hertz konvertieren */		
		for (int i = 0; i < h.length; i++) {
			h[i] = melToFrequency(h[i]); //Entspricht jetzt dem Vektor h(i) aus dem Tutorial			
		}		
		
		/* 3. Vektor f(i) erzeugen */
		f = new int[NUMBER_OF_FILTERBANKS + 2];
		
		for (int i = 0; i < h.length; i++) {
			f[i] = (int)Math.floor((2 * data.length + 1) * h[i] / Clip.getClassSamplingRate());					
		}
		
		/* 4. Filterbanken erzeugen */
		//Daten in eine 1xN - Matrix umwandeln (N - Länge von data)
		double[][] dataAs2DArray = new double[1][data.length];
		dataAs2DArray[0] = data;
		Matrix dataM = new Matrix(dataAs2DArray);
		
		//Filterbank-Matrix erstellen
		double[][] filterbanks2DArray = new double[data.length][NUMBER_OF_FILTERBANKS];
						
		for (int j = 1; j < f.length - 1; j++) { //Filterbank-Matrix spaltenweise füllen (daher i/j umgedreht)
			int lowerBin = f[j - 1], maxBin = f[j], upperBin = f[j + 1];
			
			for (int i = 0; i < data.length; i++) {
				if (i < lowerBin || i > upperBin) {
					filterbanks2DArray[i][j - 1] = 0;
				}
				else {
					if (i < maxBin) {
						filterbanks2DArray[i][j - 1] = (double)(i - lowerBin) / (maxBin - lowerBin);
					}
					else if (i > maxBin) {
						filterbanks2DArray[i][j - 1] = (double)(upperBin - i) / (upperBin - maxBin);
					}
					else {
						filterbanks2DArray[i][j - 1] = 1;
					}
				}
			}
		}		
		
		Matrix filterbanksM = new Matrix(filterbanks2DArray);
				
		Matrix resultM = dataM.times(filterbanksM);
		
		return resultM.getArrayCopy()[0];
	}
	
	private double frequencyToMel(double f) {
		return 1125.0 * Math.log(1.0 + f / 700);
	}
	
	private double frequencyToMel(int f) {
		return frequencyToMel((double)f);
	}
	
	private double melToFrequency(double m) {
		return 700 * (Math.exp(m / 1125) - 1);
	}
	
	
}
