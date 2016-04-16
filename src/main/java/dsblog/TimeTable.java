package dsblog;

public class TimeTable {

    private int[][] tt;

    private TimeTable(){
        tt = new int[Config.NUMBER_OF_SERVERS][Config.NUMBER_OF_SERVERS];
    }

    public void incrementTT(int i, int j){
        tt[i][j] += 1;
    }

    public boolean hasrec(Event e, int k){
        return tt[k][e.getNode()] >= e.getTimestamp();
    }

}
