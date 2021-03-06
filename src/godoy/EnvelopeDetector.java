package godoy;

import org.jtransforms.fft.DoubleFFT_1D;

public class EnvelopeDetector {
	
	public EnvelopeDetector() {
				
	}
	
	/* In-Place-Transformation */
	public void getEnvelope(double[] samples) {
		//localMaximumBasedEnvelopeDetector(samples);
		analyticSignalViaHilbert(samples);
	}	
	
	/* Analytisches Signal mit der Hilber-Transformation */
	private void analyticSignalViaHilbert(double[] samples) {
		int newDimension = Utils.nextPowerOfTwo(samples.length);
		
		double[] X = new double[newDimension * 2];
		
		/* In komplexe Samples konvertieren (interleaved, Imaginärteil = 0) */
		for (int xi = 0; xi < samples.length; xi++) {
			X[xi * 2] = samples[xi];			
		}
		
		DoubleFFT_1D fft = new DoubleFFT_1D(newDimension);
		
		fft.complexForward(X);
		
		for (int i = 0; i < newDimension; i++) {
			if (i == 0 || i == newDimension / 2) {
				continue;
			}
			
			double[] z = { X[i * 2], X[i * 2 + 1] };
			
			if (i < newDimension / 2) {
				rotate90DegreesNegative(z);				
			}
			else {
				rotate90DegreesPositive(z);	
			}
			X[i * 2] = z[0];
			X[i * 2 + 1] = z[1];
		}
		
		/* H-Vektor erzeugen */
//		double[] H = new double[newDimension];
//		int N = newDimension;
//		
//		for (int i = 0; i < N; i++) {
//			int h = i + 1;
//			if (h == 1 || h == N / 2 + 1) {
//				H[i] = 1;
//			}
//			else if (h > 1 && h <= N / 2) {
//				H[i] = 2;
//			}
//			else {
//				H[i] = 0;
//			}
//		}
//				
//		for (int yi = 0; yi < newDimension; yi++) {
//			X[yi * 2] *= H[yi];
//			X[yi * 2 + 1] *= H[yi];
//		}
		
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
	
	public void rotate90DegreesPositive(double[] z) {
		double re = z[0], im = z[1];
		z[0] = -im;
		z[1] = re;
	}
	
	public void rotate90DegreesNegative(double[] z) {
		double re = z[0], im = z[1];
		z[0] = im;
		z[1] = -re;
	}

}