package godoy;

import java.io.File;

import godoy.Clip;

public class godoy {	
	public static void main(String[] arguments) {	
		System.out.println("Programm startet.");
		
		/* Audio-Datei �ffnen */
		File wavFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus", "male1-sa2.wav");
		/* Pitch-Listing-Datei �ffnen */
		File pitchListingFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus", "male1-sa2.txt");
		
		try {
			Clip clip = Clip.newInstance(wavFile, pitchListingFile);
			//clip.getExporter().exportAsTXT();
			//clip.getExporter().exportFrames();
			clip.getExporter().exportFramesWindowedSamples();
			//clip.getExporter().exportSpectrums();
		}
		catch (Exception ex) {
			System.out.println("Die Verarbeitung der Audio-Datei ist fehlgeschlagen.");
			ex.printStackTrace(System.out);
		}
		
		
		System.out.println("Programm endet.");
	}	
	
}
