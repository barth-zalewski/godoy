package godoy;

import java.io.File;

import godoy.Clip;

public class godoy {	
	public static void main(String[] arguments) {	
		System.out.println("Programm startet.");
		
		/* Audio-Datei öffnen */
		File wavFile = new File("D:\\Uni\\Diplomarbeit\\Software\\samples", "ldc93.wav");
		/* Pitch-Listing-Datei öffnen */
		File pitchListingFile = new File("D:\\Uni\\Diplomarbeit\\Software\\samples", "ldc93-pitch-listing.txt");
		
		try {
			Clip clip = Clip.newInstance(wavFile, pitchListingFile);
			//clip.getExporter().exportAsTXT();
			//clip.getExporter().exportFramesSamples();
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
