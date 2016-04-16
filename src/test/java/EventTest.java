import dsblog.Config;
import dsblog.Event;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class EventTest {

    @Before
    public void setUp() throws Exception {
        Config.NUMBER_OF_SERVERS = 4;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testToString(){

        Event e = new Event();
        e.setTimestamp(5);
        e.setValue("This is a dummy event.");
        e.setNode(2);

        Event newE = Event.fromString(e.toString());

        assert e.getTimestamp() == newE.getTimestamp();
        assert e.getNode() == newE.getNode();
        assert e.getValue().equals(newE.getValue());
    }
}
