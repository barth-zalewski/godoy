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
 * Ein Clip repräsentiert eine Audio-Datei mit einer bestimmten Länge. Der Clip ist geteilt
 * in eine Serie von gleichgroßen Frames mit spektralen Infromationen. Auf die Frames
 * der spektralen Informationen kann in beliebiger Reihenfolge zugegriffen werden.
 */

public class Clip {
    
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000, 16, 1, true, true);

    public static final double PRAAT_PITCH_RESOLUTION = 0.01; //10 ms zeitliche Auflösung der Pitch-Listings
    
    private static final int CHUNK_SIZE_10_MS = 160; //160 Samples pro 10-ms-Chunk
    
    private static final int DEFAULT_FRAME_SIZE = 3 * CHUNK_SIZE_10_MS; //30 ms Frame
    
    private final List<Frame> frames = new ArrayList<Frame>();
    
    private final PitchAnalyzer pitchAnalyzer;
    
    private final int frameSize;
    
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
        
        frameSize = DEFAULT_FRAME_SIZE;
        
        byte[] buf = new byte[CHUNK_SIZE_10_MS * 2]; // 16-bit Monosamples
        int n;
        
        double timeCounter = pitchAnalyzer.initialTime();
        
        ArrayList<double[]> chunks = new ArrayList<double[]>();
        
        /* Anfangsbytes überspringen. Orientiert sich am ersten Listeneintrag aus der Pitch-Listing-Datei */
        in.skip(Math.round(pitchAnalyzer.initialTime() * AUDIO_FORMAT.getSampleRate()) * 2);

        while ((n = readFully(in, buf)) != -1) {             	
            if (n != buf.length) {                
                // Mit Nullen auffüllen, sonst gibt es eine hörbare Störung am Clipende
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
        		//den halben vorherigen Chunk nehmen, den aktuellen, den nächsten und den halben übernächsten
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
          
        	//Wegen der Präzisionfehler...
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
    	//return createCharacteristicsVectorGodoyBased();
    	//return createCharacteristicsVectorFunctionValueBased();
    	//return createCharacteristicsVectorDCTBased();
    	//return createCharacteristicsVectorMFCCBased();    	
    	return createCharacteristicsVectorCombined();
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
    
    private ArrayList<double[]> createCharacteristicsVectorGodoyBased() {
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
			ArrayList<double[]> peaksCoordinates = frames.get(i).getPeaksCoordinates(maxI);   	
			
			for (int j = 0; j < peaksCoordinates.size(); j++) {
				all.add(peaksCoordinates.get(j));    			
    		}
    	}
//		
//		double[] sums = new double[all.get(0).length];
//		
//		int chunksSize = 3;
//		
//		for (int i = 0; i < all.size(); i++) {
//			double[] thisOne = all.get(i);
//			
//			if (i % chunksSize == 0 && i != 0) {
//				double[] chunked = new double[thisOne.length];
//				for (int j = 0; j < sums.length; j++) {
//					chunked[j] = sums[j] / chunksSize;
//					sums[j] = 0;
//				}		
//				
//				characteristicsVectorSeries.add(chunked);
//			}
//			
//			
//			for (int j = 0; j < thisOne.length; j++) {
//				sums[j] += thisOne[j];
//			}
//		}		
		
    	return all;
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
//    
//    private ArrayList<double[]>createCharacteristicsVectorCombined() {
//    	ArrayList<double[]> all = new ArrayList<double[]>();
//    	
//    	int[] histogramm = analyzer.getHistogramm();
//		
//		int maxI = -1, maxPeak = -1;
//		
//		for (int i = 0; i < histogramm.length; i++) {					
//			if (histogramm[i] > maxPeak) {
//				maxPeak = histogramm[i];
//				maxI = i;
//			}
//		}
//		
//    	for (int i = 0; i < frames.size(); i++) {
//    		ArrayList<double[]> combinedCoeeficients = frames.get(i).getCombinedCoefficients(maxI);
//    		
//    		for (int j = 0; j < combinedCoeeficients.size(); j++) {    			
//    			characteristicsVectorSeries.add(combinedCoeeficients.get(j));    			
//    		}    	    		
//    	}
//    	
//    	return characteristicsVectorSeries;
//    }    

    private ArrayList<double[]>createCharacteristicsVectorCombined() {
    	double deepValleyFrequency = analyzer.getDeepValleyFrequency();    	
    	
		for (int i = 0; i < frames.size(); i++) {
    		ArrayList<double[]> mfccCoefficientsAL = frames.get(i).getMFCCCoeffiencts();
    		
    		/* Nur ein pro Frame */
    		double[] mfccCoefficients = mfccCoefficientsAL.get(0);
    		double[] combinedCoefficients = new double[mfccCoefficients.length + 1];
    		
    		for (int cc = 0; cc < mfccCoefficients.length; cc++) {
    			combinedCoefficients[cc] = mfccCoefficients[cc];
    		}
    		combinedCoefficients[mfccCoefficients.length] = deepValleyFrequency;
    		
    		characteristicsVectorSeries.add(combinedCoefficients);   	    		
    	}
    	return characteristicsVectorSeries;
    }
}
