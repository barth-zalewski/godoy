package godoy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class CorpusExtractor {
	
	private static String gender = "*";
	private static ArrayList<String> corpusForExploration = new ArrayList<String>();
	private static ArrayList<Speaker> corpusForApplicationTraining = new ArrayList<Speaker>();
	private static ArrayList<Speaker> corpusForApplicationTesting = new ArrayList<Speaker>();
	
	private static int indexOfFileForTesting = -1;
	
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
			listSpeakers(baseFolder, true);
		}
		if (gender.equals("*") || gender.equals("males")) {			
			File baseFolder = new File(basePath + "\\males");
			listSpeakers(baseFolder, true);
		}
		return corpusForApplicationTraining;
	}
	
	public static ArrayList<Speaker> getCorpusForApplicationTraining(String basePath, int index) {
		indexOfFileForTesting = index;
		return getCorpusForApplicationTraining(basePath);
	}
	
	public static ArrayList<Speaker> getCorpusForApplicationTesting(String basePath) {
		if (gender.equals("*") || gender.equals("females")) {			
			File baseFolder = new File(basePath + "\\females");
			listSpeakers(baseFolder, false);
		}
		if (gender.equals("*") || gender.equals("males")) {			
			File baseFolder = new File(basePath + "\\males");
			listSpeakers(baseFolder, false);
		}
		return corpusForApplicationTesting;
	}
	
	public static ArrayList<Speaker> getCorpusForApplicationTesting(String basePath, int index) {
		indexOfFileForTesting = index;
		return getCorpusForApplicationTesting(basePath);
	}
	
	private static void listSpeakers(final File folder, boolean isTraining) {
		HashMap<String, ArrayList<String>> stringCorpus = new HashMap<String, ArrayList<String>>();
		
		int fileIndex = -1;
		
		for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	listSpeakers(fileEntry, isTraining);
	        } else {	        	
	        	if (fileEntry.getName().indexOf(".wav") > -1) {
	        		fileIndex++;		        	
		        	
	        		String path = fileEntry.getAbsolutePath(),
	        			   absolutePathWithoutExtension = path.substring(0, path.lastIndexOf('.'));
	        		
	        		String onlyPath = absolutePathWithoutExtension.substring(0, absolutePathWithoutExtension.lastIndexOf(File.separator));
	        		onlyPath = onlyPath.substring(onlyPath.lastIndexOf(File.separator) + 1);
	        		
	        		boolean isTrainingAndFileIsForTraining = isTraining;
	        		boolean isTestingAndFileIsForTesting = !isTraining;
	        		
	        		//Falls nicht definiert, orientiere dich am Präfix "test--"
	        		if (indexOfFileForTesting == -1) {
	        			isTrainingAndFileIsForTraining = isTrainingAndFileIsForTraining && path.indexOf("test--") == -1;
	        			isTestingAndFileIsForTesting = isTestingAndFileIsForTesting && path.indexOf("test--") > -1;
	        		}
	        		else {
	        			isTrainingAndFileIsForTraining = isTrainingAndFileIsForTraining && fileIndex != indexOfFileForTesting;
	        			isTestingAndFileIsForTesting = isTestingAndFileIsForTesting && fileIndex == indexOfFileForTesting;
	        		}
	        		
	        		if (isTrainingAndFileIsForTraining || isTestingAndFileIsForTesting) { 
	        			if (stringCorpus.get(onlyPath) == null) {
	        				stringCorpus.put(onlyPath, new ArrayList<String>());
	        			}
	        			if (isTestingAndFileIsForTesting) System.out.println("Datei zum Testen = " + absolutePathWithoutExtension);
	        			stringCorpus.get(onlyPath).add(absolutePathWithoutExtension);	        			
	        		}
	        	}
	        }
	    }
		
		ArrayList<Speaker> relevantCorpus = isTraining ? corpusForApplicationTraining : corpusForApplicationTesting;
		
		for (HashMap.Entry<String, ArrayList<String>> entry : stringCorpus.entrySet()) {
			String speakerKey = entry.getKey();
			ArrayList<String> speakerClips = entry.getValue();
			
			Speaker speaker = new Speaker(speakerKey);
			
			for (int i = 0; i < speakerClips.size(); i++) {
				String fileStubName = speakerClips.get(i);
				speaker.addClip(fileStubName);
			}
			
			//Nachdem alle Clips hinzugefügt wurden, initialisieren
			speaker.initializeClips();
			
			relevantCorpus.add(speaker);
		}
	}
	
	public static void clear() {
		corpusForApplicationTraining = new ArrayList<Speaker>();
		corpusForApplicationTesting = new ArrayList<Speaker>();
	}
	
	public static void onlyFemales() {
		gender = "females";
		System.out.println("Nur Frauen.");
	}
	
	public static void onlyMales() {
		gender = "males";
		System.out.println("Nur Männer.");
	}
	
	public static void all() {
		gender = "*";
		System.out.println("Männer und Frauen.");
	}
}
