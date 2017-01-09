package godoy;

import java.io.File;
import java.util.ArrayList;

public class Speaker {
	private String directoryName;
	
	private ArrayList<String> clipsFilenameStubs;
	
	private ArrayList<double[]> cVectors;
	
	public Speaker(String directoryName) {
		this.directoryName = directoryName;
		
		clipsFilenameStubs = new ArrayList<String>();		
		
		cVectors = new ArrayList<double[]>();
	}
	
	public void addClip(String fileName) {
		clipsFilenameStubs.add(fileName);
	}
	
	public void initializeClips() {
		System.out.println(directoryName);
		try {
			for (String fileStub : clipsFilenameStubs) {
				File wavFile = new File(fileStub + ".wav");
				File pitchListingFile = new File(fileStub + ".pitch");
				
				Clip clip = Clip.newInstance(wavFile, pitchListingFile, godoy.T_ANALYSIS_DELTA);	
				
				ArrayList<double[]> cVectorsThis = clip.createCharacteristicsVector();
				for (int i = 0; i < cVectorsThis.size(); i++) {
					cVectors.add(cVectorsThis.get(i));
				}				
			}
		}
		catch(Exception exc) {
			System.out.println("Initialisierung fehlgeschlagen.");
			exc.printStackTrace(System.out);
		}			
	}
	
	public ArrayList<double[]> getCharacteristicsVectors() {
		return cVectors;
	}
	
	public String getId() {
		return directoryName;
	}
}
