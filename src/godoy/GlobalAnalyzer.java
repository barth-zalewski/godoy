package godoy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class GlobalAnalyzer {
	private static LinkedHashMap<Double, int[]> histogramm2D = new LinkedHashMap<Double, int[]>();
	
	private static LinkedHashMap<Double, ArrayList<int[]>> histogrammRows = new LinkedHashMap<Double, ArrayList<int[]>>();
	
	public static void addHistogrammRow(int[] histogrammRow, double spectrumOffset) {
		if (histogrammRows.get(spectrumOffset) == null) {
			histogrammRows.put(spectrumOffset, new ArrayList<int[]>());
		}
		
		histogrammRows.get(spectrumOffset).add(histogrammRow);
	}
	
	public static LinkedHashMap<Double, int[]> getHistogramm2D() {
		for (HashMap.Entry<Double, ArrayList<int[]>> entry : histogrammRows.entrySet()) {
			double spectrumOffset = entry.getKey();
			ArrayList<int[]> histogramms = entry.getValue();
			
			if (histogramm2D.get(spectrumOffset) == null) {
				histogramm2D.put(spectrumOffset, new int[100]);
			}
			
			for (int i = 0; i < histogramms.size(); i++) {
				int[] histogramm = histogramms.get(i);
				for (int j = 0; j < histogramm.length; j++) {
					histogramm2D.get(spectrumOffset)[j] += histogramm[j];
				}
			}
		}
		return histogramm2D;
	}
}
