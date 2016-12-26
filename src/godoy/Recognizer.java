package godoy;

import java.util.ArrayList;

public class Recognizer {
	
	
	
	public void train(ArrayList<Speaker> corpus) {
		int lengthOfGroupArray = 0;
		for (int i = 0; i < corpus.size(); i++) {
			lengthOfGroupArray += corpus.get(i).getCharacteristicsVectors().size();
		}
		
		int[] groups = new int[lengthOfGroupArray];
	}
	
	public void recognize() {
		
	}
}
