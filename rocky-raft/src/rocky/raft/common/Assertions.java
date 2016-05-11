package rocky.raft.common;

public class Assertions {

    public static void checkIfTrue(boolean expression, String message) throws Exception {
        if (!expression) {
            throw new Exception(message);
        }
    }

    public static void checkIfEquals(Object o1, Object o2, String message) throws Exception {
        if (!o1.equals(o2)) {
            throw new Exception(message);
        }
    }
}
