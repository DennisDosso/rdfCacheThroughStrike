package setup;

import properties.ProjectPaths;
import properties.ProjectValues;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** It seems that rdf4j requires a lot of time (too much) to import a database of 100M
 * triples. I tried to create this class to take and split a file in parts, so to
 * be easier to import it into a triplestore.
 * <p>
 * properties to set:
 * in path.properties
 * <li> rdfFilePath
 * <li>  ttlFilesDirectory
 * */
public class SplitDatasetFile {
	
	public SplitDatasetFile() {
		ProjectPaths.init();
		ProjectValues.init();
	}
	
	public void splitDatasetFile() {
		Path fileIn = Paths.get(ProjectPaths.rdfFilePath);
		try(BufferedReader reader = Files.newBufferedReader(fileIn)) {
			String line = "";
			String outputDirectory = ProjectPaths.ttlFilesDirectory + "/";
			int fileCounter = 0;
			
			int triplesCounter = 0;
			
			Path output;
			BufferedWriter writer = null;
			
			List<String> prefixes = new ArrayList<>();
			int x = 500000;

			while((line = reader.readLine()) != null) {
				line = filterTheLine(line);

				// take care of the prefixes
				if(line.contains("@prefix")) {
					prefixes.add(line);
					continue;
				}

				if(triplesCounter % x == 0) {// IN CASE WE NEED TO CHANGE FILE
					// we need first to check for some possible problems when dealing with lines (e.g., Wikidata has some bad boyz)

					if(line.endsWith(";") || line.endsWith(",")) { // need to proceed until the next '.'
						writer.write(line);
						writer.newLine();
						continue;
					} 
					else if(line.endsWith(".")) {
						//write the last line
						writer.write(line);
						line = null;
					}
					
					// if it exists, flush and close the previous writer
					if(writer != null) {
						writer.flush();
						writer.close();
					}
					
					System.out.println("starting fragment number " + fileCounter);
					// open a new Buffered Writer
					output = Paths.get(outputDirectory + fileCounter + ".ttl");
					fileCounter++;
					writer = Files.newBufferedWriter(output);
					//print the prefixes
					for(String prefix : prefixes) {
						writer.write(prefix);
						writer.newLine();
					}
				}
				
				if(line != null) {
					writer.write(line);
					writer.newLine();
				}
				triplesCounter++;
			}

			writer.flush();
			writer.close();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String filterTheLine(String line) {
		String toReturn = "";

		// first, delete the geo locations
		toReturn = line.replaceAll("\\^\\^geo:wktLiteral", "");
		return toReturn;
	}
	
	public static void main(String[] args) {
		SplitDatasetFile execution = new SplitDatasetFile();
		execution.splitDatasetFile();
		
		System.out.println("done");
	}
}
