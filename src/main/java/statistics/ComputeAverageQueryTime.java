package statistics;

import properties.ProjectPaths;
import properties.ProjectValues;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ComputeAverageQueryTime {

    public static void main(String[] args) {
        ProjectPaths.init();
        ProjectValues.init();

        // open the file with the queries
        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.wholeDBresultFile))) {
            String line = "";
            while((line = r.readLine()) != null) {

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
