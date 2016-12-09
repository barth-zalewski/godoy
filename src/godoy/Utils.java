package godoy;

public class Utils {
	
	public static boolean isPowerOfTwo(int value){
		return Integer.bitCount(value)==1;
	}
	
	public static int nextPowerOfTwo(int n) {
		if (isPowerOfTwo(n)) {
			return n * 2;
		}
		else {
			int i = 2;
			while (i <= n) {
				i *= 2;
			}
			return i * 2;
		}
	}
	
	public static double standardDeviation(double[] data) {
		double avg = 0;
		
		for (int d = 0; d < data.length; d++) {			
			avg += data[d];
		}
		
		avg /= data.length;		
		
		double squaredSum = 0;
		
		for (int d = 0; d < data.length; d++) {
			squaredSum += Math.pow(data[d] - avg, 2);
		}
		
		return Math.sqrt(squaredSum * (1.0 / (data.length - 1)));
	}
}
