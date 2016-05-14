package rocky.raft.common;

import java.util.concurrent.Semaphore;

public class InterruptibleSemaphore extends Semaphore {

    public InterruptibleSemaphore(int permits) {
        super(permits);
    }

    public void interrupt() {
        for (Thread thread : getQueuedThreads()) {
            thread.interrupt();
        }
    }
}
