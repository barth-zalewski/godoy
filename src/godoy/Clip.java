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

    private static final int DEFAULT_FRAME_SIZE = 1024;
    private static final int DEFAULT_OVERLAP = 2;
    
    private final List<Frame> frames = new ArrayList<Frame>();
    
    /**
     * Anzahl der Samples pro Frame. Muss eine Zweierpotenz sein (Anforderung vieler DFT-Routinen)
     */
    private final int frameSize;
    
    /**
     * Maß der Überlappung. Wert 1 heißt "keine Überlappung", 2 bedeutet, dass Frames
     * sich so überlappen, dass jeder Sample doppelt abgedeckt wird usw. Mehr Überlappung
     * bedeutet eine bessere Zeitauflösung.
     */ 
    private final int overlap;
    
    /**
     * The amount that the time samples are divided by before sending to the transformation,
     * and the amount they're multiplied after being transformed back.
     */
    private double spectralScale = 10000.0;

    private final String name;
 
    public static Clip newInstance(File file) throws UnsupportedAudioFileException, IOException {
        AudioFormat desiredFormat = AUDIO_FORMAT;
        BufferedInputStream in = new BufferedInputStream(AudioFileUtils.readAsMono(desiredFormat, file));
        return new Clip(file.getAbsolutePath(), in, DEFAULT_FRAME_SIZE, DEFAULT_OVERLAP);
    }
   
    private Clip(String name, InputStream in, int frameSize, int overlap) throws IOException {
        this.name = name;
        this.frameSize = frameSize;
        this.overlap = overlap;
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
                int sampVal = ((hi << 8) | low);
                samples[i] = (sampVal / spectralScale);
            }
            
            frames.add(new Frame(samples, windowFunc));
            in.reset();     
            long bytesToSkip = (frameSize * 2) / overlap;
            long bytesSkipped;
            if ((bytesSkipped = in.skip(bytesToSkip)) != bytesToSkip) {
                //logger.info("Skipped " + bytesSkipped + " bytes, but wanted " + bytesToSkip + " at frame " + frames.size());
            }
            in.mark(buf.length * 2);
        }
        logger.info("FrameSize:" + frames.size());
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


    public int getOverlap() {
        return overlap;
    }

    public AudioInputStream getAudio() {
        return getAudio(0);
    }
    
    public AudioInputStream getAudio(int sample) {
        return getAudio(sample, Integer.MAX_VALUE);
    }

    public AudioInputStream getAudio(int sample, int length) {

        final int initialFrame = sample / getFrameTimeSamples();
        
        InputStream audioData = new InputStream() {

            /**
             * Next frame to decode for playback.
             */
            int nextFrame = initialFrame;
            
            /**
             * A data structure that holds all the current frames of floating point samples
             * and performs the overlap-and-combine operation for us.
             */
            OverlapBuffer overlapBuffer = new OverlapBuffer(frameSize, overlap);
            
            /**
             * The current sample data. Only the lower 16 bits are significant.
             */
            int currentSample;
            
            /**
             * Flag to indicate if the current byte being read from the input stream
             * is the high byte or the low byte of a single 16-bit sample.
             */
            boolean currentByteHigh = true;
            
            int emptyFrameCount = 0;

            @Override
            public int available() throws IOException {
                return Integer.MAX_VALUE;
            }
            
            @Override
            public int read() throws IOException {
                if (overlapBuffer.needsNewFrame()) {
                    if (nextFrame < frames.size()) {
                        Frame f = frames.get(nextFrame++);
                        overlapBuffer.addFrame(f.asTimeData());
                    } else {
                        overlapBuffer.addEmptyFrame();
                        emptyFrameCount++;
                    }
                }
                
                if (emptyFrameCount >= overlap) {
                    return -1;
                } else if (currentByteHigh) {
                    currentSample = (int) (overlapBuffer.next() * spectralScale);
                    currentByteHigh = false;
                    return (currentSample >> 8) & 0xff;
                } else {
                    currentByteHigh = true;
                    return currentSample & 0xff;
                }
                
            }
            
        };
        int clipLength = getFrameCount() * getFrameTimeSamples() * (AUDIO_FORMAT.getSampleSizeInBits() / 8) / overlap;
        return new AudioInputStream(audioData, AUDIO_FORMAT, Math.min(length, clipLength));
    }

    public double getSamplingRate() {
        return AUDIO_FORMAT.getSampleRate();
    }

    public Clip subClip(int startFrame, int nFrames, int newFrameSize, int newOverlap) {
        InputStream in = null;
        try {
            // decode existing
            in = new BufferedInputStream(getAudio(startFrame * frameSize, nFrames * frameSize));

            // create new clip with new settings
            Clip subClip = new Clip("Teil von: " + name, in, newFrameSize, newOverlap);
            return subClip;
        } catch (IOException ex) {
            AssertionError err = new AssertionError("Fehler beim Resampeln.");
            err.initCause(ex);
            throw err;
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Laufzeitfehler.", ex);
            }
        }
    }

}
