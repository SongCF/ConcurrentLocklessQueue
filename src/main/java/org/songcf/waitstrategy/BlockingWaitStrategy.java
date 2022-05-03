package org.songcf.waitstrategy;

import org.songcf.barrier.DataBarrier;

/**
 * 等待策略
 */
public final class BlockingWaitStrategy implements WaitStrategy {
    private final Object mutex = new Object();

    @Override
    public long waitFor(long index, DataBarrier cursor, DataBarrier[] dependentBarrier) {
        throw new IllegalStateException("TODO");
    }

    @Override
    public void signalAllWhenBlocking() {
        synchronized (mutex) {
            mutex.notifyAll();
        }
    }

    @Override
    public String toString() {
        return "BlockingWaitStrategy{" +
                "mutex=" + mutex +
                '}';
    }
}
