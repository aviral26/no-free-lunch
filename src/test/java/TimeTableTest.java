import dsblog.Config;
import dsblog.TimeTable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class TimeTableTest {
    @Before
    public void setUp() throws Exception {
        Config.NUMBER_OF_SERVERS = 4;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testToString(){
        TimeTable table = new TimeTable(new int[][] {{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}});
        TimeTable newTable = TimeTable.fromString(table.toString());
        assert Arrays.deepEquals(table.getTt(), newTable.getTt());
    }
}
