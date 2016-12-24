package godoy;

import java.io.File;
import java.util.ArrayList;

public class Speaker {
	private String directoryName;
	
	private ArrayList<String> trainingClipsFilenameStubs;
	private ArrayList<String> recognitionClipsFilenameStubs;
	
	private ArrayList<Clip> trainingClips;
	private ArrayList<Clip> recognitionClips;
	
	private ArrayList<double[]> cVectors;
	
	public Speaker(String directoryName) {
		this.directoryName = directoryName;
		System.out.println("Speaker inited with: " + directoryName);
		trainingClipsFilenameStubs = new ArrayList<String>();
		recognitionClipsFilenameStubs = new ArrayList<String>();
		
		trainingClips = new ArrayList<Clip>();
		recognitionClips = new ArrayList<Clip>();
		
		cVectors = new ArrayList<double[]>();
	}
	
	public void addTrainingClip(String fileName) {
		trainingClipsFilenameStubs.add(fileName);
	}
	
	public void initializeTrainingClips() {
		try {
			for (String fileStub : trainingClipsFilenameStubs) {
				File wavFile = new File(fileStub + ".wav");
				File pitchListingFile = new File(fileStub + ".pitch");
				
				Clip clip = Clip.newInstance(wavFile, pitchListingFile, godoy.T_ANALYSIS_OFFSET);
				cVectors = clip.createCharacteristicsVector();
				System.out.println("cvectors.size()==" + cVectors.size());
			}
		}
		catch(Exception exc) {
			System.out.println("Initialisierung fehlgeschlagen.");
		}
	}
	
	public ArrayList<double[]> getCharacteristicsVectors() {
		return cVectors;
	}
}
