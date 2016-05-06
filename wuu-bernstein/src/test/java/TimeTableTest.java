import dsblog.Config;
import dsblog.TimeTable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class TimeTableTest {
    private static final int NUM = 4;

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
        System.out.println("Original table:\n");
        print(table);
        System.out.println();
        TimeTable newTable = TimeTable.fromString(table.toString());
        System.out.println("Retrieved table: ");
        print(newTable);
        assert Arrays.deepEquals(table.getTt(), newTable.getTt());
    }

    private void print(TimeTable table){
        int[][] t = table.getTt();
        for(int i = 0; i < NUM; i++){
            System.out.println();
            for(int j = 0; j < NUM; j++)
                System.out.print(t[i][j] + " ");
        }
    }
}
