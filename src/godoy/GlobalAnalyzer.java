package godoy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GlobalAnalyzer {
	private static Map<Double, int[]> histogramm2D = new HashMap<Double, int[]>();
	
	private static Map<Double, ArrayList<int[]>> histogrammRows = new HashMap<Double, ArrayList<int[]>>();
	
	public static void addHistogrammRow(int[] histogrammRow, double spectrumOffset) {
		if (histogrammRows.get(spectrumOffset) == null) {
			histogrammRows.put(spectrumOffset, new ArrayList<int[]>());
		}
		
		histogrammRows.get(spectrumOffset).add(histogrammRow);
	}
	
	public static Map<Double, int[]> getHistogramm2D() {
		
		return histogramm2D;
	}
}
