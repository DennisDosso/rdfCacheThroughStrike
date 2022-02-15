package statistics;

import properties.ProjectPaths;
import properties.ProjectValues;
import utils.NumberUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Computes different values taken from the file containing
 * the information about the update of the RDB
 *
 * */
public class ComputeUpdateRdbAvgTimes {

    public ComputeUpdateRdbAvgTimes() {
        ProjectPaths.init();
        ProjectValues.init();
    }

    public void computeUpdateRdbAverageTimes() {
        List<Double> times = new ArrayList<>(); // times required on the system (total)

        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.averageResultFile))) {
            String line = "";
            while((line = r.readLine()) != null) {
                Double time = Double.parseDouble(line.split(",")[1]);
                times.add(time);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double avgTime = NumberUtils.averageDouble(times);
        double variance = NumberUtils.variance(times, avgTime);
        double standardDeviation = NumberUtils.standardDeviation(variance);
        double zInterval =NumberUtils.zInterval(standardDeviation, times.size(), 1.96);
        // we round a little bit because I want to copy-paste this into latex
        avgTime = Math.round(avgTime * 100.0) / 100.0;
        zInterval = Math.round(zInterval * 100.0) / 100.0;
        System.out.println(avgTime + "\\pm" + zInterval);
    }

    public static void main(String[] args) {
        ComputeUpdateRdbAvgTimes execution = new ComputeUpdateRdbAvgTimes();
        execution.computeUpdateRdbAverageTimes();
    }
}
