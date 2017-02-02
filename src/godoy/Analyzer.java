package godoy;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sun.rmi.server.Util;

/* Analyzer für einzelne Clips */
public class Analyzer {
	
	private List<Frame> frames;
	
	private ArrayList<double[]> peaksByFrame;
	private ArrayList<double[]> stDevsByFrame;
	
	double[] histogramm;	
	
	private int minFrequency = godoy.MINIMAL_RELEVANT_FREQUENCY,
				maxFrequency = godoy.MAXIMAL_RELEVANT_FREQUENCY;
	
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
				
				/* spectralDifferences in ein Array umwandeln */
				double[] spectralDifferencesArray = new double[spectralDifferences.size()];
				for (int sd = 0; sd < spectralDifferences.size(); sd++) {
					spectralDifferencesArray[sd] = spectralDifferences.get(sd);
				}	
				
				/* Lokale Maxima extrahieren */
				//Lokales Maximum ist überall dort, wo die zwei benachbarten Werte kleiner sind.
				
				double[] isLocalMaximum = new double[spectralDifferencesArray.length]; //Wert des lokalen Maximums oder -Infnity falls keins
				
				for (int lm = 2; lm < spectralDifferencesArray.length - 3; lm++) {
					double that = spectralDifferencesArray[lm],
						   prev = spectralDifferencesArray[lm - 1],
						   pprev = spectralDifferencesArray[lm - 2],
						   next = spectralDifferencesArray[lm + 1],
						   nnext = spectralDifferencesArray[lm + 2];
					
					if (that > prev && that > pprev && that > next && that > nnext) {
						isLocalMaximum[lm] = that;
					}
					else {
						isLocalMaximum[lm] = Double.NEGATIVE_INFINITY;
					}
				}
								
				double maxPeak = Double.NEGATIVE_INFINITY, 
					   secondMaxPeak = Double.NEGATIVE_INFINITY;
				
				for (int sd = 0; sd < isLocalMaximum.length; sd++) {
					if (maxPeak < isLocalMaximum[sd]) {
						secondMaxPeak = maxPeak;
						maxPeak = isLocalMaximum[sd];						
					}
				}							
				
