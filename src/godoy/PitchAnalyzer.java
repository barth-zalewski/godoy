package godoy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;

/*
 * Grundfrequenz-Analyser. Er liest die Datei aus (wie erzeugt im Praat) und verarbeitet sie.
 * 
 * Annahme: die Grundfrequenz ändert sich innerhalb von 10 ms nicht.
 */

public class PitchAnalyzer {	
    private LinkedHashMap<Double, Double> pitches = new LinkedHashMap<Double, Double>();
    
    private final double timeStep = Clip.PRAAT_PITCH_RESOLUTION; //in Sekunden, wird von Praat vorgegeben
    
    private double initialTime;
    
	public PitchAnalyzer(File pitchListingFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(pitchListingFile));
		    String line;
		    int counter = 0;
		    while ((line = br.readLine()) != null) {
		       counter++;
		       if (counter == 1) continue; //Kopfzeile auslassen
		       
		       String[] parts = line.split("   "); //Praat fügt drei Leerzeichen zwischen Zeit und Pitch ein
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
		    
		    br.close();
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
	
	public boolean isVoicedNext(double time) {	
		time += timeStep();
		
		//Wegen Präzisionsfehler...
		time = Math.round(time * 1000000.0) / 1000000.0;
		if (pitches.get(time) != null) {
			return pitches.get(time) != 0;
		}
		else return false;
	}
	
	public boolean isVoiced2Next(double time) {		
		time += 2 * timeStep();
		
		//Wegen Präzisionsfehler...
		time = Math.round(time * 1000000.0) / 1000000.0;
		if (pitches.get(time) != null) {
			return pitches.get(time) != 0;
		}
		else return false;
	}
	
	public double getPitch(double time) {
		return pitches.get(time);
	}
	
	public double getPitchNext(double time) {
		time += timeStep();
		
		//Wegen Präzisionsfehler...
		time = Math.round(time * 1000000.0) / 1000000.0;
		
		return pitches.get(time);
	}
	
	public double getPitch2Next(double time) {
		time += 2 * timeStep();
		
		//Wegen Präzisionsfehler...
		time = Math.round(time * 1000000.0) / 1000000.0;
		
		return pitches.get(time);
	}
	
}
