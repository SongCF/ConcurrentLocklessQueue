package org.songcf.waitstrategy;

import org.songcf.barrier.DataBarrier;

public interface WaitStrategy {

    long waitFor(long index, DataBarrier cursor, DataBarrier[] dependentBarrier);

    void signalAllWhenBlocking();
}
