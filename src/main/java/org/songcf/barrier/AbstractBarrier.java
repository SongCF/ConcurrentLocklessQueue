package org.songcf.barrier;

import org.songcf.utils.AssertUtils;
import org.songcf.waitstrategy.WaitStrategy;

public abstract class AbstractBarrier implements DataBarrier {

    protected final int          bufferSize;
    protected final int          mask;
    protected final WaitStrategy waitStrategy;
    protected       Cursor       head;
    protected       Cursor        tail;
    protected       Cursor        headGatingCache;
    protected       DataBarrier[] dependentCursors = new DataBarrier[0];

    public AbstractBarrier(int mask, int bufferSize, WaitStrategy waitStrategy) {
        AssertUtils.isGreaterEqualThan(bufferSize, 1, "bufferSize must not be less than 1");
        AssertUtils.isPower2(bufferSize, "bufferSize must be a power of 2");
        this.mask = mask;
        this.bufferSize = bufferSize;
        this.waitStrategy = waitStrategy;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public long validIndex() {
        return tail.get();
    }

    @Override
    public void setDependentCursors(DataBarrier[] dependentCursors) {
        this.dependentCursors = dependentCursors;
    }

    @Override
    public DataBarrier[] getDependentCursors() {
        return this.dependentCursors;
    }

    public void setHead(Cursor head) {
        this.head = head;
    }

    public void setTail(Cursor tail) {
        this.tail = tail;
    }

    public void setHeadGatingCache(Cursor headGatingCache) {
        this.headGatingCache = headGatingCache;
    }
}