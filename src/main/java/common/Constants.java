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
     * Message status constants.
     */
    public static final String STATUS_SYNC = "__0__";
    public static final String STATUS_OK = "__1__";
    public static final String STATUS_FAIL = "__2__";
}
