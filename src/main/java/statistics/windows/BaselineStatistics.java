package statistics.windows;

import properties.ProjectPaths;
import properties.ProjectValues;
import utils.NumberUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Computes some statistics for the baseline (avg. update time, maximum size of the DB)*/
public class BaselineStatistics {

    public static void computeStatistics(int windowNumber) {
        ProjectPaths.init();
        ProjectValues.init();

        String line = "";
        int windowWidth = ProjectValues.queriesToCheck / ProjectValues.epochLength;
        int startingQueryIndex = windowNumber * windowWidth;
        long maxSize = 0;

        List<Long> times = new ArrayList<>();

        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.baselineTimeFile))) {
            for(int j = 0; j <= startingQueryIndex; ++j) {
                r.readLine(); // go to the first query we actually want
            }

            int counter = 0;
            while((line = r.readLine()) != null) {
                String[] parts = line.split(",");
                // the first part is the time in ms required to update the database
                long time = Long.parseLong(parts[0]);
                times.add(time);
                long size = Long.parseLong(parts[1]);
                if(size > maxSize)
                    maxSize = size;
                counter++;
                if(counter > windowWidth)
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // compute the average with z-value
        double timeAvg = NumberUtils.averageLong(times);
        double variance = NumberUtils.varianceForLong(times, timeAvg);
        double standardDeviation = NumberUtils.standardDeviation(variance);
        double zInterval =NumberUtils.zInterval(standardDeviation, times.size(), 1.96);
        timeAvg = Math.round(timeAvg * 100.0) / 100.0;
        zInterval = Math.round(zInterval * 100.0) / 100.0;
        // print the average update time
        System.out.println("Averga update time $" + timeAvg + "\\pm" + zInterval + "$");
        System.out.println("Max size in tuples: " + maxSize);

    }

    public static void main(String[] args) {
        BaselineStatistics.computeStatistics(0);
        BaselineStatistics.computeStatistics(1);
        BaselineStatistics.computeStatistics(2);
        BaselineStatistics.computeStatistics(3);
        BaselineStatistics.computeStatistics(4);
        BaselineStatistics.computeStatistics(5);
        BaselineStatistics.computeStatistics(6);
        BaselineStatistics.computeStatistics(7);
        BaselineStatistics.computeStatistics(8);
        BaselineStatistics.computeStatistics(9);
    }
}
