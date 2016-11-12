package godoy;

import java.io.File;

import godoy.Clip;

public class godoy {	
	public static void main(String[] arguments) {	
		System.out.println("Programm startet.");
		
		File wavFile = new File("D:\\Uni\\Diplomarbeit\\Software", "ldc93.wav");
		try {
			Clip clip = Clip.newInstance(wavFile);
			clip.getExporter().exportAsTXT();
		}
		catch (Exception ex) {
			System.out.println("Die Verarbeitung der Datei ist fehlgeschlagen.");
			ex.printStackTrace(System.out);
		}
		
		System.out.println("Programm endet.");
	}	
	
}
