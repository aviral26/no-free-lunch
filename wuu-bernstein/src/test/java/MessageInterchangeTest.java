import common.Constants;
import dsblog.Config;
import dsblog.Event;
import dsblog.Message;
import dsblog.TimeTable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class MessageInterchangeTest {
    @Before
    public void setUp() throws Exception {
        Config.NUMBER_OF_SERVERS = 4;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testToString() {
        TimeTable table = new TimeTable(new int[][] {{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}});

        Event e = new Event();
        e.setTimestamp(5);
        e.setValue("This is a dummy event.");
        e.setNode(2);

        Event e2 = new Event();
        e2.setTimestamp(6);
        e2.setValue("This is also a dummy event.");
        e2.setNode(2);

        String events_str = e.toString() + Constants.LIST_DELIMITER + e2.toString() + Constants.LIST_DELIMITER;
        Message msg = new Message(Message.Type.SYNC, table.toString() + Constants.OBJECT_DELIMITER + events_str,
                Message.Sender.SERVER, 2);

        String[] rec_str = msg.getMessage().split(Constants.OBJECT_DELIMITER);

        assert rec_str.length == 2;

        assert Arrays.deepEquals(table.getTt(), TimeTable.fromString(rec_str[0]).getTt());

        Event e_dash = Event.fromString(rec_str[1].split(Constants.LIST_DELIMITER)[0]);

        assert e.getTimestamp() == e_dash.getTimestamp();
        assert e.getNode() == e_dash.getNode();
        assert e.getValue().equals(e_dash.getValue());

        Event e2_dash = Event.fromString(rec_str[1].split(Constants.LIST_DELIMITER)[1]);

        assert e2.getTimestamp() == e2_dash.getTimestamp();
        assert e2.getNode() == e2_dash.getNode();
        assert e2.getValue().equals(e2_dash.getValue());


    }
}