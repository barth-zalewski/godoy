package godoy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
	
	private Analyzer analyzer;
	
	public Exporter(List<Frame> data) {
		frames = data;
	}

	/* Exportiert ein Frame, die dazugeh�rige Fensterfunktion und das Spektrum */
	public void exportFrames() {
		try {			
			for (int i = 0; i < frames.size(); i++) {
				double[] samples = frames.get(i).getAllSamples();
				
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
				    height = 201;							
				
				BufferedImage bi = new BufferedImage(width, 2 * height, BufferedImage.TYPE_INT_RGB);
				
			    Graphics2D ig2 = bi.createGraphics();
		
		        ig2.setPaint(Color.white);
		        ig2.setColor(Color.white);
			    
			    ig2.fillRect(0, 0, width - 1, 2 * height - 1);
			    
			    //Horizontale Achse hinzuf�gen
			    ig2.setPaint(Color.gray);
		        ig2.drawLine(0, height / 2, width, height / 2);
		        
		        //Samples malen
		        int prevX = 0, prevY = height / 2;
		        
		        for (int s = 0; s < samples.length; s++) {					
				    int endX = s * pixelsPerSample, 
				    	endY;
				    
				    double sample = samples[s];				    
				    
				    endY = (int)((height / 2) - ((height / 2) * (sample / absMax)));
				    
				    ig2.drawLine(prevX, prevY, endX, endY);
				    
				    prevX = endX;
				    prevY = endY;
				}
		        
		        //Fenster hinzuf�gen
		        double[] window = new double[samples.length];
		        for (int wi = 0; wi < window.length; wi++) {
		        	window[wi] = absMax;
		        }
		        frames.get(i).getWindowFuncWholeFrame().applyWindow(window);
		        
		        prevX = 0;
		        prevY = height / 2;
		        
		        ig2.setPaint(Color.orange);
		        
		        for (int s = 0; s < window.length; s++) {					
				    int endX = s * pixelsPerSample, 
				    	endY;
				    
				    double sample = window[s];				    
				    
				    endY = (int)((height / 2) - ((height / 2) * (sample / absMax)));
				    
				    ig2.drawLine(prevX, prevY, endX, endY);
				    
				    prevX = endX;
				    prevY = endY;
				}
		        
		        //Envelope hinzuf�gen
//		        prevX = 0;
//		        prevY = height / 2;
//		        
//		        ig2.setPaint(Color.magenta);
		        
		        //Trennlinie hinzuf�gen
		        ig2.setPaint(Color.black);
		        ig2.drawLine(0, height, width, height);
		        
		        //Spektrum hinzuf�gen
		        //Achse hinzuf�gen
		        ig2.setPaint(Color.gray);
		        ig2.drawLine(0, height + height / 2, width, height + height / 2);
		        
		        prevX = 0;
		        prevY = height + height / 2;
		        
		        ig2.setPaint(Color.blue);
		        
		        double[] spectrum = frames.get(i).getWholeFrameSpectrum();
		        
		        double spectrumMin = Double.POSITIVE_INFINITY, spectrumMax = Double.NEGATIVE_INFINITY;
				
				for (int s = 0; s < spectrum.length; s++) {					
				    if (spectrum[s] < spectrumMin) {
				    	spectrumMin = spectrum[s];
				    }
				    if (spectrum[s] > spectrumMax) {
				    	spectrumMax = spectrum[s];
				    }
				}
				
				double spectrumAbsMax = Math.max(Math.abs(spectrumMin), Math.abs(spectrumMax));
				
				for (int s = 0; s < spectrum.length; s++) {					
				    int endX = s * pixelsPerSample * 2, //2, da FFT um die H�lfte k�rzer ist als das Input 
				    	endY;
				    
				    double sample = spectrum[s];				    
				    
				    endY = (int)(height + (height / 2) - ((height / 2) * (sample / spectrumAbsMax)));
				    
				    ig2.drawLine(prevX, prevY, endX, endY);
				    
				    prevX = endX;
				    prevY = endY;
				}
		        
			    ImageIO.write(bi, "BMP", new File("D:\\Uni\\Diplomarbeit\\Software\\output\\frames\\fr-" + frames.get(i).getTimePosition() + ".bmp"));
			}
				      
	    } catch (IOException ie) {
	      logger.info("Bild nicht gespeichert");
	      ie.printStackTrace();
	    }

	}
	
	/* Exportiert ein Framebild mit eingezeichneten Fenstern, gefensterte Snapshots und dazugeh�rige Spektren */
	public void exportFramesWindowedSamples() {
		try {
			for (int i = 0; i < frames.size(); i++) {
				//if (i != 130 && i != 55) continue; //#
								
				Map<Integer, double[]> windowedSamples1 = frames.get(i).getSnapshots1();
				Map<Integer, double[]> windowedSamples2 = frames.get(i).getSnapshots2();
				
				double[] allSamples = frames.get(i).getAllSamples();
				
				Map<Integer, double[]> spectrums1 = frames.get(i).getSpectrums1();
				Map<Integer, double[]> spectrums2 = frames.get(i).getSpectrums2();
				
				int numberOfWindows = windowedSamples1.size();
				
				for (int j = 0; j < numberOfWindows; j++) {
					if (j != 150) continue; //#
					
					double[] samples1 = windowedSamples1.get(j);
					double[] samples2 = windowedSamples2.get(j);
					
					/* 3 Pixel pro Sample */
					int pixelsPerSample = 3,
					    width = allSamples.length * pixelsPerSample,
					    height = 201;							
					
					BufferedImage bi = new BufferedImage(width, 4 * height, BufferedImage.TYPE_INT_RGB);
					
				    Graphics2D ig2 = bi.createGraphics();
			
			        ig2.setPaint(Color.white);
			        ig2.setColor(Color.white);
				    
				    ig2.fillRect(0, 0, width - 1, 4 * height - 1);
				    
				    /* Gesamtes Frame zeichnen */				    
				    
				    //Horizontale Achse hinzuf�gen
				    ig2.setPaint(Color.gray);
			        ig2.drawLine(0, height / 2, width, height / 2);
			        
			        ig2.setPaint(Color.black);
			        
			        int prevX = 0, prevY = height / 2;
			        
			        double allMin = Double.POSITIVE_INFINITY, allMax = Double.NEGATIVE_INFINITY;
					
			        //Samples hinzuf�gen
					for (int s = 0; s < allSamples.length; s++) {					
					    if (allSamples[s] < allMin) {
					    	allMin = allSamples[s];
					    }
					    if (allSamples[s] > allMax) {
					    	allMax = allSamples[s];
					    }
					}
					
					double allAbsMax = Math.max(Math.abs(allMin), Math.abs(allMax));
					
					for (int s = 0; s < allSamples.length; s++) {					
					    int endX = s * pixelsPerSample, 
					    	endY;
					    
					    double sample = allSamples[s];				    
					    
					    endY = (int)((height / 2) - ((height / 2) * (sample / allAbsMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
					
					//Envelope zeichnen
					ig2.setPaint(Color.cyan);
					
					prevX = 0;
					prevY = height / 2;
					
					double[] env = frames.get(i).getEnvelope();
					
					for (int s = 0; s < env.length; s++) {					
					    int endX = s * pixelsPerSample, 
					    	endY;
					    
					    double sample = env[s];				    
					    
					    endY = (int)((height / 2) - ((height / 2) * (sample / allAbsMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
					
					//Fenster hinzuf�gen
					int samplesPerWindow = frames.get(i).getSamplesPerWindow(),
					    samplesPerWindowHalf = samplesPerWindow / 2;
					
					ig2.setPaint(Color.gray);
					
					int firstWindowCenter = j * pixelsPerSample + samplesPerWindowHalf * pixelsPerSample;
					ig2.drawLine(firstWindowCenter, 0, firstWindowCenter, height);
					
					int secondWindowCenter = firstWindowCenter + (int)(frames.get(i).getSamplesPerPeriod() * frames.get(i).getSecondSpectrumOffset()) * pixelsPerSample;
					ig2.drawLine(secondWindowCenter, 0, secondWindowCenter, height);
					
					double[] window = new double[samplesPerWindow];
					for (int wi = 0; wi < samplesPerWindow; wi++) {
						window[wi] = allAbsMax;
					}
					WindowFunction winFunc = new HammingWindowFunction(window.length);
					winFunc.applyWindow(window);
					
					ig2.setPaint(Color.pink);
					
					prevX = firstWindowCenter - samplesPerWindowHalf * pixelsPerSample;
					prevY = height / 2;
					
					for (int s = 0; s < window.length; s++) {					
					    int endX = prevX + pixelsPerSample, 
					    	endY;
					    
					    double sample = window[s];				    
					    
					    endY = (int)((height / 2) - ((height / 2) * (sample / allAbsMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
					
					prevX = secondWindowCenter - samplesPerWindowHalf * pixelsPerSample;
					prevY = height / 2;
					
					for (int s = 0; s < window.length; s++) {					
					    int endX = prevX + pixelsPerSample, 
					    	endY;
					    
					    double sample = window[s];				    
					    
					    endY = (int)((height / 2) - ((height / 2) * (sample / allAbsMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
			        
			        /* Gefensterte Snapshots zeichnen */
			        pixelsPerSample = (int)(width / samples1.length);
			        
			        /* Zuerst kleinsten und gr��ten Sample finden */
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
					
			        prevX = 0;
			        prevY = height + height / 2;
			        
			        //Horizontale Achse hinzuf�gen
				    ig2.setPaint(Color.gray);
			        ig2.drawLine(0, height + height / 2, width, height + height / 2);
			        
			        //Snapshots 1
			        ig2.setPaint(Color.blue);
			        
			        for (int s = 0; s < samples1.length; s++) {				        	
					    int endX = s * pixelsPerSample, 
					    	endY;
					    
					    double sample = samples1[s];					    
					    
					    endY = (int)(height + (height / 2) - ((height / 2) * (sample / absMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
			        
			        //Snapshots 2
			        
			        prevX = 0;
			        prevY = height * 2 + height / 2;
			        
			        //Horizontale Achse hinzuf�gen
				    ig2.setPaint(Color.gray);
			        ig2.drawLine(0, height * 2 + height / 2, width, height * 2 + height / 2);
			        
			        ig2.setPaint(Color.red);
			        			        
			        for (int s = 0; s < samples2.length; s++) {				        	
					    int endX = s * pixelsPerSample, 
					    	endY;
					    
					    double sample = samples2[s];
					    
					    endY = (int)(2 * height + (height / 2) - ((height / 2) * (sample / absMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
			        
			        //Spektren 
			        double[] spectrum1 = spectrums1.get(j);
					double[] spectrum2 = spectrums2.get(j);
					
					/* Zuerst kleinsten und gr��ten Sample finden */
					double spMin = Double.POSITIVE_INFINITY, spMax = Double.NEGATIVE_INFINITY;
					
					for (int s = 0; s < spectrum1.length; s++) {					
					    if (spectrum1[s] < spMin) {
					    	spMin = spectrum1[s];
					    }
					    if (spectrum1[s] > spMax) {
					    	spMax = spectrum1[s];
					    }
					    
					    if (spectrum2[s] < spMin) {
					    	spMin = spectrum2[s];
					    }
					    if (spectrum2[s] > spMax) {
					    	spMax = spectrum2[s];
					    }
					    
					    double diff = spectrum1[s] - spectrum2[s];
					    
					    if (diff < spMin) {
					    	spMin = diff;
					    }
					    if (diff > spMax) {
					    	spMax = diff;
					    }
					}
					
					double spAbsMax = Math.max(Math.abs(spMin), Math.abs(spMax));
					
					int pixelsPerFrequency = width / spectrum1.length;
					
					prevX = 0;
					prevY = height * 3 + height / 2;
			        
			        ig2.setPaint(Color.green);
			        
			        for (int s = 0; s < spectrum1.length; s++) {				        	
					    int endX = s * pixelsPerFrequency, 
					    	endY;
					    
					    double frequency = spectrum1[s];					    
					    
					    endY = (int)(height * 3 + (height / 2) - ((height / 2) * (frequency / spAbsMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
			        
			        prevX = 0;
			        prevY = height * 3 + height / 2;
			        
			        ig2.setPaint(Color.red);
			        
			        for (int s = 0; s < spectrum2.length; s++) {				        	
					    int endX = s * pixelsPerFrequency, 
					    	endY;
					    
					    double frequency = spectrum2[s];
					    
					    endY = (int)(height * 3 + (height / 2) - ((height / 2) * (frequency / spAbsMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
			        
			        //Differenz malen
			        prevX = 0;
			        prevY = height * 3 + height / 2;
			        
			        ig2.setPaint(Color.black);
			        
			        for (int s = 0; s < spectrum1.length; s++) {				        	
					    int endX = s * pixelsPerFrequency, 
					    	endY;
					    
					    double frequency1 = spectrum1[s], frequency2 = spectrum2[s];
					    
					    endY = (int)(height * 3 + (height / 2) - ((height / 2) * ((frequency1 - frequency2) / spAbsMax)));
					    
					    ig2.drawLine(prevX, prevY, endX, endY);
					    
					    prevX = endX;
					    prevY = endY;
					}
			        
			        File folder = new File("D:\\Uni\\Diplomarbeit\\Software\\output\\snapshots\\fr-" + frames.get(i).getTimePosition());
			        folder.mkdirs();			        
			        
				    ImageIO.write(bi, "PNG", new File("D:\\Uni\\Diplomarbeit\\Software\\output\\snapshots\\fr-" + frames.get(i).getTimePosition() + "\\win-" + j + ".png"));
				    
				    //if (j == 3) break;
				}
				
				//if (i == 0) break;
			}
				      
	    } catch (IOException ie) {
	      logger.info("Bild nicht gespeichert");
	      ie.printStackTrace();
	    }
	}
	
	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}
	
	public void exportPeakPositions() {
		ArrayList<double[]> stDevsByFrame = analyzer.getPeaksByFrame();
		
		try {
			for (int i = 0; i < frames.size(); i++) {
				double[] stDevByFrame = stDevsByFrame.get(i);
				
				double[] allSamples = frames.get(i).getAllSamples();
				
				/* 3 Pixel pro Sample */
				int pixelsPerSample = 3,
				    width = allSamples.length * pixelsPerSample,
				    height = 201;							
				
				BufferedImage bi = new BufferedImage(width, 2 * height, BufferedImage.TYPE_INT_RGB);
				
			    Graphics2D ig2 = bi.createGraphics();
		
		        ig2.setPaint(Color.white);
		        ig2.setColor(Color.white);
			    
			    ig2.fillRect(0, 0, width - 1, 2 * height - 1);
			    
			    /* Gesamtes Frame zeichnen */				    
			    
			    //Horizontale Achse hinzuf�gen
			    ig2.setPaint(Color.gray);
		        ig2.drawLine(0, height / 2, width, height / 2);
		        
		        ig2.setPaint(Color.black);
		        
		        int prevX = 0, prevY = height / 2;
		        
		        double allMin = Double.POSITIVE_INFINITY, allMax = Double.NEGATIVE_INFINITY;
				
		        //Samples hinzuf�gen
				for (int s = 0; s < allSamples.length; s++) {					
				    if (allSamples[s] < allMin) {
				    	allMin = allSamples[s];
				    }
				    if (allSamples[s] > allMax) {
				    	allMax = allSamples[s];
				    }
				}
				
				double allAbsMax = Math.max(Math.abs(allMin), Math.abs(allMax));
				
				for (int s = 0; s < allSamples.length; s++) {					
				    int endX = s * pixelsPerSample, 
				    	endY;
				    
				    double sample = allSamples[s];				    
				    
				    endY = (int)((height / 2) - ((height / 2) * (sample / allAbsMax)));
				    
				    ig2.drawLine(prevX, prevY, endX, endY);
				    
				    prevX = endX;
				    prevY = endY;
				}
				
				/* Standardabweichung zeichnen */
				ig2.setPaint(Color.red);	
		        
		        prevX = pixelsPerSample * (allSamples.length - stDevByFrame.length) / 2;
		        prevY = height + height / 2;
		        
		        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		        
		        for (int s = 0; s < stDevByFrame.length; s++) {					
				    if (stDevByFrame[s] < min) {
				    	min = stDevByFrame[s];
				    }
				    if (stDevByFrame[s] > max) {
				    	max = stDevByFrame[s];
				    }
				}
		        
		        double absMax = Math.max(Math.abs(min), Math.abs(max));
		        
		        for (int s = 0; s < stDevByFrame.length; s++) {					
				    int endX = prevX + pixelsPerSample, 
				    	endY;
				    
				    double sample = stDevByFrame[s];						    
				    
				    endY = (int)(height + (height / 2) - ((height / 2) * (sample / absMax)));
				    
				    ig2.drawLine(prevX, prevY, endX, endY);
				    
				    prevX = endX;
				    prevY = endY;
				}
		        
		        
		        //Envelope zeichnen
				ig2.setPaint(Color.cyan);
				
				prevX = 0;
				prevY = height / 2;
				
				double[] env = frames.get(i).getEnvelope();
				
				for (int s = 0; s < env.length; s++) {					
				    int endX = s * pixelsPerSample, 
				    	endY;
				    
				    double sample = env[s];				    
				    
				    endY = (int)((height / 2) - ((height / 2) * (sample / allAbsMax)));
				    
				    ig2.drawLine(prevX, prevY, endX, endY);
				    
				    prevX = endX;
				    prevY = endY;
				}
		        
		        /* Lokale Peaks einzeichnen */
		        prevX = 0;
		        prevY = 0;
		        
		        ig2.setPaint(Color.green);
		        
		        int[] localMaxima = frames.get(i).getPeriodStartingPoints();
		        
		        for (int s = 0; s < localMaxima.length; s++) {					
				    int endX = prevX + pixelsPerSample, 
				    	endY = 0;
				    
				    double sample = localMaxima[s];						    
				    
				    if (sample == 1) {
				    	endY = (int)(2* height);
				    
				    	ig2.drawLine(prevX, prevY, endX, endY);
				    }
				    
				    prevX = endX;
				    prevY = endY;
				}
								
			    ImageIO.write(bi, "PNG", new File("D:\\Uni\\Diplomarbeit\\Software\\output\\stdevs\\fr-" + frames.get(i).getTimePosition() + ".png"));
			}
		}
		catch(Exception ex) {
			logger.info("Bild nicht gespeichert");
		    ex.printStackTrace();
		}
	}
	
	public void exportPeaksPositionHistogramm() {
		try {
			int[] histogramm = analyzer.getHistogramm();
			
			int size = 400;
			
			BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
			
		    Graphics2D ig2 = bi.createGraphics();
	
	        ig2.setPaint(Color.white);
	        ig2.setColor(Color.white);
		    
		    ig2.fillRect(0, 0, size - 1, size - 1);
		    
		    ig2.setPaint(Color.black);
		    ig2.setColor(Color.black);
		    
		    int rectWidth = size / 20;
		    
		    double maxHistogrammValue = Double.NEGATIVE_INFINITY;
		    
		    for (int h = 0; h < histogramm.length; h += 5) {
		    	int sum = 0;
		    	for (int hq = 0; hq < 5; hq++) {
		    		sum += histogramm[h + hq];
		    	}
		    	maxHistogrammValue = Math.max(maxHistogrammValue, sum);
		    }
		    
		    for (int h = 0; h < histogramm.length; h += 5) {		    	
		    	int sum = 0;
		    	for (int hq = 0; hq < 5; hq++) {
		    		sum += histogramm[h + hq];
		    	}
		    	
		    	int rectHeight = (int)((sum / maxHistogrammValue) * size);
		    	ig2.fillRect((h / 5) * rectWidth, size - rectHeight, rectWidth - 1, rectHeight);
		    }
		    
		    ImageIO.write(bi, "PNG", new File("D:\\Uni\\Diplomarbeit\\Software\\output\\histo\\", "histogramm.png"));
		}
		catch(Exception ex) {
			logger.info("Bild nicht gespeichert");
		    ex.printStackTrace();
		}
	}
	

	public void exportSpectrogrammFullFrames(String filename) {
		try {
			ArrayList<double[]> spectrogramm = analyzer.getSpectrogrammFullFrames();
			
			int pixelsProFrequency = 2, // H�he
			    pixelsProFrame = 20; // Breite
			
			/* Maximalen Wert finden */
			double maxValue = Double.NEGATIVE_INFINITY, minValue = Double.POSITIVE_INFINITY;
			
			for (int i = 0; i < spectrogramm.size(); i++) {
				double[] spectrum = spectrogramm.get(i);
				for (int j = 0; j < spectrum.length; j++) {
					maxValue = Math.max(maxValue, spectrum[j]);
					minValue = Math.min(minValue, spectrum[j]);
				}
			}
			
			maxValue = Math.log10(maxValue);
			minValue = Math.log10(minValue);			
			
			int width = pixelsProFrame * spectrogramm.size(),
				height = pixelsProFrequency * spectrogramm.get(0).length;
			
			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			
		    Graphics2D ig2 = bi.createGraphics();
	
	        ig2.setPaint(Color.white);
	        ig2.setColor(Color.white);
		    
		    ig2.fillRect(0, 0, width, height);
		    
		    /* F�r die Deep-Valley-Linie */		    
		    int prevX = -1, prevY = -1;
		    
		    int sumMinJ = 0;
		    
		    for (int i = 0; i < spectrogramm.size(); i++) {
				double[] spectrum = spectrogramm.get(i);

				double min = Double.POSITIVE_INFINITY;
				int minJ = -1;
				
				for (int j = 0; j < spectrum.length; j++) {
					int grayscale = (int)((Math.log10(spectrum[j]) - minValue) * (255.0 / (maxValue - minValue)));
					
					if (Math.log10(spectrum[j]) - minValue < min) {
						min = Math.log10(spectrum[j]) - minValue;
						minJ = j;
					}
					
					Color gray = new Color(grayscale, grayscale, grayscale);
					ig2.setPaint(gray);
			        ig2.setColor(gray);
			        
			        ig2.fillRect(i * pixelsProFrame, height - (j + 1) * pixelsProFrequency, pixelsProFrame, pixelsProFrequency);
				}
				
				sumMinJ += minJ;
				
				int endX = i * pixelsProFrame;
				int endY = height - (minJ + 1) * pixelsProFrequency;
				ig2.setPaint(Color.red);	
				ig2.drawLine(prevX, prevY, endX, endY);
				
				prevX = endX;
				prevY = endY;
			}
		    
		    sumMinJ /= spectrogramm.size();
		    
		    ig2.setPaint(Color.green);	
			ig2.drawLine(0, height - (sumMinJ + 1) * pixelsProFrequency, width, height - (sumMinJ + 1) * pixelsProFrequency);
			
			int meanDeppValleyFq = (int)(sumMinJ * Clip.getClassSamplingRate() / (spectrogramm.get(0).length * 2));
		    
		    ImageIO.write(bi, "PNG", new File("D:\\Uni\\Diplomarbeit\\Software\\output\\spectrogramms-full\\", filename + " (meanDeepValleyFq=" + meanDeppValleyFq + " Hz).png"));
		}
		catch(Exception ex) {
			System.out.println("Bild nicht gespeichert");
		    ex.printStackTrace();
		}
	}
	
	public void exportSpectrogrammClosedOpenDifference(String filename) {
		exportSpectrogrammClosedOpenDifference(filename, false);
	}
	
	public void exportSpectrogrammClosedOpenDifference(String filename, boolean onlyRelevantFrequencies) {
		try {
			ArrayList<double[]> spectrogramm = analyzer.getSpectrogrammClosedOpenDifference();
			
			int pixelsProFrequency = 2, // H�he
			    pixelsProFrame = 20; // Breite
			
			/* Maximalen Wert finden */
			double maxValue = Double.NEGATIVE_INFINITY, minValue = Double.POSITIVE_INFINITY;
			
			for (int i = 0; i < spectrogramm.size(); i++) {
				double[] spectrum = spectrogramm.get(i);
				for (int j = 0; j < spectrum.length; j++) {
					maxValue = Math.max(maxValue, spectrum[j]);
					minValue = Math.min(minValue, spectrum[j]);
				}
			}
			
			//Verschieben.
			double oldMinValue = minValue;
			
			maxValue -= 2 * minValue;
			minValue -= 2 * minValue;
			
			maxValue = Math.log10(maxValue);
			minValue = Math.log10(minValue);	
			
			int width = pixelsProFrame * spectrogramm.size(),
				height = pixelsProFrequency * spectrogramm.get(0).length;
			
			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			
		    Graphics2D ig2 = bi.createGraphics();
	
	        ig2.setPaint(Color.white);
	        ig2.setColor(Color.white);
		    
		    ig2.fillRect(0, 0, width, height);
		    
		    /* F�r die Max-Linie */		    
		    int prevX = -1, prevY = -1;
		    
		    int sumMaxJ = 0;
		    
		    for (int i = 0; i < spectrogramm.size(); i++) {
				double[] spectrum = spectrogramm.get(i);
				
				double max = Double.NEGATIVE_INFINITY;
				int maxJ = -1;
				
				for (int j = 0; j < spectrum.length; j++) {

					if (onlyRelevantFrequencies) {
						double fq = (j * Clip.getClassSamplingRate() / (spectrogramm.get(0).length * 2));
						if (fq < godoy.MINIMAL_RELEVANT_FREQUENCY || fq > godoy.MAXIMAL_RELEVANT_FREQUENCY) {
							continue;
						}
					}
					
					
					int grayscale = (int)((Math.log10(spectrum[j] - 2 * oldMinValue) - minValue) * (255.0 / (maxValue - minValue)));	
					if (Math.log10(spectrum[j] - 2 * oldMinValue) > max) {
						max = Math.log10(spectrum[j] - 2 * oldMinValue);
						maxJ = j;
					}
					
					Color gray = new Color(grayscale, grayscale, grayscale);					
			        ig2.setColor(gray);
			        
			        ig2.fillRect(i * pixelsProFrame, height - (j + 1) * pixelsProFrequency, pixelsProFrame, pixelsProFrequency);
				}
				
				sumMaxJ += maxJ;
				
				int endX = i * pixelsProFrame;
				int endY = height - (maxJ + 1) * pixelsProFrequency;
				ig2.setPaint(Color.red);	
				ig2.drawLine(prevX, prevY, endX, endY);
				
				prevX = endX;
				prevY = endY;
				
			}
		    
		    sumMaxJ /= spectrogramm.size();
		    
		    ig2.setPaint(Color.green);	
			ig2.drawLine(0, height - (sumMaxJ + 1) * pixelsProFrequency, width, height - (sumMaxJ + 1) * pixelsProFrequency);
			
			int meanPeakFq = (int)(sumMaxJ * Clip.getClassSamplingRate() / (spectrogramm.get(0).length * 2));
		    
		    ImageIO.write(bi, "PNG", new File("D:\\Uni\\Diplomarbeit\\Software\\output\\spectrogramms-diff\\", filename + " (meanPeakFq=" + meanPeakFq + " Hz).png"));
		}
		catch(Exception ex) {
			System.out.println("Bild nicht gespeichert");
		    ex.printStackTrace();
		}
	}
}
