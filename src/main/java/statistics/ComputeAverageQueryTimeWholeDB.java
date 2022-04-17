package statistics;

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
public class ComputeAverageQueryTimeWholeDB {

    public static void main(String[] args) {
        ProjectPaths.init();
        ProjectValues.init();

        List<Long> times = new ArrayList<>();
        List<Double> averages = new ArrayList<>();
        int counter = 0;

        // open the file with the results
        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.wholeDBresultFile))) {

            String oldLine = r.readLine();
            String line = "";

            while((line = r.readLine()) != null) {
                if(counter >= ProjectValues.queriesToCheck) {
                    oldLine = line;
                    break;
                }
                counter++;

                if(oldLine.startsWith("#")) {
                    // beginning of a new query
                    times.clear();
                    try{
                        long t = Long.parseLong(line);
                        times.add(t);

                    } catch (NumberFormatException e) {
                        // really nothing, this is likely a timeout or execution error
                    }
                    oldLine = line;
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
        System.out.println(totalAvg + "\\pm" + zInterval);


    }
}
