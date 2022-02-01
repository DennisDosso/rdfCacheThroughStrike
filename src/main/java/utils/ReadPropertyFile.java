package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ReadPropertyFile {

    public static Map<String, String> doIt(String propertyFilePath) {
        InputStream input = null;
        Map<String, String> map = new HashMap<>();
        try {
            input = new FileInputStream(propertyFilePath);

            // load the properties file
            Properties prop = new Properties();
            prop.load(input);

            //take all the keys in the file, and use them to populate the mao
            Set<Object> keys = prop.keySet();
            for(Object k : keys) {
                String key = (String) k;
                String value = prop.getProperty(key);

                map.put(key, value);
            }
        }
        catch (IOException ex) {
            System.err.println("error reading property file " + propertyFilePath);
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    System.err.println("error closing file " + propertyFilePath);
                    e.printStackTrace();
                }
            }
        }

        return map;
    }
}
