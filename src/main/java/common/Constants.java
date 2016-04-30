package common;

public class Constants {
    /**
     * Delimiter for heterogeneous objects and database and log entries.
     */
    public static final String OBJECT_DELIMITER = " !object! ";

    /**
     * Delimiter for homogeneous objects in a list.
     */
    public static final String LIST_DELIMITER = " !list! ";

    /**
     * Delimiter for primitive elements.
     */
    public static final String ELEMENT_DELIMITER = " !element! ";

    /**
     * Delimiter for rows of primitive elements.
     */
    public static final String ROW_DELIMITER = " !row! ";

    /**
     * Server status reply constants.
     */
    public static final String STATUS_SYNC = "SYNC";
    public static final String STATUS_OK = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";

    public static final String DB_FILE = "/Users/aviral/Hogwarts/CS-271//no-free-lunch/SERVER-DB_FILE";
    public static final String LOG_FILE = "/Users/aviral/Hogwarts/CS-271//no-free-lunch/SERVER-LOG_FILE";

    /**
     * Unit testing switch. Set to false when deploying.
     */
    public static final boolean UNIT_TESTING = true;
}
