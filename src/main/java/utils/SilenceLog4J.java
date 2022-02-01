package utils;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

public class SilenceLog4J {

    public static void silence() {
        // disable the warnings from log4j
        List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for ( org.apache.log4j.Logger logger : loggers ) {
            logger.setLevel(Level.OFF);
        }
    }
}
