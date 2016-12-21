package godoy;

import java.io.File;
import java.util.ArrayList;

public class CorpusExtractor {
	
	private static String gender = "*";
	private static ArrayList<String> corpus = new ArrayList<String>();
	
	public static ArrayList<String> getCorpus(String basePath) {
		if (gender.equals("*") || gender.equals("females")) {			
			File baseFolder = new File(basePath + "\\females");
			listFiles(baseFolder);
		}
		if (gender.equals("*") || gender.equals("males")) {			
			File baseFolder = new File(basePath + "\\males");
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
	        		String path = fileEntry.getAbsolutePath(),
	        			   absolutePathWithoutExtension = path.substring(0, path.lastIndexOf('.'));
	        		corpus.add(absolutePathWithoutExtension);	        		
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
		gender = "*";
	}
}
