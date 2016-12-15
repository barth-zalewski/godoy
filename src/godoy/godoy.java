package godoy;

import java.io.File;
import java.util.ArrayList;

import godoy.Clip;

public class godoy {	
	public static void main(String[] arguments) {	
		System.out.println("Programm startet.");
		
		/* Korpus initialisieren */
		CorpusExtractor.onlyFemales();
		ArrayList<File> corpus = CorpusExtractor.getCorpus("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus");
		
		/* Peak-Histogramm erzeugen */
		// Wird gemacht.
		
		//Alte Logik
		/* Audio-Datei öffnen */
		File wavFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old", "dr1-fecd0-sa1.wav");
		/* Pitch-Listing-Datei öffnen */
		File pitchListingFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old", "dr1-fecd0-sa1.txt");
		
		try {
			Clip clip = Clip.newInstance(wavFile, pitchListingFile);		
			//clip.getExporter().exportFramesWindowedSamples();
			//clip.getExporter().exportSpectrums();
			clip.getExporter().exportPeakPositions();
		}
		catch (Exception ex) {
			System.out.println("Die Verarbeitung der Audio-Datei ist fehlgeschlagen.");
			ex.printStackTrace(System.out);
		}
		
		
		System.out.println("Programm endet.");
	}	
	
}
