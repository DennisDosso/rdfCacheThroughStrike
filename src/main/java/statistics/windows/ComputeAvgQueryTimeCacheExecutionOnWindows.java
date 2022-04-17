package statistics.windows;

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
public class ComputeAvgQueryTimeCacheExecutionOnWindows {

    public static void computeOnWindowsNumber(int windowNumber) {
        ProjectPaths.init();
        ProjectValues.init();

        List<Long> times = new ArrayList<>(); // times required on the system (total)
        List<Long> cacheMissTimes = new ArrayList<>(); // times required on the cache (when there is a miss)
        List<Long> dbMissTimes = new ArrayList<>(); // times required on the DB (after the miss)
        List<Double> averages = new ArrayList<>();

        int misses = 0, hits = 0, timeouts = 0;
        int counter = 0;
        int queriesToSkip = ProjectValues.queriesToCheck * windowNumber;



        // open the file with the results
        try(BufferedReader r = Files.newBufferedReader(Paths.get(ProjectPaths.cacheResultFile))) {

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
                    break;
                }

                if(oldLine.startsWith("#")) { // beginning of a new query
                    times.clear(); cacheMissTimes.clear(); dbMissTimes.clear();
                    counter++;

                    // read the data
                    String[] parts = line.split(",");
                    long time = Long.parseLong(parts[1]);
                    if(time > 10000) { // patch to take care of some outliers. Need to remove after completion
                        oldLine = line;
                        timeouts++;
                        continue;
                    }

                    if(parts[0].equals("miss")) { // this is a miss
                        long cacheTime = Long.parseLong(parts[2]);
                        long dbTime = Long.parseLong(parts[3]);
                        times.add(time); cacheMissTimes.add(cacheTime); dbMissTimes.add(dbTime);
                        misses++;
                    } else if(parts[0].equals("hit")) { // this is a hit
                        times.add(time);
                        hits++;
                    } else if(parts[0].equals("timeout")) { // this is a timeout
                        times.add(time);
                        timeouts++;
                    }

                    oldLine = line;
                } else { // in the middle of the current query
                    if(line.startsWith("#")) { // actually at the end of the current query
                        // remove the outliers
                        long max = Collections.max(times);
                        long min = Collections.min(times);
//                        times.remove(Long.valueOf(min));
//                        times.remove(Long.valueOf(max));

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

        System.out.println("Window number " + windowNumber);
        System.out.println(totalAvg + "\\pm" + zInterval);
        System.out.print(" hits: " + hits);
        System.out.print(" misses: " + misses);
        System.out.println(" timeouts: " + timeouts);
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
    }
}
