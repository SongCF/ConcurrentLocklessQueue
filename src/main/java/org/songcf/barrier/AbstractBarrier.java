package org.songcf.barrier;

import org.songcf.utils.AssertUtils;
import org.songcf.waitstrategy.WaitStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractBarrier implements DataBarrier {

    protected final int               bufferSize;
    protected final int               mask;
    protected final WaitStrategy      waitStrategy;
    protected       Cursor            head;
    protected       Cursor            tail;
    protected       Cursor            headGatingCache;
    //protected       DataBarrier[]     dependentCursors = new DataBarrier[0];
    protected       List<DataBarrier> parents          = new ArrayList<>();
    protected       List<DataBarrier> children         = new ArrayList<>();

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
        //this.dependentCursors = dependentCursors;
        List<DataBarrier> l = new ArrayList<>(Arrays.asList(dependentCursors));
        this.setParentCursors(l);
    }

    @Override
    public DataBarrier[] getDependentCursors() {
        //TODO delete
        return null;
    }

    @Override
    public List<DataBarrier> getParentCursors() {
        return parents;
    }

    @Override
    public List<DataBarrier> getChildCursors() {
        return children;
    }

    @Override
    public void setParentCursors(List<DataBarrier> parents) {
        List<DataBarrier> oldParents = this.parents;
        for (DataBarrier tmp: oldParents) {
            tmp.getChildCursors().remove(this);
        }
        for (DataBarrier tmp: parents) {
            tmp.getChildCursors().add(this);
        }
        this.parents = parents;
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