package statistics;

import properties.ProjectPaths;
import properties.ProjectValues;
import utils.NumberUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        // open the file with the results
        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.wholeDBresultFile))) {

            String oldLine = r.readLine();
            String line = "";

            while((line = r.readLine()) != null) {
                if(oldLine.startsWith("#")) {
                    // beginning of a new query
                    times.clear();
                    long t = Long.parseLong(line);
                    times.add(t);
                    oldLine = line;
                } else { // current query
                    if(line.startsWith("#")) { // we need to close the current query
                        long max = Collections.max(times);
                        long min = Collections.min(times);
                        times.remove(Long.valueOf(min));
                        times.remove(Long.valueOf(max));

                        double avg = NumberUtils.averageLong(times);
                        averages.add(avg);
                        oldLine = line;
                    } else {
                        long time = Long.parseLong(line);
                        times.add(time);
                        oldLine = line;
                    }
                }

            } // if we are here, we need to close the last query
            long time = Long.parseLong(oldLine);
            times.add(time);
            long max = Collections.max(times);
            long min = Collections.min(times);
            times.remove(Long.valueOf(min));
            times.remove(Long.valueOf(max));
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
