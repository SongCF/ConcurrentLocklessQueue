package org.songcf.handler;

import org.songcf.barrier.DataBarrier;
import org.songcf.utils.AssertUtils;

public abstract class AbstractEventHandler implements EventHandler {

    DataBarrier consumeBarrier;
    int         concurrentNum = 1;

    @Override
    public abstract void onEvent(Object event);

    @Override
    public DataBarrier getConsumeBarrier() {
        return consumeBarrier;
    }

    @Override
    public void setConsumeBarrier(DataBarrier dataBarrier) {
        this.consumeBarrier = dataBarrier;
    }

    @Override
    public void setConcurrentNum(int num) {
        AssertUtils.isGreaterEqualThan(num, 1, "invalid concurrentNum: " + num);
        concurrentNum = num;
    }

    @Override
    public int getConcurrentNum() {
        return concurrentNum;
    }
}