package statistics.windows;

import properties.ProjectPaths;
import properties.ProjectValues;
import utils.NumberUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/** Script used to compute the average time required to compute the queries on the whole database.
 *
 * <p>Property to set:
 * <li>
 *     wholeDBresultFile (path)
 * </li></p>
 * */
public class ComputeAverageQueryTimeWholeDBOnWindows {

    /** Computes the average on a window of queries. This window has
     * limit given by ProjectValues.queriesToCheck and offset given by
     * (ProjectValues.queriesToCheck * windowNumber).
     * It prints the result.
     * You can invoke this method many times to obtain the result in your window
     * */
    public static void computeOnWindowsNumber(int windowNumber) {
        ProjectPaths.init();
        ProjectValues.init();



        List<Long> times = new ArrayList<>();
        List<Double> averages = new ArrayList<>();
        int counter = 0;
        int queriesToSkip = ProjectValues.queriesToCheck * windowNumber;
        int emptyRS = 0; // number of empty result sets

        // open the file with the results
        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.wholeDBresultFile))) {

            String oldLine = r.readLine();
            String line = "";

            for(int j = 0; j < queriesToSkip; ++j) { // this for loop reads line by line, skipping a number of queries
                // equal to queriesToSkip. This complex thing is due to the fact that we have many lines
                // reporting the times for each query, and, given potential errors, we cannot always be sure
                // that each query has been always been executed K amount of times

                // read it until you make it
                if(oldLine.startsWith("#")) {
                    // read on
                    line = r.readLine();
                }

                while(!line.startsWith("#")) { // keep going until next query
                    oldLine = line;
                    line = r.readLine();
                }
                oldLine = line;
            }

            while((line = r.readLine()) != null) {

                if(counter >= ProjectValues.queriesToCheck) {
                    oldLine = line;
                    break;
                }


                if(oldLine.startsWith("#")) {
                    if(oldLine.contains("false"))
                        emptyRS++;
                    // beginning of a new query
                    times.clear();
                    try {
                        long t = Long.parseLong(line);
                        times.add(t);

                    } catch (NumberFormatException e) {
                        // really nothing, this is likely a timeout or execution error
                    }
                    oldLine = line;
                    counter++;
                } else { // current query
                    if(line.startsWith("#")) { // we need to close the current query
                        try {
                            long max = Collections.max(times);
                            long min = Collections.min(times);
//                        times.remove(Long.valueOf(max));
//                        times.remove(Long.valueOf(min));
                            double avg = NumberUtils.averageLong(times);
                            averages.add(avg);
                        } catch (NoSuchElementException e) {
                            // do nothing, this was a timeout
                        }
                        oldLine = line;
                    } else {
                        try{
                            long t = Long.parseLong(line);
                            times.add(t);
                        } catch (NumberFormatException e) {
                            // really nothing, this is likely a timeout or execution error
                        }
                        oldLine = line;
                    }
                }

            } // if we are here, we need to close the last query
            long time = Long.parseLong(oldLine);
            times.add(time);
            long max = Collections.max(times);
            long min = Collections.min(times);
//            times.remove(Long.valueOf(min));
//            times.remove(Long.valueOf(max));
            double avg = NumberUtils.averageLong(times);
            averages.add(avg);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // now we take the averages of the averages
        double totalAvg = NumberUtils.averageDouble(averages);
        double variance = NumberUtils.variance(averages, totalAvg);
        double standardDeviation = NumberUtils.standardDeviation(variance);
        double zInterval =NumberUtils.zInterval(standardDeviation, averages.size(), 1.96);

        // we round a little bit because I want to copy-paste this into latex
        totalAvg = Math.round(totalAvg * 100.0) / 100.0;
        zInterval = Math.round(zInterval * 100.0) / 100.0;

        System.out.println("Window number " + windowNumber + " has " + emptyRS + " empty result sets");
        System.out.println(totalAvg + "\\pm" + zInterval);
    }

    public static void main(String[] args) {
        computeOnWindowsNumber(0);
        computeOnWindowsNumber(1);
        computeOnWindowsNumber(2);
        computeOnWindowsNumber(3);
        computeOnWindowsNumber(4);
        computeOnWindowsNumber(5);
        computeOnWindowsNumber(6);
        computeOnWindowsNumber(7);
        computeOnWindowsNumber(8);
        computeOnWindowsNumber(9);
        computeOnWindowsNumber(10);
    }
}
