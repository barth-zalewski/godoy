package godoy;

import java.io.File;

import godoy.Clip;

public class godoy {	
	public static void main(String[] arguments) {	
		System.out.println("Programm startet.");
		
		/* Audio-Datei öffnen */
		File wavFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus", "dr1-fetb0-si1778.wav");
		/* Pitch-Listing-Datei öffnen */
		File pitchListingFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus", "dr1-fetb0-si1778.txt");
		
		try {
			Clip clip = Clip.newInstance(wavFile, pitchListingFile);
			//clip.getExporter().exportAsTXT();
			//clip.getExporter().exportFrames();
			clip.getExporter().exportFramesWindowedSamples();
			//clip.getExporter().exportSpectrums();
			//clip.getExporter().exportStDevs();
		}
		catch (Exception ex) {
			System.out.println("Die Verarbeitung der Audio-Datei ist fehlgeschlagen.");
			ex.printStackTrace(System.out);
		}
		
		
		System.out.println("Programm endet.");
	}	
	
}
