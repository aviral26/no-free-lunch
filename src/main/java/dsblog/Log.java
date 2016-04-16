package dsblog;

import common.Constants;
import utils.LogUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Log {

    private String LOG_TAG = "Log";
    private PrintWriter logWriter;
    private Scanner logReader;
    private String LOG_FILE;

    public Log(int serverID){
        LOG_TAG += "-" + serverID;
        LOG_FILE = "~/.SERVER-" + serverID + "-LOG_FILE";
        try{
            logWriter = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE)));
        }
        catch(IOException e){
            LogUtils.error(LOG_TAG, "Failed to initialize Log file.", e);
            System.exit(-1);
        }
    }

    public void append(Event e) throws IOException {
        logWriter.write(e.toString() + Constants.OBJECT_DELIMITER);
        LogUtils.debug(LOG_TAG, "Appended new event to log.");
    }

    public List<Event> readLog() throws IOException{
        logReader = new Scanner(new BufferedReader(new FileReader(LOG_FILE)));
        logReader.useDelimiter(Constants.OBJECT_DELIMITER);
        List<Event> events = new ArrayList<>();

        while(logReader.hasNext())
            events.add(Event.fromString(logReader.next()));

        LogUtils.debug(LOG_TAG, "Read " + events.size() + " events from log.");
        return events;
    }
}
