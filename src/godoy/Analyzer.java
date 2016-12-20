package godoy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* Analyzer für einzelne Clips */
public class Analyzer {
	
	private List<Frame> frames;
	private ArrayList<double[]> peaksByFrame;
	int[] histogramm;
	
	private int minFrequency = 3000,
				maxFrequency = 6000;
	
	public Analyzer(List<Frame> data) {
		frames = data;		
	}	
	
	public void trackPeaks() {		
		peaksByFrame = new ArrayList<double[]>();
		
		for (int i = 0; i < frames.size(); i++) {
			Map<Integer, double[]> spectrums1 = frames.get(i).getSpectrums1();
			Map<Integer, double[]> spectrums2 = frames.get(i).getSpectrums2();
			
			double[] peaks = new double[spectrums1.size()];
			
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
				
				//Existiert ein positiver Peak größer als X% des nächsten Wertes?
				double maxPeak = Double.NEGATIVE_INFINITY, secondMaxPeak = maxPeak;
				
				for (int sd = 0; sd < spectralDifferencesArray.length; sd++) {
					if (maxPeak < spectralDifferencesArray[sd]) {
						secondMaxPeak = maxPeak;
						maxPeak = spectralDifferencesArray[sd];						
					}
				}							
				
				peaks[j] = secondMaxPeak / maxPeak < 0.8 ? 1 : 0;
			}
			
			peaksByFrame.add(peaks);
		}			
	}	
	
	//Vor dieser Funktion muss "trackPeaks" aufgerufen werden
	public void peaksPositionsHistogramm() {
		histogramm = new int[100]; //Prozentuale Indizes bzgl. des Anfangs der Periode
		
		for (int i = 0; i < frames.size(); i++) {
			double[] peaks = peaksByFrame.get(i);			
			int[] periodStartingPoints = frames.get(i).getPeriodStartingPoints();
			int halfLengthOffset = (int)((periodStartingPoints.length - peaks.length) / 2);
			
			//Peaks-Länge mit allSamples angleichen
			double[] peaksFilled = new double[periodStartingPoints.length];
			
			for (int p = 0; p < peaks.length; p++) {
				peaksFilled[p + halfLengthOffset] = peaks[p];
			}
			
			int samplesPerPeriod = (int)((1 / frames.get(i).getPitch()) * Clip.getClassSamplingRate());
			
			//Histogramm erzeugen
			int lastSamplePositionBeingPeriodStart = 0;
			
			for (int p = 0; p < peaksFilled.length; p++) {
				if (periodStartingPoints[p] == 1) {
					lastSamplePositionBeingPeriodStart = p;
				}
				if (peaksFilled[p] == 1) {
					int percentage = (int)(100 * (p - lastSamplePositionBeingPeriodStart) / samplesPerPeriod);
					if (percentage < 100) {
						histogramm[percentage]++;
					}
				}
			}		
		}
	}
	
	public ArrayList<double[]> getPeaksByFrame() {
		return peaksByFrame;
	}
	
	public int[] getHistogramm() {
		return histogramm;
	}
}
