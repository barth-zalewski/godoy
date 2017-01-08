package godoy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class GlobalExporter {
	public static void exportHistogramm2D(LinkedHashMap<Double, double[]> histogramm2D) {		
		try {			
			int size = 800;
			
			BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
			
		    Graphics2D ig2 = bi.createGraphics();
	
	        ig2.setPaint(Color.white);
	        ig2.setColor(Color.white);
		    
		    ig2.fillRect(0, 0, size, size);
		    
		    int rectWidth = size / 20,
		    	rectHeight = size / histogramm2D.keySet().size();
		    
		    double maxHistogrammValue = Double.NEGATIVE_INFINITY;
		    
		    for (Map.Entry<Double, double[]> entry : histogramm2D.entrySet()) {						
		    	double[] histogramm = entry.getValue();				
				for (int i = 0; i < histogramm.length; i++) {
					maxHistogrammValue = Math.max(maxHistogrammValue, histogramm[i]);
				}
			}
		    
		    int offsetCounter = 0;
		    for (Map.Entry<Double, double[]> entry : histogramm2D.entrySet()) {				
		    	double[] histogramm = entry.getValue();				
				for (int i = 0; i < histogramm.length; i++) {
					int grayscale = 255 - (int)((histogramm[i] / maxHistogrammValue) * 255);
					Color gray = new Color(grayscale, grayscale, grayscale);
					ig2.setPaint(gray);
			        ig2.setColor(gray);
			        
			        ig2.fillRect(i * rectWidth, size - (offsetCounter + 1) * rectHeight, rectWidth, rectHeight);
				}
				
				offsetCounter++;
			}
		    
		    ImageIO.write(bi, "PNG", new File("D:\\Uni\\Diplomarbeit\\Software\\output\\histo\\", "histogramm-2d.png"));
		}
		catch(Exception ex) {
			System.out.println("Bild nicht gespeichert");
		    ex.printStackTrace();
		}
	
	}
	
}
