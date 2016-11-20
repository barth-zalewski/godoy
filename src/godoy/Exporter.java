package godoy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * Diese Klasse dient dem Export von Daten
 */


public class Exporter {
	private final List<Frame> frames;
	
	private static final Logger logger = Logger.getLogger(Exporter.class.getName());
	
	public Exporter(List<Frame> data) {
		frames = data;
	}
	
	/* Exportiert ein Mischmasch an Daten */
	public void exportAsTXT() {
		BufferedWriter writer = null;
        try {        
            File logFile = new File("D:\\Uni\\Diplomarbeit\\Software\\samples", "export.txt");
            writer = new BufferedWriter(new FileWriter(logFile));
            writer.write("Export\r\n");
            writer.write("===========\r\n");
            
            for (int i = 0; i < frames.size(); i++) {
            	writer.write("Frame #" + i + "\r\n");
            	double[] samples = frames.get(i).getSamples();
            	
            	for (int s = 0; s < samples.length; s++) {
            		writer.write(samples[s] + "\r\n");
            	}
            	
            	writer.write("\r\n");
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        } 
        finally {
            try {                
                writer.close();
            } 
            catch (Exception e) { }
        }

	}
	
	public void exportFramesSamples() {
		try {
			for (int i = 0; i < frames.size(); i++) {
				double[] samples = frames.get(i).getSamples();
				
				/* Zuerst kleinsten und gr��ten Sample finden */
				double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
				
				for (int s = 0; s < samples.length; s++) {					
				    if (samples[s] < min) {
				    	min = samples[s];
				    }
				    if (samples[s] > max) {
				    	max = samples[s];
				    }
				}
				
				double absMax = Math.max(Math.abs(min), Math.abs(max));
				
				/* 3 Pixel pro Sample */
				int pixelsPerSample = 3,
				    width = samples.length * pixelsPerSample,
				    height = 200;							
				
				BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				
			    Graphics2D ig2 = bi.createGraphics();
		
		        ig2.setPaint(Color.white);
		        ig2.setColor(Color.white);
			    
			    ig2.fillRect(0, 0, width - 1, height - 1);
			    
			    ig2.setPaint(Color.blue);
		        
		        ig2.drawLine(0, 0, 100, 100);
		        
			    ImageIO.write(bi, "BMP", new File("D:\\Uni\\Diplomarbeit\\Software\\samples\\frames\\fr-" + i + ".bmp"));
			}
				      
	    } catch (IOException ie) {
	      logger.info("Bild nicht gespeichert");
	      ie.printStackTrace();
	    }

	}
}