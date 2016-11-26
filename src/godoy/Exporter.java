package godoy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
            	writer.write("Frame t = " + frames.get(i).getTimePosition() + "\r\n");
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
			int maxTrim = 99999999;
			
			for (int i = 0; i < frames.size(); i++) {
				double[] samples = frames.get(i).getSamples();
				
				/* Zuerst kleinsten und größten Sample finden */
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
				
				if (absMax > maxTrim) absMax = maxTrim;
				
				/* 3 Pixel pro Sample */
				int pixelsPerSample = 3,
				    width = samples.length * pixelsPerSample,
				    height = 201;							
				
				BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				
			    Graphics2D ig2 = bi.createGraphics();
		
		        ig2.setPaint(Color.white);
		        ig2.setColor(Color.white);
			    
			    ig2.fillRect(0, 0, width - 1, height - 1);
			    
			    //Horizontale Achse hinzufügen
			    ig2.setPaint(Color.gray);
		        ig2.drawLine(0, height / 2, width, height / 2);
		        
		        //Samples malen
		        int prevX = 0, prevY = height / 2;
		        
		        for (int s = 0; s < samples.length; s++) {					
				    int endX = s * pixelsPerSample, 
				    	endY;
				    
				    double sample = samples[s];
				    
				    if (sample > maxTrim) sample = maxTrim;
				    if (sample < -maxTrim) sample = -maxTrim;
				    
				    endY = (int)((height / 2) - ((height / 2) * (sample / absMax)));
				    
				    ig2.drawLine(prevX, prevY, endX, endY);
				    
				    prevX = endX;
				    prevY = endY;
				}
		        
			    ImageIO.write(bi, "BMP", new File("D:\\Uni\\Diplomarbeit\\Software\\samples\\frames\\fr-" + frames.get(i).getTimePosition() + ".bmp"));
			}
				      
	    } catch (IOException ie) {
	      logger.info("Bild nicht gespeichert");
	      ie.printStackTrace();
	    }

	}
	
	public void exportFramesWindowedSamples() {
		try {
			int maxTrim = 99999999;
			
			for (int i = 0; i < frames.size(); i++) {
				Map<Integer, double[]> windowedSamples1 = frames.get(i).getWindowedSamples1();
				Map<Integer, double[]> windowedSamples2 = frames.get(i).getWindowedSamples2();
				
				int numberOfWindows = windowedSamples1.size();
				
				for (int j = 0; j < numberOfWindows; j++) {
					double[] samples1 = windowedSamples1.get(j);
					double[] samples2 = windowedSamples2.get(j);
					
					/* Zuerst kleinsten und größten Sample finden */
					double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
					
					for (int s = 0; s < samples1.length; s++) {					
					    if (samples1[s] < min) {
					    	min = samples1[s];
					    }
					    if (samples1[s] > max) {
					    	max = samples1[s];
					    }
					    if (samples2[s] < min) {
					    	min = samples2[s];
					    }
					    if (samples2[s] > max) {
					    	max = samples2[s];
					    }
					}
					
					double absMax = Math.max(Math.abs(min), Math.abs(max));
					
					if (absMax > maxTrim) absMax = maxTrim;
					
					/* 3 Pixel pro Sample */
					int pixelsPerSample = 3,
					    width = samples1.length * pixelsPerSample,
					    height = 201;							
					
					BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					
				    Graphics2D ig2 = bi.createGraphics();
			
			        ig2.setPaint(Color.white);
			        ig2.setColor(Color.white);
				    
				    ig2.fillRect(0, 0, width - 1, height - 1);
				    
				    //Horizontale Achse hinzufügen
				    ig2.setPaint(Color.gray);
			        ig2.drawLine(0, height / 2, width, height / 2);
			        
			        //Samples malen
			        int prevX = 0, prevY = height / 2;
			        
			        ig2.setPaint(Color.blue);
			        
			        for (int s = 0; s < samples1.length; s++) {				        	
					    int endX = s * pixelsPerSample, 
					    	endY;
					    
					    double sample = samples1[s];
					    
					    if (sample > maxTrim) sample = maxTrim;
					    if (sample < -maxTrim) sample = -maxTrim;
					    
					    endY = (int)((height / 2) - ((height / 2) * (sample / absMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
			        
			        prevX = 0;
			        prevY = height / 2;
			        
			        ig2.setPaint(Color.red);
			        
			        for (int s = 0; s < samples2.length; s++) {				        	
					    int endX = s * pixelsPerSample, 
					    	endY;
					    
					    double sample = samples2[s];
					    
					    if (sample > maxTrim) sample = maxTrim;
					    if (sample < -maxTrim) sample = -maxTrim;
					    
					    endY = (int)((height / 2) - ((height / 2) * (sample / absMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
			        
			        File folder = new File("D:\\Uni\\Diplomarbeit\\Software\\samples\\frames\\fr-" + frames.get(i).getTimePosition());
			        folder.mkdirs();			        
			        
				    ImageIO.write(bi, "BMP", new File("D:\\Uni\\Diplomarbeit\\Software\\samples\\frames\\fr-" + frames.get(i).getTimePosition() + "\\win-" + j + ".bmp"));
				}
			}
				      
	    } catch (IOException ie) {
	      logger.info("Bild nicht gespeichert");
	      ie.printStackTrace();
	    }
	}
	
	public void exportSpectrums() {
		try {						
			for (int i = 0; i < frames.size(); i++) {
				Map<Integer, double[]> spectrums1 = frames.get(i).getSpectrums1();
				Map<Integer, double[]> spectrums2 = frames.get(i).getSpectrums2();
				
				int numberOfWindows = spectrums1.size();
				
				for (int j = 0; j < numberOfWindows; j++) {
					double[] spectrum1 = spectrums1.get(j);
					double[] spectrum2 = spectrums2.get(j);
					
					/* Zuerst kleinsten und größten Sample finden */
					double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
					
					for (int s = 0; s < spectrum1.length; s++) {					
					    if (spectrum1[s] < min) {
					    	min = spectrum1[s];
					    }
					    if (spectrum1[s] > max) {
					    	max = spectrum1[s];
					    }
					    if (spectrum2[s] < min) {
					    	min = spectrum2[s];
					    }
					    if (spectrum2[s] > max) {
					    	max = spectrum2[s];
					    }
					}
					
					double absMax = Math.max(Math.abs(min), Math.abs(max));
					
					/* 3 Pixel pro Sample */
					int pixelsPerFrequency = 20,
					    width = spectrum1.length * pixelsPerFrequency,
					    height = 301;							
					
					BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					
				    Graphics2D ig2 = bi.createGraphics();
			
			        ig2.setPaint(Color.white);
			        ig2.setColor(Color.white);
				    
				    ig2.fillRect(0, 0, width - 1, height - 1);
				    
				    //Horizontale Achse hinzufügen
				    ig2.setPaint(Color.gray);
			        ig2.drawLine(0, height / 2, width, height / 2);
			        
			        //Samples malen
			        int prevX = 0, prevY = height / 2;
			        
			        ig2.setPaint(Color.blue);
			        
			        for (int s = 0; s < spectrum1.length; s++) {				        	
					    int endX = s * pixelsPerFrequency, 
					    	endY;
					    
					    double frequency = spectrum1[s];					    
					    
					    endY = (int)((height / 2) - ((height / 2) * (frequency / absMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
			        
			        prevX = 0;
			        prevY = height / 2;
			        
			        ig2.setPaint(Color.red);
			        
			        for (int s = 0; s < spectrum2.length; s++) {				        	
					    int endX = s * pixelsPerFrequency, 
					    	endY;
					    
					    double frequency = spectrum2[s];
					    
					    endY = (int)((height / 2) - ((height / 2) * (frequency / absMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
			        
			        File folder = new File("D:\\Uni\\Diplomarbeit\\Software\\samples\\frames\\spectrum-" + frames.get(i).getTimePosition());
			        folder.mkdirs();			        
			        
				    ImageIO.write(bi, "BMP", new File("D:\\Uni\\Diplomarbeit\\Software\\samples\\frames\\spectrum-" + frames.get(i).getTimePosition() + "\\win-" + j + ".bmp"));
				}
			}
				      
	    } catch (IOException ie) {
	      logger.info("Bild nicht gespeichert");
	      ie.printStackTrace();
	    }
	}
}