				peaks[j] = secondMaxPeak / maxPeak < 0.05 ? 1 : 0;
			}
			
			peaksByFrame.add(peaks);
		}			
	}	
	
	/* DELTA ist vorgegeben, OFFSET wird verändert */
	//Vor dieser Funktion muss "trackPeaks" aufgerufen werden
	public void peaksPositionsHistogramm() {
		histogramm = new double[100]; //Prozentuale Indizes bzgl. des Anfangs der Periode
		
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
	
	public void trackStDevs() {		
		stDevsByFrame = new ArrayList<double[]>();
		
		for (int i = 0; i < frames.size(); i++) {
			Map<Integer, double[]> spectrums1 = frames.get(i).getSpectrums1();
			Map<Integer, double[]> spectrums2 = frames.get(i).getSpectrums2();
			
			double[] stDevs = new double[spectrums1.size()];
			
			//Jedes Frame hat viele Spektren
			for (int j = 0; j < spectrums1.size(); j++) {
				double[] spectrum1 = spectrums1.get(j),
						 spectrum2 = spectrums2.get(j);
				
				ArrayList<Double> spectralDifferences = new ArrayList<Double>();
				
				for (int s = 0; s < spectrum1.length; s++) {		
					double frequency = (double)s * 0.5 * Clip.getClassSamplingRate() / spectrum1.length;
					if (frequency > godoy.MINIMAL_RELEVANT_FREQUENCY && frequency < godoy.MAXIMAL_RELEVANT_FREQUENCY) {
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
				
				/* spectralDifferences in ein Array umwandeln */
				double[] spectralDifferencesArray = new double[spectralDifferences.size()];
				for (int sd = 0; sd < spectralDifferences.size(); sd++) {
					spectralDifferencesArray[sd] = spectralDifferences.get(sd);
				}
				
				double stDev = Utils.standardDeviation(spectralDifferencesArray);
				
				stDevs[j] = stDev;
			}
			
			stDevsByFrame.add(stDevs);
		}
	}
	
	//Vor dieser Funktion muss "trackStDevs" aufgerufen werden
	public void stDevHistogramm() {
		histogramm = new double[100]; //Prozentuale Indizes bzgl. des Anfangs der Periode
		int[] sums = new int[histogramm.length];
		
		for (int i = 0; i < frames.size(); i++) {
			double[] stDevs = stDevsByFrame.get(i);			
			int[] periodStartingPoints = frames.get(i).getPeriodStartingPoints(); //Dieses Array hat die Länge == Frame-Gesamtlänge
			int halfLengthOffset = (int)((periodStartingPoints.length - stDevs.length) / 2);
			
			//StDevs-Länge mit allSamples angleichen
			double[] stDevsFilled = new double[periodStartingPoints.length];
			
			for (int p = 0; p < stDevs.length; p++) {
				stDevsFilled[p + halfLengthOffset] = stDevs[p];
			}
			
			int samplesPerPeriod = (int)((1 / frames.get(i).getPitch()) * Clip.getClassSamplingRate());
			
			//Histogramm erzeugen
			int lastSamplePositionBeingPeriodStart = 0;
			
			for (int p = 0; p < stDevsFilled.length; p++) {
				if (periodStartingPoints[p] == 1) {
					lastSamplePositionBeingPeriodStart = p;
				}
				int percentage = (int)(100 * (p - lastSamplePositionBeingPeriodStart) / samplesPerPeriod);
				if (percentage < 100) {
					histogramm[percentage] += stDevsFilled[p];
					sums[percentage]++;
				}
			}		
		}
		
		for (int hi = 0; hi < histogramm.length; hi++) {
			histogramm[hi] /= sums[hi] > 0 ? 
							  sums[hi] :
						      1;
		}
	}	
	
	public ArrayList<double[]> getPeaksByFrame() {
		return peaksByFrame;
	}
	
	public double[] getHistogramm() {
		return histogramm;
	}
	
	public ArrayList<double[]> getSpectrogrammFullFrames() {
		ArrayList<double[]> spectrogramm = new ArrayList<double[]>();
		
		for (int i = 0; i < frames.size(); i++) {
			double[] fullSpectrum = frames.get(i).getWholeFrameSpectrum();
			double[] shortenedSpectrum = new double[fullSpectrum.length / 2];
			for (int isp = 0; isp < shortenedSpectrum.length; isp++) { //Negative Frequenzen verwerfen
				shortenedSpectrum[isp] = fullSpectrum[isp];
			}
			spectrogramm.add(shortenedSpectrum);
		}
		return spectrogramm;
	}
	
	public ArrayList<double[]> getSpectrogrammClosedOpenDifference() {
		ArrayList<double[]> spectrogramm = new ArrayList<double[]>();		
		
		for (int i = 0; i < frames.size(); i++) {
			ArrayList<double[]> diffSpectrums = frames.get(i).getDiffSpectrums();
			
			for (int s = 0; s < diffSpectrums.size(); s++) {
				spectrogramm.add(diffSpectrums.get(s));
			}
			
		}
		return spectrogramm;
	}
	
	public int[] getHistogrammPeaksByFrequency() {
		
		int[] histogramm = null; //Halbe FFT-Länge
		
		for (int i = 0; i < frames.size(); i++) {
			Map<Integer, double[]> spectrums1 = frames.get(i).getSpectrums1(),
								   spectrums2 = frames.get(i).getSpectrums2();
			
			for (int j = 0; j < spectrums1.size(); j++) {
				double[] spectrum1 = spectrums1.get(j),
						 spectrum2 = spectrums2.get(j);
				
				ArrayList<Double> spectralDifferences = new ArrayList<Double>();
				
				for (int s = 0; s < spectrum1.length; s++) {		
					double frequency = (double)s * 0.5 * Clip.getClassSamplingRate() / spectrum1.length;
					if (frequency > minFrequency && frequency < maxFrequency) {
						spectralDifferences.add(spectrum1[s] - spectrum2[s]);
					}
					//else {
					//	spectralDifferences.add(0.0);
					//}
				}
				
				if (histogramm == null) {
					histogramm = new int[spectralDifferences.size()];
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
				
				/* spectralDifferences in ein Array umwandeln */
				double[] spectralDifferencesArray = new double[spectralDifferences.size()];
				for (int sd = 0; sd < spectralDifferences.size(); sd++) {
					spectralDifferencesArray[sd] = spectralDifferences.get(sd);
				}	
				
				/* Lokale Maxima extrahieren */
				//Lokales Maximum ist überall dort, wo die zwei benachbarten Werte kleiner sind.
				
				double[] isLocalMaximum = new double[spectralDifferencesArray.length]; //Wert des lokalen Maximums oder -Infnity falls keins
				
				for (int lm = 2; lm < spectralDifferencesArray.length - 3; lm++) {
					double that = spectralDifferencesArray[lm],
						   prev = spectralDifferencesArray[lm - 1],
						   pprev = spectralDifferencesArray[lm - 2],
						   next = spectralDifferencesArray[lm + 1],
						   nnext = spectralDifferencesArray[lm + 2];
					
					if (that > prev && that > pprev && that > next && that > nnext) {
						isLocalMaximum[lm] = that;
					}
					else {
						isLocalMaximum[lm] = 0;
					}
				}
								
				double maxPeak = 0, 
					   secondMaxPeak = 0;
				
				int indexOfPeak = -1;
				
				for (int sd = 0; sd < isLocalMaximum.length; sd++) {					
					if (maxPeak < isLocalMaximum[sd]) {
						secondMaxPeak = maxPeak;
						maxPeak = isLocalMaximum[sd];
						indexOfPeak = sd;
					}
				}		
				
				if (secondMaxPeak / maxPeak < 0.05 && indexOfPeak != -1) {
					histogramm[indexOfPeak]++;
				}
			}
		}
		
		return histogramm;
	}
	
	public double getDeepValleyFrequency() {
		ArrayList<double[]> spectrogramm = getSpectrogrammFullFrames();
		
		/* Maximalen Wert finden */
		double maxValue = Double.NEGATIVE_INFINITY, minValue = Double.POSITIVE_INFINITY;
		
		for (int i = 0; i < spectrogramm.size(); i++) {
			double[] spectrum = spectrogramm.get(i);
			for (int j = 0; j < spectrum.length; j++) {
				maxValue = Math.max(maxValue, spectrum[j]);
				minValue = Math.min(minValue, spectrum[j]);
			}
		}
		
		maxValue = Math.log10(maxValue);
		minValue = Math.log10(minValue);			
		
		int sumMinJ = 0;
		
		for (int i = 0; i < spectrogramm.size(); i++) {
			double[] spectrum = spectrogramm.get(i);

			double min = Double.POSITIVE_INFINITY;
			int minJ = -1;
			
			for (int j = 0; j < spectrum.length; j++) {		
				double fq = (j * Clip.getClassSamplingRate() / (spectrogramm.get(0).length * 2));
				boolean isRelevantFrequency = fq >= godoy.MINIMAL_RELEVANT_FREQUENCY && fq <= godoy.MAXIMAL_RELEVANT_FREQUENCY;
				
				if (!isRelevantFrequency) {
					continue;
				}
				
				if (Math.log10(spectrum[j]) - minValue < min) {
					min = Math.log10(spectrum[j]) - minValue;
					minJ = j;
				}				
			}
			
			sumMinJ += minJ;			
		}
	    
	    sumMinJ /= spectrogramm.size();
	    
	    return sumMinJ * Clip.getClassSamplingRate() / (spectrogramm.get(0).length * 2);
	}
	
	public double getCyclicPeakFrequency() {
		ArrayList<double[]> spectrogramm = getSpectrogrammClosedOpenDifference();
		
		int sumMaxJ = 0;
		
		for (int i = 0; i < spectrogramm.size(); i++) {
			double[] spectrum = spectrogramm.get(i);
			
			double max = Double.NEGATIVE_INFINITY;		
			int maxJ = -1;
			
			for (int j = 0; j < spectrum.length; j++) {
				double fq = (j * Clip.getClassSamplingRate() / (spectrogramm.get(0).length * 2));
				boolean isRelevantFrequency = fq >= godoy.MINIMAL_RELEVANT_FREQUENCY && fq <= godoy.MAXIMAL_RELEVANT_FREQUENCY;
				
				if (!isRelevantFrequency) {
					continue;
				}
				
				if (isRelevantFrequency && spectrum[j] > max) { 
					max = spectrum[j];	
					maxJ = j;
				}				
			}
			
			sumMaxJ += maxJ;			
		}
		
		sumMaxJ /= spectrogramm.size();
		
		return sumMaxJ * Clip.getClassSamplingRate() / (spectrogramm.get(0).length * 2);
	}
}
