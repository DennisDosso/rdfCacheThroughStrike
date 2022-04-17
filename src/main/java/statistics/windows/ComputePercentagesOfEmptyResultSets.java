package statistics.windows;

import properties.ProjectPaths;
import properties.ProjectValues;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ComputePercentagesOfEmptyResultSets {

    static int windowSize = 1000;

    public static void main(String[] args) {

        ProjectPaths.init();
        ProjectValues.init();
        int counter = 0;
        int windowCounter = 0;
        String line = "";
        int emptySetQueries = 0;
        int emptuSetQueriesOnWindow = 0;
        boolean toPrint = true;


        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.wholeDBresultFile))) {
            while((line = r.readLine()) != null) {
                if(line.contains("#")) {
                    counter++;
                    windowCounter++;
                    if(line.contains("false")) {
                        emptySetQueries++;
                        emptuSetQueriesOnWindow++;
                    }
                    toPrint = true;
                }

                if(windowCounter % windowSize == 0 && toPrint) {
                    System.out.println("empty set ratio on window: " + (double)emptuSetQueriesOnWindow/windowSize);
                    System.out.println("empty set ratio overall: " + (double) emptySetQueries/counter);
                    windowCounter = 0;
                    emptuSetQueriesOnWindow = 0;
                    toPrint = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
