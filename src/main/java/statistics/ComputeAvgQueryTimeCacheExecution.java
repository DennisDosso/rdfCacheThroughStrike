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

/**
 *
 * Property to set: ProjectPaths.cacheResultFile
 * */
public class ComputeAvgQueryTimeCacheExecution {

    public static void main(String[] args) {
        ProjectPaths.init();
        ProjectValues.init();

        List<Long> times = new ArrayList<>(); // times required on the system (total)
        List<Long> cacheMissTimes = new ArrayList<>(); // times required on the cache (when there is a miss)
        List<Long> dbMissTimes = new ArrayList<>(); // times required on the DB (sfter the miss)
        List<Double> averages = new ArrayList<>();

        int misses = 0, hits = 0, timeouts = 0;

        // open the file with the results
        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.cacheResultFile))) {
            String oldLine = r.readLine();
            String line = "";

            while((line = r.readLine()) != null) {
                if(oldLine.startsWith("#")) { // beginning of a new query
                    times.clear(); cacheMissTimes.clear(); dbMissTimes.clear();

                    // read the data
                    String[] parts = line.split(",");
                    if(parts[0].equals("miss")) { // this is a miss
                        long time = Long.parseLong(parts[1]);
                        long cacheTime = Long.parseLong(parts[2]);
                        long dbTime = Long.parseLong(parts[3]);
                        times.add(time); cacheMissTimes.add(cacheTime); dbMissTimes.add(dbTime);
                        misses++;
                    } else if(parts[0].equals("hit")) { // this is a hit
                        long time = Long.parseLong(parts[1]);
                        times.add(time);
                        hits++;
                    } else if(parts[0].equals("timeout")) { // this is a timeout
                        long time = Long.parseLong(parts[1]);
                        times.add(time);
                        timeouts++;
                    }

                    oldLine = line;
                } else { // in the middle of the current query
                    if(line.startsWith("#")) { // actually at the end of the current query
                        // remove the outliers
                        long max = Collections.max(times);
                        long min = Collections.min(times);
                        times.remove(Long.valueOf(min));
                        times.remove(Long.valueOf(max));

                        double avg = NumberUtils.averageLong(times);
                        averages.add(avg);
                    } else { //effectively in the middle of the query
                        String[] parts = line.split(",");
                        if(parts[0].equals("miss")) { // this is a miss
                            long time = Long.parseLong(parts[1]);
                            long cacheTime = Long.parseLong(parts[2]);
                            long dbTime = Long.parseLong(parts[3]);
                            times.add(time); cacheMissTimes.add(cacheTime); dbMissTimes.add(dbTime);
                        } else if(parts[0].equals("hit")) { // this is a hit
                            long time = Long.parseLong(parts[1]);
                            times.add(time);
                        } else if(parts[0].equals("timeout")) { // this is a timeout
                            long time = Long.parseLong(parts[1]);
                            times.add(time);
                        }
                    }
                    oldLine = line;
                }
            }
            // if we are here, we need to close the last query execution
            String[] parts = oldLine.split(",");
            if(parts[0].equals("miss")) { // this is a miss
                long time = Long.parseLong(parts[1]);
                long cacheTime = Long.parseLong(parts[2]);
                long dbTime = Long.parseLong(parts[3]);
                times.add(time); cacheMissTimes.add(cacheTime); dbMissTimes.add(dbTime);
            } else if(parts[0].equals("hit")) { // this is a hit
                long time = Long.parseLong(parts[1]);
                times.add(time);
            } else if(parts[0].equals("timeout")) { // this is a timeout
                long time = Long.parseLong(parts[1]);
                times.add(time);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // we need to remove the first elements from the list of averages (the cache was 'cold')
        for(int i = 0; i < ProjectValues.epochLength; ++i) {
            // remove the first epoch
            averages.remove(0);
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
        System.out.println("hits: " + hits);
        System.out.println("misses: " + misses);
        System.out.println("timeouts: " + timeouts);
    }
}
