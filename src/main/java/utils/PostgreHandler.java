package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Singleton that deals with the connection to a PostgreSQL database
 * */
public class PostgreHandler {

    private static Connection connection = null;
    private static String owner = null;

    /** If a connection to the RDB does not exist, it creates and returns it. Otherwise, it returns it.
     *
     * The class that creates this connection sets itself as owner of the connection by passing its
     * name (or another string that you must remember though) to this method. Only the owner
     * can later close the connection
     * */
    public static Connection getConnection(String jdbcConnectionString, String owner_) throws SQLException {
        if(connection == null || connection.isClosed()) {
            if(owner == null || owner.equals(owner_)) { // if there is no owner or we already are the owners
                connection = DriverManager.getConnection(jdbcConnectionString); // create the connection
                owner = owner_; // set ourselves if owner (useful when no owner was present)
            }
        }

        return connection; // return the connection in any case
    }

    /** Call this method when you think you need a connection and do not think that other
     * processes will need to use it
     * */
    public static Connection getConnection(String jdbcConnectionString) throws SQLException {
        return PostgreHandler.getConnection(jdbcConnectionString, "default");
    }

    public static void closeConnection(String owner_) throws SQLException {
        if(connection == null)
            return;
        if(connection.isClosed())
            return;

        if(owner != null && owner.equals(owner_)) {
            connection.close();
        }
    }

    /** Closes the connection in any case, without any concern for who is the owner.
     * Use with precaution.*/
    public static void closeConnection()  throws SQLException {
        if(connection == null)
            return;
        if(connection.isClosed())
            return;
        connection.close();
    }
}
