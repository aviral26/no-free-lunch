package dsblog;

import com.squareup.tape.QueueFile;
import utils.LogUtils;

import java.io.IOException;

public class Log {

    private String LOG_TAG = "Log";
    private QueueFile logWriter;

    private Log(int serverID){
        logWriter = new Log(Config.LOG_FILE);
        LOG_TAG += "-" + serverID;
    }

    public void append(Event e) throws IOException {
        logWriter.add(e.toString().getBytes());
        LogUtils.debug(LOG_TAG, "Appended event to log.");
    }

}
