package dsblog;

import common.Constants;
import utils.CommonUtils;
import utils.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Log {

    private String LOG_TAG = "Log";
    private FileOutputStream logWriter;
    private String LOG_FILE;

    public Log(int serverID) {
        LOG_TAG += "-" + serverID;
        LOG_FILE = Constants.LOG_FILE + "-" + serverID;
        try {
            logWriter = new FileOutputStream(LOG_FILE); // Create a new file each time.
        } catch (IOException e) {
            LogUtils.error(LOG_TAG, "Failed to initialize Log file.", e);
            System.exit(-1);
        }
    }

    public void append(Event e) throws IOException {
        logWriter.write((e.toString() + Constants.OBJECT_DELIMITER).getBytes());
        logWriter.flush();
        LogUtils.debug(LOG_TAG, "Appended new event to log.");
    }

    public List<Event> readLog() throws IOException {
        FileInputStream logReader = new FileInputStream(LOG_FILE);
        byte[] file = new byte[(int) new File(LOG_FILE).length()];
        List<Event> events = new ArrayList<>();
        logReader.read(file);
        CommonUtils.closeQuietly(logReader);

        String[] file_str = (new String(file)).split(Constants.OBJECT_DELIMITER);
        for (String s : file_str)
            events.add(Event.fromString(s));

        LogUtils.debug(LOG_TAG, "Read " + events.size() + " events from log.");
        return events;
    }
}
