package godoy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import godoy.Clip;

public class godoy {	
	
	/* Konfigurationen */
	public static double T_ANALYSIS_OFFSET = 0.85; /* Wo fängt das erste Fenster an (wird zur Zeit unbenutzt, da der Wert für jeden Sprecher einzeln ermittelt wird. */
	public static double T_ANALYSIS_DELTA = 0.3; /* Wo fängt das zweite Fenster an, bezogen auf das erste Fenster */
	
	public static int MINIMAL_RELEVANT_FREQUENCY = 3000;
	public static int MAXIMAL_RELEVANT_FREQUENCY = 6000;
	
	public static int NUMBER_FIRST_DCT_COEFFICIENTS_FOR_CHARACTERISTICS_VECTOR = 80;
	public static boolean USE_PITCH_FOR_CHARACTERISTICS = false;
	
	public static void main(String[] arguments) {	
		System.out.println("Programm startet.");	
		
		/** EXPLORATIVER TEIL **/
		
		/* Korpus initialisieren */
		CorpusExtractor.onlyMales();
		ArrayList<String> corpus = CorpusExtractor.getCorpusForExploration("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus");
		
		if (false) for (String fileStub : corpus) {
			File wavFile = new File(fileStub + ".wav");
			File pitchListingFile = new File(fileStub + ".pitch");
			System.out.println("Verarbeite Datei " + fileStub);
			try {	
				for (double sp = 0.25; sp <= 0.85; sp += 0.05) {
					//Wegen der Präzisionfehler...
		            sp = Math.round(sp * 1000.0) / 1000.0;
		            
					Clip clip = Clip.newInstance(wavFile, pitchListingFile, godoy.T_ANALYSIS_DELTA);
					//ArrayList<double[]> spectrogramm = clip.getAnalyzer().getSpectrogrammFullFrames();
					String[] partsOfFilename = fileStub.split("\\\\");
					clip.getExporter().exportFramesWindowedSamples(partsOfFilename[partsOfFilename.length - 2] + "---" + wavFile.getName());
					//clip.getExporter().exportSpectrogrammFullFrames(partsOfFilename[partsOfFilename.length - 2] + "---" + wavFile.getName());
					//clip.getExporter().exportSpectrogrammClosedOpenDifference(partsOfFilename[partsOfFilename.length - 2] + "---" + wavFile.getName());
					//clip.getExporter().exportHistogrammPeaksByFrequency(partsOfFilename[partsOfFilename.length - 2] + "---" + wavFile.getName());					
//					clip.getAnalyzer().trackPeaks();
//					clip.getAnalyzer().peaksPositionsHistogramm();
					clip.getAnalyzer().trackStDevs();
					clip.getAnalyzer().stDevHistogramm();								
					GlobalAnalyzer.addHistogrammRow(clip.getAnalyzer().getHistogramm(), sp);
				}
			}
			catch (Exception ex) {
				System.out.println("Die Verarbeitung der Audio-Datei " + fileStub + " ist fehlgeschlagen.");
				ex.printStackTrace(System.out);
			}
		}
		
		/* Peak-Histogramm erzeugen */
//		LinkedHashMap<Double, double[]> histogramm2D = GlobalAnalyzer.getHistogramm2D();
//		GlobalExporter.exportHistogramm2D(histogramm2D);
		
		/** TRAINING / ERKENNUNG **/
		
		if (true) {
			CorpusExtractor.onlyFemales();		
		
			int recognitionRateSum = 0, recognitionRateCount = 0;
			
			for (int fi = 0; fi < 2; fi++) {
				System.out.println("==================================");
				System.out.println("fi = " + fi);
				System.out.println("==================================");
				
				/* Initialisiert alle Sprecher mit den zugehörigen Merkmalvektorfolgen */
				ArrayList<Speaker> corpusForTraining = CorpusExtractor.getCorpusForApplicationTraining("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus", 0);		
				Recognizer recognizer = new Recognizer();
				recognizer.train(corpusForTraining);
				
				ArrayList<Speaker> corpusForTesting = CorpusExtractor.getCorpusForApplicationTesting("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus", 0);	
				
				//Prozentuale Erkennungsrate speichern
				int correctlyRecognizedSpeakers = 0;		
				for (int i = 0; i < corpusForTesting.size(); i++) {
					HashMap<String, Integer> recognized = recognizer.recognize(corpusForTesting.get(i));
					int maxRecognizedProbability = 0;
					String idOfSpeakerWithMaxProbability = "";
					
					int recognizedProbabilityOfThis = 0;
					
					for (HashMap.Entry<String, Integer> estimate : recognized.entrySet()) {
						if (estimate.getValue() > maxRecognizedProbability) {
							maxRecognizedProbability = estimate.getValue();
							idOfSpeakerWithMaxProbability = estimate.getKey();
						}
						if (estimate.getKey().equals(corpusForTesting.get(i).getId())) {
							recognizedProbabilityOfThis = estimate.getValue();
						}
					}
					
					if (idOfSpeakerWithMaxProbability.equals(corpusForTesting.get(i).getId())) {
						correctlyRecognizedSpeakers++;
						System.out.println(corpusForTesting.get(i).getId() + " has been recognized with probability of " + maxRecognizedProbability + "%");
					}
					else {
						System.out.println(corpusForTesting.get(i).getId() + " has NOT been recognized. (" + recognizedProbabilityOfThis + "% :" + maxRecognizedProbability + "% for " + idOfSpeakerWithMaxProbability + ")");
					}					
				}
				
				int recognitionRate = (int)(100 * correctlyRecognizedSpeakers / corpusForTesting.size());
				recognitionRateSum += recognitionRate;
				recognitionRateCount++;
				
				System.out.println("==================================");
				System.out.println("Recognition rate for fi = " + fi + " is " + recognitionRate + "%");
			}
			
			System.out.println("==================================");
			System.out.println("==================================");
			System.out.println("Recognition rate ALL is " + (recognitionRateSum / recognitionRateCount) + "%");
		}
		
		
		/** ALTE LOGIK - zum Testen des Programms bei nur einer Datei. **/
		
		if (false) {
			/* Audio-Datei öffnen */
			File wavFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old\\male----1.wav");
			/* Pitch-Listing-Datei öffnen */
			File pitchListingFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old\\male----1.pitch");
			
			try {
				ArrayList<double[]> allDiffs = new ArrayList<double[]>();

				String fileStub = "D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old\\male----1";
				for (double i = 0.25; i <= 0.85; i += 0.05) {
				//for (double i = 0; i <= 1; i += 0.05) {				
					//Wegen der Präzisionfehler...
		            i = Math.round(i * 1000.0) / 1000.0;
		            
					//T_ANALYSIS_OFFSET = (double)i;
		            
					Clip clip = Clip.newInstance(wavFile, pitchListingFile, i);	
		            //Clip clip = Clip.newInstance(wavFile, pitchListingFile, T_ANALYSIS_DELTA);	
					String[] partsOfFilename = fileStub.split("\\\\");
					clip.getExporter().exportFramesWindowedSamples(partsOfFilename[partsOfFilename.length - 2] + "---" + wavFile.getName() + "---" + i);
				}
				
				//clip.getExporter().exportFramesWindowedSamples();
				//clip.getExporter().exportSpectrums();
				//clip.getExporter().exportPeakPositions();
				//clip.getExporter().exportPeaksPositionHistogramm();
				
			}
			catch (Exception ex) {
				System.out.println("Die Verarbeitung der Audio-Datei ist fehlgeschlagen.");
				ex.printStackTrace(System.out);
			}
		}
		
		
		System.out.println("Programm endet.");
	}	
	
}
