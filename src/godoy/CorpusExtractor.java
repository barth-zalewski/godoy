package godoy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class CorpusExtractor {
	
	private static String gender = "*";
	private static ArrayList<String> corpusForExploration = new ArrayList<String>();
	private static ArrayList<Speaker> corpusForApplicationTraining = new ArrayList<Speaker>();
	
	/* Liefert einfach nur Dateinamen zurück. Sprecherzuordnung ist unwichtig. */
	public static ArrayList<String> getCorpusForExploration(String basePath) {
		if (gender.equals("*") || gender.equals("females")) {			
			File baseFolder = new File(basePath + "\\females");
			listFiles(baseFolder);
		}
		if (gender.equals("*") || gender.equals("males")) {			
			File baseFolder = new File(basePath + "\\males");
			listFiles(baseFolder);
		}
		return corpusForExploration;
	}
	
	private static void listFiles(final File folder) {		
		for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	listFiles(fileEntry);
	        } else {
	        	if (fileEntry.getName().indexOf(".wav") > -1) {
	        		String path = fileEntry.getAbsolutePath(),
	        			   absolutePathWithoutExtension = path.substring(0, path.lastIndexOf('.'));
	        		corpusForExploration.add(absolutePathWithoutExtension);	        		
	        	}
	        }
	    }
	}
	
	public static ArrayList<Speaker> getCorpusForApplicationTraining(String basePath) {
		if (gender.equals("*") || gender.equals("females")) {			
			File baseFolder = new File(basePath + "\\females");
			listSpeakers(baseFolder);
		}
		if (gender.equals("*") || gender.equals("males")) {			
			File baseFolder = new File(basePath + "\\males");
			listSpeakers(baseFolder);
		}
		return corpusForApplicationTraining;
	}
	
	private static void listSpeakers(final File folder) {
		HashMap<String, ArrayList<String>> stringCorpus = new HashMap<String, ArrayList<String>>();
		
		for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	listSpeakers(fileEntry);
	        } else {
	        	if (fileEntry.getName().indexOf(".wav") > -1) {
	        		String path = fileEntry.getAbsolutePath(),
	        			   absolutePathWithoutExtension = path.substring(0, path.lastIndexOf('.'));
	        		
	        		String onlyPath = absolutePathWithoutExtension.substring(0, absolutePathWithoutExtension.lastIndexOf(File.separator));
	        		onlyPath = onlyPath.substring(onlyPath.lastIndexOf(File.separator) + 1);
	        		
	        		if (path.indexOf("test--") == -1) {
	        			if (stringCorpus.get(onlyPath) == null) {
	        				stringCorpus.put(onlyPath, new ArrayList<String>());
	        			}
	        			
	        			stringCorpus.get(onlyPath).add(absolutePathWithoutExtension);	        			
	        		}
	        	}
	        }
	    }
		
		for (HashMap.Entry<String, ArrayList<String>> entry : stringCorpus.entrySet()) {
			String speakerKey = entry.getKey();
			ArrayList<String> speakerClips = entry.getValue();
			
			Speaker speaker = new Speaker(speakerKey);
			
			for (int i = 0; i < speakerClips.size(); i++) {
				String fileStubName = speakerClips.get(i);
				speaker.addTrainingClip(fileStubName);
			}
			
			//Nachdem alle Clips hinzugefügt wurden, initialisieren
			speaker.initializeTrainingClips();
		}
	}
	
	public static void onlyFemales() {
		gender = "females";
	}
	
	public static void onlyMales() {
		gender = "males";
	}
	
	public static void all() {
		gender = "*";
	}
}
