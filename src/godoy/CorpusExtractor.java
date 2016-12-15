package godoy;

import java.io.File;
import java.util.ArrayList;

public class CorpusExtractor {
	
	private static String gender = "*";
	private static ArrayList<File> corpus = new ArrayList<File>();
	
	public static ArrayList<File> getCorpus(String basePath) {
		if (gender.equals("*") || gender.equals("females")) {			
			File baseFolder = new File(basePath + "\\females");
			listFiles(baseFolder);
		}
		return corpus;
	}
	
	private static void listFiles(final File folder) {		
		for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	listFiles(fileEntry);
	        } else {
	        	if (fileEntry.getName().indexOf(".wav") > -1) {
	        		corpus.add(fileEntry);
	        	}
	        }
	    }

	}
	
	public static void onlyFemales() {
		gender = "females";
	}
	
	public static void onlyMales() {
		gender = "males";
	}
	
	public static void all() {
		gender = "all";
	}
}
