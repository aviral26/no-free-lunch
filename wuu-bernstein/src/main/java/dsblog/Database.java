package dsblog;

import common.Constants;
import utils.CommonUtils;
import utils.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Database {

    private String LOG_TAG = "Database";
    private FileOutputStream dbWriter;
    private int counter;
    private String DB_FILE;

    Database(int serverID) {
        LOG_TAG += "-" + serverID;
        DB_FILE = Constants.DB_FILE + "-" + serverID;
        counter = 0;
        try {
            dbWriter = new FileOutputStream(DB_FILE, true); // Create a new file whether it exists or not.
        } catch (IOException e) {
            LogUtils.error(LOG_TAG, "Failed to initialize DB file.", e);
            System.exit(-1);
        }
    }

    public void insert(String blogPost) throws IOException {
        //dbWriter.write(("Post number " + ++counter + ". " + blogPost + Constants.OBJECT_DELIMITER).getBytes());
        dbWriter.write((blogPost + Constants.OBJECT_DELIMITER).getBytes());
        dbWriter.flush();

        LogUtils.debug(LOG_TAG, "Written new post to file. Counter = " + counter);
    }

    public String lookUp() throws IOException {
        FileInputStream dbReader = new FileInputStream(DB_FILE);
        byte[] file = new byte[(int) (new File(DB_FILE).length())];
        dbReader.read(file);
        CommonUtils.closeQuietly(dbReader);
        LogUtils.debug(LOG_TAG, "Look up successful.");
        return new String(file);
    }
}
