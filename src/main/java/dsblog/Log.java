package dsblog;

import com.squareup.tape.QueueFile;
import utils.LogUtils;

import java.io.File;
import java.io.IOException;

public class Log {

    private String LOG_TAG = "Log";
    private QueueFile logWriter;
    private String LOG_FILE;

    public Log(int serverID){
        LOG_TAG += "-" + serverID;
        LOG_FILE = "~/.SERVER-" + serverID + "-LOG_FILE";
        try{
            logWriter = new QueueFile(new File(LOG_FILE));
        }
        catch(IOException e){
            LogUtils.error(LOG_TAG, "Failed to initialize Log file.", e);
            System.exit(-1);
        }
    }

    public void append(Event e) throws IOException {
        logWriter.add(e.toString().getBytes());
        LogUtils.debug(LOG_TAG, "Appended event to log.");
    }

}
