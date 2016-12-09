package godoy;

import org.jtransforms.fft.DoubleFFT_1D;

public class HilbertTransformation {
	
	public HilbertTransformation() {
				
	}
	
	
	/* In-Place-Transformation */
	public void getEnvelope(double[] samples) {
		int newDimension = Utils.nextPowerOfTwo(samples.length);
		
		double[] X = new double[newDimension * 2];
		
		/* In komplexe Samples konvertieren (interleaved, Imaginärteil = 0) */
		for (int xi = 0; xi < samples.length; xi++) {
			X[xi * 2] = samples[xi];			
		}
		
		DoubleFFT_1D fft = new DoubleFFT_1D(newDimension);
		
		fft.complexForward(X);
		
		/* H-Vektor erzeugen */
		double[] H = new double[newDimension];
		int N = newDimension;
		
		for (int i = 0; i < N; i++) {
			int h = i + 1;
			if (h == 1 || h == N / 2 + 1) {
				H[i] = 1;
			}
			else if (h > 1 && h <= N / 2) {
				H[i] = 2;
			}
			else {
				H[i] = 0;
			}
		}
				
		for (int yi = 0; yi < newDimension; yi++) {
			X[yi * 2] *= H[yi];
			X[yi * 2 + 1] *= H[yi];
		}
		
		fft.complexInverse(X, true);
		
		//Als reeles Array überschreiben
		double[] rl = new double[samples.length];
		
		for (int i = 0 ; i < samples.length; i++) {
			rl[i] = X[2 * i];	
		}
		
		for (int i = 0 ; i < samples.length; i++) {
			samples[i] = Math.sqrt(Math.pow(samples[i], 2) + Math.pow(rl[i], 2));
		}		
	}

	/* In-Place-Transformation */
	public void getEnvelope2(double[] array) {
		double [] revan = new double [array.length];

		for (int i =0; i<array.length; i++){

			double sum = 0;

			if(i % 2 == 0){ // i even

				for (int j = 0; j < array.length; j++){

					if (j % 2 == 1){ // j odd

						sum += array[j] / (i - j);

					}

				}

				sum *= 2.0 / Math.PI;

			} else { // i odd

				for (int j = 0; j < array.length; j++){

					if (j % 2 == 0){ // j even

						sum += array[j] / (i - j);

					}

				}

				sum *= 2.0 / Math.PI;

			}

			revan[i] = sum;

		}

		for (int ri = 0; ri < array.length; ri++) {
			array[ri] = Math.sqrt(Math.pow(array[ri], 2) + Math.pow(revan[ri], 2));
		}
	}
}
