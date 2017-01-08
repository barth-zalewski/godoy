package godoy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class GlobalAnalyzer {
	private static LinkedHashMap<Double, double[]> histogramm2D = new LinkedHashMap<Double, double[]>();
	
	private static LinkedHashMap<Double, ArrayList<double[]>> histogrammRows = new LinkedHashMap<Double, ArrayList<double[]>>();
	
	public static void addHistogrammRow(double[] histogrammRow, double spectrumOffset) {
		if (histogrammRows.get(spectrumOffset) == null) {
			histogrammRows.put(spectrumOffset, new ArrayList<double[]>());
		}
		
		histogrammRows.get(spectrumOffset).add(histogrammRow);
	}
	
	public static LinkedHashMap<Double, double[]> getHistogramm2D() {
		for (HashMap.Entry<Double, ArrayList<double[]>> entry : histogrammRows.entrySet()) {
			double spectrumOffset = entry.getKey();
			ArrayList<double[]> histogramms = entry.getValue();
			
			if (histogramm2D.get(spectrumOffset) == null) {
				histogramm2D.put(spectrumOffset, new double[20]); //5-%-Schritte
			}
			
			for (int i = 0; i < histogramms.size(); i++) {
				double[] histogramm = histogramms.get(i);
				for (int j = 0; j < histogramm.length; j++) {
					int index5 = j / 5; //Gruppierung in 5-%-Bins
					histogramm2D.get(spectrumOffset)[index5] += histogramm[j];
				}
			}
		}
		return histogramm2D;
	}
}
