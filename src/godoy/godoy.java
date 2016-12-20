package godoy;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import godoy.Clip;

public class godoy {	
	public static void main(String[] arguments) {	
		System.out.println("Programm startet.");
		
		/* Korpus initialisieren */
		CorpusExtractor.onlyFemales();
		ArrayList<String> corpus = CorpusExtractor.getCorpus("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus");
		
		for (String fileStub : corpus) {
			File wavFile = new File(fileStub + ".wav");
			File pitchListingFile = new File(fileStub + ".pitch");
			System.out.println(fileStub);
			try {
				for (double sp = 0.25; sp <= 0.7; sp += 0.05) {
					Clip clip = Clip.newInstance(wavFile, pitchListingFile, sp);
					clip.getAnalyzer().peaksPositionsHistogramm();
					GlobalAnalyzer.addHistogrammRow(clip.getAnalyzer().getHistogramm(), sp);					
					Map<Double, int[]> histogramm2D = GlobalAnalyzer.getHistogramm2D();
					GlobalExporter.exportHistogramm2D(histogramm2D);
				}
			}
			catch (Exception ex) {
				System.out.println("Die Verarbeitung der Audio-Datei " + fileStub + " ist fehlgeschlagen.");
				ex.printStackTrace(System.out);
			}
		}
		
		/* Peak-Histogramm erzeugen */
		// Wird gemacht.
		
		//Alte Logik
		/* Audio-Datei �ffnen */
//		File wavFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old\\dr1-fetb0-si1778.wav");
//		/* Pitch-Listing-Datei �ffnen */
//		File pitchListingFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old\\dr1-fetb0-si1778.txt");
//		
//		try {
//			Clip clip = Clip.newInstance(wavFile, pitchListingFile);		
//			//clip.getExporter().exportFramesWindowedSamples();
//			//clip.getExporter().exportSpectrums();
//			//clip.getExporter().exportPeakPositions();
//			clip.getExporter().exportPeaksPositionHistogramm();
//		}
//		catch (Exception ex) {
//			System.out.println("Die Verarbeitung der Audio-Datei ist fehlgeschlagen.");
//			ex.printStackTrace(System.out);
//		}
//		
		
		System.out.println("Programm endet.");
	}	
	
}
