package godoy;

import java.io.File;

import godoy.Clip;

public class godoy {	
	public static void main(String[] arguments) {	
		System.out.println("Programm startet.");
		
		/* Audio-Datei �ffnen */
		File wavFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old", "dr1-fecd0-sa1.wav");
		/* Pitch-Listing-Datei �ffnen */
		File pitchListingFile = new File("D:\\Uni\\Diplomarbeit\\Software\\selected-corpus\\old", "dr1-fecd0-sa1.txt");
		
		try {
			Clip clip = Clip.newInstance(wavFile, pitchListingFile);
			//clip.getExporter().exportAsTXT();
			//clip.getExporter().exportFrames();
			//clip.getExporter().exportFramesWindowedSamples();
			//clip.getExporter().exportSpectrums();
			clip.getExporter().exportStDevs();
		}
		catch (Exception ex) {
			System.out.println("Die Verarbeitung der Audio-Datei ist fehlgeschlagen.");
			ex.printStackTrace(System.out);
		}
		
		
		System.out.println("Programm endet.");
	}	
	
}
