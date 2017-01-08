package godoy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import godoy.Clip;

public class godoy {	
	
	/* Konfigurationen */
	public static double T_ANALYSIS_OFFSET = 0.2; /* Wo fängt das erste Fenster an (wird zur Zeit unbenutzt, da der Wert für jeden Sprecher einzeln ermittelt wird. */
	public static double T_ANALYSIS_DELTA = 0.45; /* Wo fängt das zweite Fenster an, bezogen auf das erste Fenster */
	
	public static int MINIMAL_RELEVANT_FREQUENCY = 2500;
	public static int MAXIMAL_RELEVANT_FREQUENCY = 6000;
	
	public static int NUMBER_FIRST_DCT_COEFFICIENTS_FOR_CHARACTERISTICS_VECTOR = 3;
	public static boolean USE_PITCH_FOR_CHARACTERISTICS = false;
	
	public static void main(String[] arguments) {	
		System.out.println("Programm startet.");	
		
		/** EXPLORATIVER TEIL **/
		
		/* Korpus initialisieren */
		CorpusExtractor.onlyFemales();
		ArrayList<String> corpus = CorpusExtractor.getCorpusForExploration("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus");
		
		if (true) for (String fileStub : corpus) {
			File wavFile = new File(fileStub + ".wav");
			File pitchListingFile = new File(fileStub + ".pitch");
			System.out.println("Verarbeite Datei " + fileStub);
			try {	
				for (double sp = 0.25; sp <= 0.85; sp += 0.05) {
					//Wegen der Präzisionfehler...
		            sp = Math.round(sp * 1000.0) / 1000.0;
		            
					Clip clip = Clip.newInstance(wavFile, pitchListingFile, sp);
					//ArrayList<double[]> spectrogramm = clip.getAnalyzer().getSpectrogrammFullFrames();
					String[] partsOfFilename = fileStub.split("\\\\");
					
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
		LinkedHashMap<Double, double[]> histogramm2D = GlobalAnalyzer.getHistogramm2D();
		GlobalExporter.exportHistogramm2D(histogramm2D);
		
		/** TRAINING / ERKENNUNG **/
		
//		CorpusExtractor.all();
//		
//		/* Initialisiert alle Sprecher mit den zugehörigen Merkmalvektorfolgen */
//		ArrayList<Speaker> corpusForTraining = CorpusExtractor.getCorpusForApplicationTraining("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus");		
//		Recognizer recognizer = new Recognizer();
//		recognizer.train(corpusForTraining);
//		
//		ArrayList<Speaker> corpusForTesting = CorpusExtractor.getCorpusForApplicationTesting("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus");	
//		
//		//Prozentuale Erkennungsrate speichern
//		int correctlyRecognizedSpeakers = 0;		
//		for (int i = 0; i < corpusForTesting.size(); i++) {
//			HashMap<String, Integer> recognized = recognizer.recognize(corpusForTesting.get(i));
//			int maxRecognizedProbability = 0;
//			String idOfSpeakerWithMaxProbability = "";
//			
//			int recognizedProbabilityOfThis = 0;
//			
//			for (HashMap.Entry<String, Integer> estimate : recognized.entrySet()) {
//				if (estimate.getValue() > maxRecognizedProbability) {
//					maxRecognizedProbability = estimate.getValue();
//					idOfSpeakerWithMaxProbability = estimate.getKey();
//				}
//				if (estimate.getKey().equals(corpusForTesting.get(i).getId())) {
//					recognizedProbabilityOfThis = estimate.getValue();
//				}
//			}
//			
//			if (idOfSpeakerWithMaxProbability.equals(corpusForTesting.get(i).getId())) {
//				correctlyRecognizedSpeakers++;
//				System.out.println(corpusForTesting.get(i).getId() + " has been recognized with probability of " + maxRecognizedProbability + "%");
//			}
//			else {
//				System.out.println(corpusForTesting.get(i).getId() + " has NOT been recognized. (" + recognizedProbabilityOfThis + "% :" + maxRecognizedProbability + "% for " + idOfSpeakerWithMaxProbability + ")");
//			}					
//		}
//		
//		System.out.println("==================================");
//		System.out.println("Recognition rate = " + ((int)(100 * correctlyRecognizedSpeakers / corpusForTesting.size())) + "%");
		
		
		
		/** ALTE LOGIK - zum Testen des Programms bei nur einer Datei. **/
		
//		/* Audio-Datei öffnen */
//		File wavFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old\\dr1-fetb0-si1778.wav");
//		/* Pitch-Listing-Datei öffnen */
//		File pitchListingFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old\\dr1-fetb0-si1778.txt");
//		
//		try {
//			Clip clip = Clip.newInstance(wavFile, pitchListingFile, 0.5);		
//			//clip.getExporter().exportFramesWindowedSamples();
//			//clip.getExporter().exportSpectrums();
//			//clip.getExporter().exportPeakPositions();
//			//clip.getExporter().exportPeaksPositionHistogramm();
//			
//			clip.createCharacteristicsVector();
//		}
//		catch (Exception ex) {
//			System.out.println("Die Verarbeitung der Audio-Datei ist fehlgeschlagen.");
//			ex.printStackTrace(System.out);
//		}
//		
		
		System.out.println("Programm endet.");
	}	
	
}
