package godoy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
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
    
    /**
     * Anzahl der Samples pro Frame. 
     */
    private final int frameSize;
    
    private final String name;
    
    private Exporter exporter;
 
    public static Clip newInstance(File file) throws UnsupportedAudioFileException, IOException {
        AudioFormat desiredFormat = AUDIO_FORMAT;
        BufferedInputStream in = new BufferedInputStream(AudioFileUtils.readAsMono(desiredFormat, file));
        return new Clip(file.getAbsolutePath(), in, DEFAULT_FRAME_SIZE);
    }
   
    private Clip(String name, InputStream in, int frameSize) throws IOException {
        this.name = name;
        this.frameSize = frameSize;
        
        WindowFunction windowFunc = new VorbisWindowFunction(frameSize);
        
        byte[] buf = new byte[frameSize * 2]; // 16-bit mono samples
        int n;
        in.mark(buf.length * 2);
        while ((n = readFully(in, buf)) != -1) {             	
            if (n != buf.length) {                
                // Mit Nullen auffüllen, sonst gibt es eine hörbare Störung am Clipende
                for (int i = n; i < buf.length; i++) {
                    buf[i] = 0;
                }
            }
            double[] samples = new double[frameSize];
            for (int i = 0; i < frameSize; i++) {
                int hi = buf[2 * i];
                int low = buf[2 * i + 1] & 0xff;
                int sampVal = (hi << 8) | low;            	
                samples[i] = sampVal;
            }
            
            frames.add(new Frame(samples, windowFunc));
            
            in.reset();     
            long bytesToSkip = frameSize * 2;
            in.skip(bytesToSkip);
            in.mark(buf.length * 2);                 
        }
        
        Frame someFrame = frames.get(25);
        logger.info("counter=" + frames.size());
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
