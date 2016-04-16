package dsblog;

import utils.LogUtils;

import java.io.*;

public class Database {

    private String LOG_TAG = "Database";
    private BufferedWriter dbWriter;
    private BufferedReader dbReader;
    private int counter;

    Database(int serverID){
        LOG_TAG += "-" + serverID;
        dbWriter = new BufferedWriter(new FileWriter(Config.DB_FILE));
        counter = 0;
    }

    public void insert(String blogPost) throws IOException {
        dbWriter.write(++counter + blogPost + Config.POST_DELIMITER);
        LogUtils.debug(LOG_TAG, "Written post to file. Counter = " + counter);
    }

    public String lookUp() throws IOException {
        dbReader = new BufferedReader(new FileReader(Config.DB_FILE));
        StringBuilder result = new StringBuilder("");
        String temp;
        while((temp = dbReader.readLine()) != null)
            result.append(temp);

        return result.toString();
    }
}
