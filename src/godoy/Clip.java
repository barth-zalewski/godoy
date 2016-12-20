package godoy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Ein Clip repräsentiert einen Audio-Clip mit einer bestimmten Länge. Der Clip ist geteilt
 * in eine Serie von gleichgroßen Frames mit spektralen Infromationen. Auf die Frames
 * der spektralen Informationen kann in beliebiger Reihenfolge zugegriffen werden.
 */

public class Clip {

    private static final Logger logger = Logger.getLogger(Clip.class.getName());

    /**
     * Nur mit diesem Format können wir arbeiten.
     */
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000, 16, 1, true, true);

    private static final int DEFAULT_FRAME_SIZE = 480; //30 ms Frame
    
    private static final int DEFAULT_FRAME_SIZE_ZEROPADDED = 512;
    
    private final List<Frame> frames = new ArrayList<Frame>();
    
    private final PitchAnalyzer pitchAnalyzer;
    
    /**
     * Anzahl der Samples pro Frame. 
     */
    private final int frameSize;
    
    private final String name;
    
    private Exporter exporter;
    
    private Analyzer analyzer;
 
    public static Clip newInstance(File file, File pitchListingFile, double secondSpectrumOffset) throws UnsupportedAudioFileException, IOException {
        AudioFormat desiredFormat = AUDIO_FORMAT;
        BufferedInputStream in = new BufferedInputStream(AudioFileUtils.readAsMono(desiredFormat, file));
        return new Clip(file.getAbsolutePath(), in, pitchListingFile, secondSpectrumOffset);
    }
   
    private Clip(String name, InputStream in, File pitchListingFile, double secondSpectrumOffset) throws IOException {
        this.name = name;
        
        /* Pitch-Listing abarbeiten */
        pitchAnalyzer = new PitchAnalyzer(pitchListingFile);
        
        frameSize = DEFAULT_FRAME_SIZE;
        
        int frameSizeZeropadded = DEFAULT_FRAME_SIZE_ZEROPADDED;
        
        byte[] buf = new byte[frameSize * 2]; // 16-bit Monosamples
        int n;
        
        double timeCounter = pitchAnalyzer.initialTime();
        
        /* Anfangsbytes überspringen. Orientiert sich am ersten Listeneintrag aus der Pitch-Listing-Datei */
        in.skip(Math.round(pitchAnalyzer.initialTime() * AUDIO_FORMAT.getSampleRate()) * 2);
        
        while ((n = readFully(in, buf)) != -1) {             	
            if (n != buf.length) {                
                // Mit Nullen auffüllen, sonst gibt es eine hörbare Störung am Clipende
                for (int i = n; i < buf.length; i++) {
                    buf[i] = 0;
                }
            }
            
            /* Frame zur Analyse nur erzeugen, wenn alle 3 Steps stimmhaft */
            if (pitchAnalyzer.isVoiced(timeCounter) && pitchAnalyzer.isVoicedNext(timeCounter) && pitchAnalyzer.isVoiced2Next(timeCounter)) {            
	            double[] samples = new double[frameSizeZeropadded];
	            int isamp = 0;
	            for (; isamp < frameSize; isamp++) {
	                int hi = buf[2 * isamp];
	                int low = buf[2 * isamp + 1] & 0xff;
	                int sampVal = (hi << 8) | low;            	
	                samples[isamp] = sampVal;
	            }
	            
	            for (; isamp < frameSizeZeropadded; isamp++) {
	            	samples[isamp] = 0;
	            }
	            
	            double meanPitch = (pitchAnalyzer.getPitch(timeCounter) + pitchAnalyzer.getPitchNext(timeCounter) + pitchAnalyzer.getPitch2Next(timeCounter)) / 3;
	            
	            Frame fr = new Frame(samples, meanPitch, AUDIO_FORMAT.getSampleRate(), secondSpectrumOffset);
	            fr.setTimePosition(timeCounter);
	            
	            frames.add(fr);
            }
            
            timeCounter += 3 * pitchAnalyzer.timeStep(); //3, weil 30 ms für Analyse und 10 ms Praatausgabe
            
            //Wegen der Präzisionfehler...
            timeCounter = Math.round(timeCounter * 1000000.0) / 1000000.0;
                 
        }
        
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

    public int getFrameTimeSamples() {
        return frameSize;
    }

    public int getFrameFreqSamples() {
        return frameSize;
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

}
