package dsblog;

public class TimeTable {

    private int[][] tt;

    TimeTable(){
        tt = new int[Config.NUMBER_OF_SERVERS][Config.NUMBER_OF_SERVERS];
    }

    public void incrementTT(int i, int j){
        tt[i][j] += 1;
    }

    public int[][] getTt(){
        return tt;
    }

    public boolean hasrec(Event e, int k){
        return tt[k][e.getNode()] >= e.getTimestamp();
    }

    public static TimeTable fromString(String str){
        // TODO
        return null;
    }

    public String toString(){
        // TODO
        return null;
    }

    public void updateSelf(TimeTable other, int selfID, int otherID){

        int[][] otherTt = other.getTt();

        // Take pair-wise max of all elements.
        for(int i = 0; i < Config.NUMBER_OF_SERVERS; i++)
            for(int j = 0; j < Config.NUMBER_OF_SERVERS; j++)
                tt[i][j] = tt[i][j] >= otherTt[i][j] ? tt[i][j] : otherTt[i][j];

        // Update self's row to max of self's row and other's row
        for(int i = 0; i < Config.NUMBER_OF_SERVERS; i++)
            tt[selfID][i] = tt[selfID][i] >= otherTt[otherID][i] ? tt[selfID][i] : otherTt[otherID][i];

    }

}
