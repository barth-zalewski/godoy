package godoy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Ein Clip repr�sentiert eine Audio-Datei mit einer bestimmten L�nge. Der Clip ist geteilt
 * in eine Serie von gleichgro�en Frames mit spektralen Infromationen. Auf die Frames
 * der spektralen Informationen kann in beliebiger Reihenfolge zugegriffen werden.
 */

public class Clip {
    
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000, 16, 1, true, true);

    public static final double PRAAT_PITCH_RESOLUTION = 0.01; //10 ms zeitliche Aufl�sung der Pitch-Listings
    
    private static final int CHUNK_SIZE_10_MS = 160; //160 Samples pro 10-ms-Chunk
    
    private static final int DEFAULT_FRAME_SIZE = 3 * CHUNK_SIZE_10_MS; //30 ms Frame
    
    private final List<Frame> frames = new ArrayList<Frame>();
    
    private final PitchAnalyzer pitchAnalyzer;
    
    private Exporter exporter;
    
    private Analyzer analyzer;
    
    private ArrayList<double[]> characteristicsVectorSeries;
 
    public static Clip newInstance(File file, File pitchListingFile, double secondSpectrumOffset) throws UnsupportedAudioFileException, IOException {
        AudioFormat desiredFormat = AUDIO_FORMAT;
        BufferedInputStream in = new BufferedInputStream(AudioFileUtils.readAsMono(desiredFormat, file));
        return new Clip(file.getAbsolutePath(), in, pitchListingFile, secondSpectrumOffset);
    }
   
    private Clip(String name, InputStream in, File pitchListingFile, double secondSpectrumOffset) throws IOException {
        /* Pitch-Listing abarbeiten */
        pitchAnalyzer = new PitchAnalyzer(pitchListingFile);
        
        byte[] buf = new byte[CHUNK_SIZE_10_MS * 2]; // 16-bit Monosamples
        int n;
        
        double timeCounter = pitchAnalyzer.initialTime();
        
        ArrayList<double[]> chunks = new ArrayList<double[]>();
        
        /* Anfangsbytes �berspringen. Orientiert sich am ersten Listeneintrag aus der Pitch-Listing-Datei */
        in.skip(Math.round(pitchAnalyzer.initialTime() * AUDIO_FORMAT.getSampleRate()) * 2);

        while ((n = readFully(in, buf)) != -1) {             	
            if (n != buf.length) {                
                // Mit Nullen auff�llen, sonst gibt es eine h�rbare St�rung am Clipende
                for (int i = n; i < buf.length; i++) {
                    buf[i] = 0;
                }
            }
            
            double[] chunk = new double[CHUNK_SIZE_10_MS];
            
            for (int isamp = 0; isamp < CHUNK_SIZE_10_MS; isamp++) {
                int hi = buf[2 * isamp];
                int low = buf[2 * isamp + 1] & 0xff;
                int sampVal = (hi << 8) | low;            	
                chunk[isamp] = sampVal;
            }
            
            chunks.add(chunk);
        }
        

        /*
         *  Stimmhafte Frames erzeugen
         *  Annahme: Praat zentriert die Grundfrequenz-Angabe (f0 gilt von -5 ms bis +5 ms vom Zeitpunkt) 
         *  */
        
        for (int i = 1; i < chunks.size() - 2; i++) {
        	/* Frame zur Analyse nur erzeugen, wenn alle 3 Steps stimmhaft */
        	if (pitchAnalyzer.isVoiced(timeCounter) && pitchAnalyzer.isVoicedNext(timeCounter) && pitchAnalyzer.isVoiced2Next(timeCounter)) {
        		//den halben vorherigen Chunk nehmen, den aktuellen, den n�chsten und den halben �bern�chsten
        		double[] samples = new double[DEFAULT_FRAME_SIZE];
        		
        		int c = 0;
        		for (int fi = CHUNK_SIZE_10_MS / 2; fi < CHUNK_SIZE_10_MS; fi++, c++) {
        			samples[c] = chunks.get(i - 1)[fi];
        		}
        		for (int fi = 0; fi < CHUNK_SIZE_10_MS; fi++, c++) {
        			samples[c] = chunks.get(i)[fi];
        		}
        		for (int fi = 0; fi < CHUNK_SIZE_10_MS; fi++, c++) {
        			samples[c] = chunks.get(i + 1)[fi];
        		}
        		for (int fi = 0; fi < CHUNK_SIZE_10_MS / 2; fi++, c++) {
        			samples[c] = chunks.get(i + 2)[fi];
        		}
        		
        		double meanPitch = (pitchAnalyzer.getPitch(timeCounter) + pitchAnalyzer.getPitchNext(timeCounter) + pitchAnalyzer.getPitch2Next(timeCounter)) / 3;
	            
	            Frame frame = new Frame(samples, meanPitch, AUDIO_FORMAT.getSampleRate(), secondSpectrumOffset);
	            frame.setTimePosition(timeCounter);
	            
	            frames.add(frame);
        	}
        	
        	timeCounter += PRAAT_PITCH_RESOLUTION;
          
        	//Wegen der Pr�zisionfehler...
        	timeCounter = Math.round(timeCounter * 1000000.0) / 1000000.0;
        }
        
        System.out.println("frames.size() == " + frames.size());
        
        exporter = new Exporter(frames);
        analyzer = new Analyzer(frames);
        
        analyzer.trackPeaks();
        analyzer.peaksPositionsHistogramm();
        exporter.setAnalyzer(analyzer);
              
    }    

	private int readFully(InputStream in, byte[] buf) throws IOException {
        int offset = 0;
        int length = buf.length;
        int bytesRead = 0;
        while ((offset < buf.length) && ((bytesRead = in.read(buf, offset, length)) != -1)) {            
            length -= bytesRead;
            offset += bytesRead;            
        }        
        if (offset > 0) {            
            return offset;
        } 
        else {            
            return -1;
        }
    }
	
    public int getFrameCount() {
        return frames.size();
    }
    
    public Frame getFrame(int i) {
        return frames.get(i);
    }

    public double getSamplingRate() {
        return AUDIO_FORMAT.getSampleRate();
    }
    
    public static double getClassSamplingRate() {
        return AUDIO_FORMAT.getSampleRate();
    }
    
    public Exporter getExporter() {
    	return exporter;
    }
    
    public Analyzer getAnalyzer() {
    	return analyzer;
    }
    
    /* Analyse / Erkennung */
    public ArrayList<double[]> createCharacteristicsVector() {
    	characteristicsVectorSeries = new ArrayList<double[]>();
    	
    	ArrayList<double[]> cvAll = createCharacteristicsVectorDCTBased(), 
    						cv = new ArrayList<double[]>();
    		
    	cv.add(cvAll.get(0));
    	
    	for (int c = 1; c < cvAll.size(); c++) {
    		boolean isSameAsPrevious = true;
    		double[] previous = cvAll.get(c - 1), current = cvAll.get(c);
    		
    		for (int ci = 0; ci < previous.length; ci++) { //Sie haben garantiert die gleiche L�nge
    			if (previous[ci] != current[ci]) {
    				isSameAsPrevious = false;
    			}
    		}
    		
    		if (!isSameAsPrevious) {
    			cv.add(current);
    		}
    	}
    				
    	return cv;
    	
    	//return createCharacteristicsVectorGodoyBased();
    	//return createCharacteristicsVectorFunctionValueBased();
    	//return createCharacteristicsVectorCombinedMFCC_DCT();
    	//return createCharacteristicsVectorDCTBasedWithCepstrum();
    	//return createCharacteristicsVectorMFCCBased();    	
    	//return createCharacteristicsVectorCombined();
    }
    
    private ArrayList<double[]> createCharacteristicsVectorFunctionValueBased() {
    	for (int i = 0; i < frames.size(); i++) {
    		ArrayList<double[]> diffValues = frames.get(i).getDiffValues();
    		
    		for (int j = 0; j < diffValues.size(); j++) {
    			characteristicsVectorSeries.add(diffValues.get(j));    			
    		}    	    		
    	}
    	
    	return characteristicsVectorSeries;
    }
    
    private ArrayList<double[]> createCharacteristicsVectorDCTBased() {
    	for (int i = 0; i < frames.size(); i++) {
    		ArrayList<double[]> dctCoeeficients = frames.get(i).getDCTCoeffiencts();
    		
    		for (int j = 0; j < dctCoeeficients.size(); j++) {
    			characteristicsVectorSeries.add(dctCoeeficients.get(j));    			
    		}    	    		
    	}
    	
    	return characteristicsVectorSeries;
    }
    
    private ArrayList<double[]> createCharacteristicsVectorCombinedMFCC_DCT() {
    	final boolean CONSIDER_PITCH = true;    	
    	
    	for (int i = 0; i < frames.size(); i++) {
    		ArrayList<double[]> dctCoeeficients = frames.get(i).getDCTCoeffiencts();
    		ArrayList<double[]> mfccCoeeficients = frames.get(i).getMFCCCoeffiencts();
    		
    		//Es gibt eine unterschiedliche Anzahl DCT-Vektoren und nur einen MFCC_Vektor; und diese haben unterschiedliche L�ngen    		
    		
    		for (int j = 0; j < dctCoeeficients.size(); j++) {
    			double[] dctVector = dctCoeeficients.get(j),
    					 mfccVector = mfccCoeeficients.get(0);
    			double[] combinedVector = new double[dctVector.length + mfccVector.length + (CONSIDER_PITCH ? 1 : 0)];
    			
    			for (int k = 0; k < mfccVector.length; k++) {
    				combinedVector[k] = mfccVector[k];
    			}
    			for (int k = 0; k < dctVector.length; k++) {
    				combinedVector[k + mfccVector.length] = dctVector[k];
    			}
    			if (CONSIDER_PITCH) {
    				combinedVector[dctVector.length + mfccVector.length] = frames.get(i).getPitch();
    			}
    			
    			characteristicsVectorSeries.add(combinedVector);    			
    		}    	    		
    	}
    	
    	return characteristicsVectorSeries;
    }
    
    private ArrayList<double[]> createCharacteristicsVectorDCTBasedWithCepstrum() {
    	for (int i = 0; i < frames.size(); i++) {
    		ArrayList<double[]> dctCoeeficients = frames.get(i).getDCTCoeffienctsWithCepstrum();
    		
    		for (int j = 0; j < dctCoeeficients.size(); j++) {
    			characteristicsVectorSeries.add(dctCoeeficients.get(j));    			
    		}    	    		
    	}
    	
    	return characteristicsVectorSeries;
    }
    
    private ArrayList<double[]> createCharacteristicsVectorGodoyBased() {
    	double deepValleyFrequency = analyzer.getDeepValleyFrequency(),  
     		   cyclicPeakFrequency = analyzer.getCyclicPeakFrequency();  
     	
 		for (int i = 0; i < frames.size(); i++) {     		
     		double pitch = frames.get(i).getPitch();
     		
     		/* Nur ein pro Frame */
     		double[] combinedCoefficients = new double[3];
     		
 			combinedCoefficients[0] = deepValleyFrequency;     			
 		
 			combinedCoefficients[1] = cyclicPeakFrequency;
 			
 			combinedCoefficients[2] = pitch;     			     		
     		
     		characteristicsVectorSeries.add(combinedCoefficients);   	    		
     	}
 		
     	return characteristicsVectorSeries;
    }
    
    private ArrayList<double[]> createCharacteristicsVectorMFCCBased() {
    	for (int i = 0; i < frames.size(); i++) {
    		ArrayList<double[]> mfccCoeeficients = frames.get(i).getMFCCCoeffiencts();
    		
    		for (int j = 0; j < mfccCoeeficients.size(); j++) {    			
    			characteristicsVectorSeries.add(mfccCoeeficients.get(j));    			
    		}    	    		
    	}
    	
    	return characteristicsVectorSeries;
    }    
    
    private ArrayList<double[]> createCharacteristicsVectorCombinedOld() {
    	ArrayList<double[]> all = new ArrayList<double[]>();
    	
    	double[] histogramm = analyzer.getHistogramm();
		
		int maxI = -1;
		double maxPeak = -1;
		
		for (int i = 0; i < histogramm.length; i++) {					
			if (histogramm[i] > maxPeak) {
				maxPeak = histogramm[i];
				maxI = i;
			}
		}
		
    	for (int i = 0; i < frames.size(); i++) {
    		ArrayList<double[]> combinedCoeeficients = frames.get(i).getCombinedCoefficients(maxI);
    		
    		for (int j = 0; j < combinedCoeeficients.size(); j++) {    			
    			characteristicsVectorSeries.add(combinedCoeeficients.get(j));    			
    		}    	    		
    	}
    	
    	return characteristicsVectorSeries;
    }    

    /* MFCC + Deep valley frequency + Cylic peak frequency + Pitch */
    private ArrayList<double[]> createCharacteristicsVectorCombined() {
    	double deepValleyFrequency = analyzer.getDeepValleyFrequency(),  
    		   cyclicPeakFrequency = analyzer.getCyclicPeakFrequency();  
    	
    	/* Local configurations */
    	final boolean CONSIDER_DEEP_VALLEY_FQ = true,
    				  CONSIDER_CYCLIC_PEAK_FQ = true,
    				  CONSIDER_PITCH = false;
    	
    	int moreOfIndex = 0;
    	
    	if (CONSIDER_DEEP_VALLEY_FQ) moreOfIndex++;
    	if (CONSIDER_CYCLIC_PEAK_FQ) moreOfIndex++;
    	if (CONSIDER_PITCH) moreOfIndex++;
    	
		for (int i = 0; i < frames.size(); i++) {
    		ArrayList<double[]> mfccCoefficientsAL = frames.get(i).getMFCCCoeffiencts();
    		double pitch = frames.get(i).getPitch();
    		
    		/* Nur ein pro Frame */
    		double[] mfccCoefficients = mfccCoefficientsAL.get(0);
    		double[] combinedCoefficients = new double[mfccCoefficients.length + moreOfIndex];
    		
    		for (int cc = 0; cc < mfccCoefficients.length; cc++) {
    			combinedCoefficients[cc] = mfccCoefficients[cc];
    		}
    		
    		int additionalIndex = 0;
    		
    		if (CONSIDER_DEEP_VALLEY_FQ) {
    			combinedCoefficients[mfccCoefficients.length + additionalIndex] = deepValleyFrequency;
    			additionalIndex++;
    		}
    		
    		if (CONSIDER_CYCLIC_PEAK_FQ) {
    			combinedCoefficients[mfccCoefficients.length + additionalIndex] = cyclicPeakFrequency;
    			additionalIndex++;
    		}
    		
    		if (CONSIDER_PITCH) {
    			combinedCoefficients[mfccCoefficients.length + additionalIndex] = pitch;
    			additionalIndex++;
    		}
    		
    		characteristicsVectorSeries.add(combinedCoefficients);   	    		
    	}
    	return characteristicsVectorSeries;
    }
}
