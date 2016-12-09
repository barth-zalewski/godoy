package godoy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Analyzer {
	
	private List<Frame> frames;
	private ArrayList<double[]> stDevsByFrame;
	
	private int minFrequency = 3000,
				maxFrequency = 5500;
	
	public Analyzer(List<Frame> data) {
		frames = data;
	}
	
	public void trackStDev() {		
		stDevsByFrame = new ArrayList<double[]>();
		
		for (int i = 0; i < frames.size(); i++) {
			Map<Integer, double[]> spectrums1 = frames.get(i).getSpectrums1();
			Map<Integer, double[]> spectrums2 = frames.get(i).getSpectrums2();
			
			double[] stDevs = new double[spectrums1.size()];
			
			for (int j = 0; j < spectrums1.size(); j++) {
				double[] spectrum1 = spectrums1.get(j),
						 spectrum2 = spectrums2.get(j);
				
				ArrayList<Double> spectralDifferences = new ArrayList<Double>();
				
				for (int s = 0; s < spectrum1.length; s++) {		
					double frequency = (double)s * 0.5 * Clip.getClassSamplingRate() / spectrum1.length;
					if (frequency > minFrequency && frequency < maxFrequency) {
						spectralDifferences.add(spectrum1[s] - spectrum2[s]);
					}
				}
				
				/* Differenzen mittelwertfrei machen */
				double sdMean = 0;
				for (int sd = 0; sd < spectralDifferences.size(); sd++) {
					sdMean += spectralDifferences.get(sd);
				}
				sdMean /= spectralDifferences.size();
				for (int sd = 0; sd < spectralDifferences.size(); sd++) {
					spectralDifferences.set(sd, spectralDifferences.get(sd) - sdMean);
				}
				
				double[] spectralDifferencesArray = new double[spectralDifferences.size()];
				for (int sd = 0; sd < spectralDifferences.size(); sd++) {
					spectralDifferencesArray[sd] = spectralDifferences.get(sd);
				}
				
				stDevs[j] = Utils.standardDeviation(spectralDifferencesArray);				
			}
			
			stDevsByFrame.add(stDevs);
		}			
	}
	
	public ArrayList<double[]> getStDevsByFrame() {
		return stDevsByFrame;
	}
}
