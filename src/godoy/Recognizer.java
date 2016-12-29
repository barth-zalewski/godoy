package godoy;

import java.util.ArrayList;
import java.util.HashMap;

public class Recognizer {
	
	HashMap<Integer, Speaker> groupNumberToSpeaker;
	LDA lda;
	
	public Recognizer() {
		groupNumberToSpeaker = new HashMap<Integer, Speaker>();		
	}
	
	public void train(ArrayList<Speaker> corpus) {		
		int lengthOfGroupArray = 0;
		for (int i = 0; i < corpus.size(); i++) {
			lengthOfGroupArray += corpus.get(i).getCharacteristicsVectors().size();			
		}
		
		int[] groups = new int[lengthOfGroupArray];
		double[][] data = new double[lengthOfGroupArray][godoy.NUMBER_FIRST_DCT_COEFFICIENTS_FOR_CHARACTERISTICS_VECTOR];
		String groupsAsString = "";
		
		int c = 0;
		
		for (int i = 0; i < corpus.size(); i++) {
			//Um im Nachhinein zu wissen, welche Gruppe welchem Sprecher entspricht
			groupNumberToSpeaker.put(i + 1, corpus.get(i));
			
			for (int j = 0; j < corpus.get(i).getCharacteristicsVectors().size(); j++) {
				groups[c] = i + 1;
				groupsAsString += "[" + (c) + "]=" + (i + 1) + ",";
				data[c] = corpus.get(i).getCharacteristicsVectors().get(j);
				c++;
			}
		}
		System.out.println("groups=" + groupsAsString);
		lda = new LDA(data, groups, true);
	}
	
	public HashMap<String, Integer> recognize(Speaker unknownSpeaker) {
		ArrayList<Speaker> recognizedAs = new ArrayList<Speaker>();
		ArrayList<double[]> vectors = unknownSpeaker.getCharacteristicsVectors();
		
		for (int i = 0; i < vectors.size(); i++) {
			int predictedGroup = lda.predict(vectors.get(i));
			Speaker recognizedSpeaker = groupNumberToSpeaker.get(predictedGroup);
			recognizedAs.add(recognizedSpeaker);			
		}
		
		HashMap<String, Integer> estimatedProbabilities = new HashMap<String, Integer>();
		
		for (int i = 0; i < recognizedAs.size(); i++) {
			Speaker ithSpeaker = recognizedAs.get(i);
			if (estimatedProbabilities.get(ithSpeaker.getId()) == null) {
				estimatedProbabilities.put(ithSpeaker.getId(), 0);
			}
			
			estimatedProbabilities.put(ithSpeaker.getId(), estimatedProbabilities.get(ithSpeaker.getId()) + 1);
		}
		
		for (HashMap.Entry<String, Integer> estimate : estimatedProbabilities.entrySet()) {
			estimatedProbabilities.put(estimate.getKey(), (int)(100 * estimate.getValue() / vectors.size()));
		}
		
		return estimatedProbabilities;
	}
}
