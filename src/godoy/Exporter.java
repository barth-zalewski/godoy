package godoy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Diese Klasse dient dem Export von Daten
 */


public class Exporter {
	private final List<Frame> frames;
	
	public Exporter(List<Frame> data) {
		frames = data;
	}
	
	public void exportAsTXT() {
		BufferedWriter writer = null;
        try {        
            File logFile = new File("D:\\Uni\\Diplomarbeit\\Software", "export.txt");
            writer = new BufferedWriter(new FileWriter(logFile));
            writer.write("Export\r\n");
            writer.write("===========\r\n");
            
            for (int i = 0; i < frames.size(); i++) {
            	writer.write("Frame #" + i + "\r\n");
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
}
