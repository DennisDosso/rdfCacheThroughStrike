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

/** Use this when you simply want to compute the average of a series of values.
 * Use this with the file containing the values used to compute the lineages.
 * */
public class ComputeSimpleAverage {

    public ComputeSimpleAverage() {
        ProjectPaths.init();
        ProjectValues.init();
    }

    public void computeAverageAndZval() {
        List<Double> times = new ArrayList<>(); // times required on the system (total)

        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.averageResultFile))) {
            String line = "";
            while((line = r.readLine()) != null) {
                double val = Double.parseDouble(line);
                times.add(val);
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
        ComputeSimpleAverage execution = new ComputeSimpleAverage();
        execution.computeAverageAndZval();
    }
}
