package batch.printutil;

import properties.ProjectPaths;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/** Simply writes one line as specified in a file in append.
 * It is intended to be invoked from a .sh script.
 * It is useful when I want to write a result file and I want to add some more metadata inside the file
 * at different points of execution
 * */
public class PrintOneLineBatch {

    public static void main(String[] args) {

        ProjectPaths.init();

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(ProjectPaths.cacheTimesFile, true))) {
            for(String s : args) {
                writer.write(s + " ");
            }
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
