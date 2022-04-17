package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ConvertToHash {

    /** Converts one string to its SHA-256 hash version
     * */
    public static String convertToHashSHA256(String toBeConverted) {
        String converted = "";

        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
            mDigest.update(toBeConverted.getBytes());
            converted = new String(mDigest.digest());
            converted = converted.replaceAll("\\u0000", ""); // remove the null characters from our strings
            // I did this because, apparently, PostgreSQL created some exceptions when we
            // tried to insert string with this character

        } catch (NoSuchAlgorithmException e) {
            System.err.println("No such algorithm as SHA-256 here");
            e.printStackTrace();
        }
        return converted;
    }
}
