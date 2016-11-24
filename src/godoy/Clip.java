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
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(44100, 16, 1, true, true);

    private static final int DEFAULT_FRAME_SIZE = 1323; //30 ms Frame 
    
    private final List<Frame> frames = new ArrayList<Frame>();
    
    private final PitchAnalyzer pitchAnalyzer;
    
    /**
     * Anzahl der Samples pro Frame. 
     */
    private final int frameSize;
    
    private final String name;
    
    private Exporter exporter;
 
    public static Clip newInstance(File file, File pitchListingFile) throws UnsupportedAudioFileException, IOException {
        AudioFormat desiredFormat = AUDIO_FORMAT;
        BufferedInputStream in = new BufferedInputStream(AudioFileUtils.readAsMono(desiredFormat, file));
        return new Clip(file.getAbsolutePath(), in, pitchListingFile);
    }
   
    private Clip(String name, InputStream in, File pitchListingFile) throws IOException {
        this.name = name;
        
        /* Pitch-Listing abarbeiten */
        pitchAnalyzer = new PitchAnalyzer(pitchListingFile);
        
        frameSize = (int) (pitchAnalyzer.timeStep() * AUDIO_FORMAT.getSampleRate());
        
        byte[] buf = new byte[frameSize * 2]; // 16-bit Monosamples
        int n;
        
//        in.mark(buf.length * 2); // Probably not needed
        
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
            
            /* Frame zur Analyse nur erzeugen, wenn stimmhaft */
            if (pitchAnalyzer.isVoiced(timeCounter)) {            
	            double[] samples = new double[frameSize];
	            for (int i = 0; i < frameSize; i++) {
	                int hi = buf[2 * i];
	                int low = buf[2 * i + 1] & 0xff;
	                int sampVal = (hi << 8) | low;            	
	                samples[i] = sampVal;
	            }
	            
	            Frame fr = new Frame(samples, pitchAnalyzer.getPitch(timeCounter), AUDIO_FORMAT.getSampleRate());
	            fr.setTimePosition(timeCounter);
	            
	            frames.add(fr);
            }
            
            timeCounter += pitchAnalyzer.timeStep();
            
            //Wegen der Präzisionfehler...
            timeCounter = Math.round(timeCounter * 1000000.0) / 1000000.0;
            
//            in.reset();      // Probably not needed
//            long bytesToSkip = frameSize * 2; // Probably not needed
//            in.skip(bytesToSkip); // Probably not needed
//            in.mark(buf.length * 2);        // Probably not needed         
        }
        
//        Frame someFrame = frames.get(25);
        logger.info("counter=" + frames.size());
//        logger.info("ags=" + AUDIO_FORMAT.getSampleRate());
//        for (int i = 0; i < someFrame.getLength(); i++) {
//        	logger.info("fri(" + i + ")=" + someFrame.getReal(i));
//        }
        
        exporter = new Exporter(frames);
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
    
    public Exporter getExporter() {
    	return exporter;
    }

}
