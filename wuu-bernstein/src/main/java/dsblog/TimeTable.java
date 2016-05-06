package dsblog;

import common.Constants;
import utils.LogUtils;

public class TimeTable {

    private int[][] tt;

    TimeTable(){
        tt = new int[Config.NUMBER_OF_SERVERS][Config.NUMBER_OF_SERVERS];
    }

    public TimeTable(int[][] table){
        tt = new int[Config.NUMBER_OF_SERVERS][Config.NUMBER_OF_SERVERS];
        for(int i = 0; i < Config.NUMBER_OF_SERVERS; i++)
            for(int j = 0; j < Config.NUMBER_OF_SERVERS; j++)
                tt[i][j] = table[i][j];
    }

    public int incrementAndReadMyTimestamp(int myId){
        tt[myId][myId] += 1;
        return tt[myId][myId];
    }

    public int[][] getTt(){
        return tt;
    }

    boolean hasrec(Event e, int k){
        return tt[k][e.getNode()] >= e.getTimestamp();
    }

    public static TimeTable fromString(String str){

        int[][] table = new int[Config.NUMBER_OF_SERVERS][Config.NUMBER_OF_SERVERS];
        String[] rows = str.split(Constants.ROW_DELIMITER);
        int i = 0;
        for(String row : rows){
            String[] elements = row.split(Constants.ELEMENT_DELIMITER);
            for(int j = 0; j < Config.NUMBER_OF_SERVERS; j++)
                table[i][j] = Integer.parseInt(elements[j]);
            i++;
        }
        return new TimeTable(table);
    }

    @Override
    public String toString(){
        StringBuilder table = new StringBuilder("");
        StringBuilder row;

        for(int i = 0; i < Config.NUMBER_OF_SERVERS; i++){
            row = new StringBuilder("");
            for(int j = 0; j < Config.NUMBER_OF_SERVERS; j++)
                row.append(tt[i][j]).append(Constants.ELEMENT_DELIMITER);
            table.append(row).append(Constants.ROW_DELIMITER);
        }
        return table.toString();
    }

    void updateSelf(TimeTable other, int selfID, int otherID){

        int[][] otherTt = other.getTt();

        // Take pair-wise max of all elements.
        for(int i = 0; i < Config.NUMBER_OF_SERVERS; i++)
            for(int j = 0; j < Config.NUMBER_OF_SERVERS; j++)
                tt[i][j] = tt[i][j] >= otherTt[i][j] ? tt[i][j] : otherTt[i][j];

        // Update self's row to max of self's row and other's row.
        for(int i = 0; i < Config.NUMBER_OF_SERVERS; i++)
            tt[selfID][i] = tt[selfID][i] >= otherTt[otherID][i] ? tt[selfID][i] : otherTt[otherID][i];

    }

}
