package dsblog;

import utils.LogUtils;

import java.io.*;

public class Database {

    private String LOG_TAG = "Database";
    private BufferedWriter dbWriter;
    private BufferedReader dbReader;
    private int counter;
    private String DB_FILE;

    Database(int serverID){
        LOG_TAG += "-" + serverID;
        DB_FILE = "~/.SERVER-" + serverID + "-DB_FILE";
        counter = 0;
        try{
            dbWriter = new BufferedWriter(new FileWriter(DB_FILE));
        }
        catch(IOException e){
            LogUtils.error(LOG_TAG, "Failed to initialize DB file.", e);
            System.exit(-1);
        }
    }

    public void insert(String blogPost) throws IOException {
        dbWriter.append(++counter + blogPost + Config.POST_DELIMITER);
        LogUtils.debug(LOG_TAG, "Written post to file. Counter = " + counter);
    }

    public String lookUp() throws IOException {
        dbReader = new BufferedReader(new FileReader(DB_FILE));
        StringBuilder result = new StringBuilder("");
        String temp;
        while((temp = dbReader.readLine()) != null)
            result.append(temp);

        return result.toString();
    }
}
