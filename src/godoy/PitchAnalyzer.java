package godoy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;

import java.util.logging.Logger;

/*
 * Grundfrequenz-Analyser. Er liest die Datei aus (wie erzeugt im Praat) und verarbeitet sie.
 * 
 * Annahme: die Grundfrequenz �ndert sich innerhalb von 10 ms nicht.
 */

public class PitchAnalyzer {
	private static final Logger logger = Logger.getLogger(PitchAnalyzer.class.getName());
	
    private LinkedHashMap<Double, Double> pitches = new LinkedHashMap<Double, Double>();
    
    private final double timeStep = 0.01; //in Sekunden, wird von Praat vorgegeben
    
    private double initialTime;
    
	public PitchAnalyzer(File pitchListingFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(pitchListingFile));
		    String line;
		    int counter = 0;
		    while ((line = br.readLine()) != null) {
		       counter++;
		       if (counter == 1) continue; //Kopfzeile auslassen
		       
		       String[] parts = line.split("   "); //Praat f�gt drei Leerzeichen zwischen Zeit und Pitch ein
		       double time = Double.parseDouble(parts[0]), pitch;
		       
		       if (parts[1].equals("--undefined--")) {
		    	   pitch = 0;
		       }
		       else {
		    	   pitch = Double.parseDouble(parts[1]);
		       }
		       
		       if (counter == 2) { //Die erste erfasste Zeit speichern
		    	   initialTime = time;
		       }
		       
		       pitches.put(time, pitch);
		    }
		}
		catch (Exception ex) {
			System.out.println("Die Verarbeitung der Pitch-Listing-Datei ist fehlgeschlagen.");
			ex.printStackTrace(System.out);
		}
	}
	
	public double timeStep() {
		return timeStep;
	}
	
	public double initialTime() {
		return initialTime;
	}
	
	public boolean isVoiced(double time) {		
		if (pitches.get(time) != null) {
			return pitches.get(time) != 0;
		}
		else return false;
	}
	
}