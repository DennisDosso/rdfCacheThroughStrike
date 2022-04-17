package statistics.windows;

import properties.ProjectPaths;
import properties.ProjectValues;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class ComputeRatioOfRepetitionInQueries {

    static int windowSize = 10000;

    public static void main(String[] args) {

        ProjectPaths.init();
        ProjectValues.init();
        int counter = 0;
        String line = "";
        int differentQueries = 0;
        int repeatedQueries = 0;


        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.selectQueryFile))) {
            Set<String> queries = new HashSet<>(); // set that contains the queries we ask
            while((line = r.readLine()) != null) {
                if(queries.contains(line)) {
                    repeatedQueries++;
                } else {
                    queries.add(line);
                }
                differentQueries++;
                counter++;
                if(counter % windowSize == 0) {
                    double ratio = ((double) repeatedQueries) / differentQueries;
                    System.out.println("Percentage of repetition so far: " + ratio);
                }
            }

            // finally
            System.out.println("Percentage at the end: " + ((double) repeatedQueries) / differentQueries);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
