package org.songcf.handler;

import org.songcf.DataPool;
import org.songcf.utils.AbstractLoopRunnable;
import org.songcf.utils.AssertUtils;

import java.util.Map;

public class EventProcessor<T> extends AbstractLoopRunnable {

    //并发数量
    private final int                 concurrentNum;
    private final EventProcessor<T>[] concurrentProcessors;
    private final EventHandler        eventHandler;
    private final DataPool<T>         dataPool;

    public EventProcessor(DataPool<T> dataPool, EventHandler eventHandler) {
        this(1, dataPool, eventHandler);
    }

    public EventProcessor(int concurrentNum, DataPool<T> dataPool, EventHandler eventHandler) {
        this.dataPool = dataPool;
        this.eventHandler = eventHandler;
        AssertUtils.isGreaterEqualThan(concurrentNum, 1, "invalid concurrentNum");
        this.concurrentNum = concurrentNum;
        this.concurrentProcessors = new EventProcessor[concurrentNum];
        if (concurrentNum > 1) {
            //多线程公用barrier
            for (int i = 0; i < concurrentNum; i++) {
                this.concurrentProcessors[i] = new EventProcessor<>(1, this.dataPool, this.eventHandler);
            }
        }
    }

    /**
     * 启动 [支持多线程] 1.单线程则自己处理onLoop消息 2.多线程则concurrentProcessors处理消息
     */
    @Override
    public void startThread() {
        synchronized (this) {
            if (concurrentNum <= 1) {
                super.startThread();
            } else {
                for (int i = 0; i < concurrentNum; i++) {
                    this.concurrentProcessors[i].startThread();
                }
            }
        }
    }

    /**
     * 停止 [支持多线程]
     */
    @Override
    public void stopThread() {
        synchronized (this) {
            if (concurrentNum <= 1) {
                super.stopThread();
            } else {
                for (int i = 0; i < concurrentNum; i++) {
                    this.concurrentProcessors[i].stopThread();
                }
            }
        }
    }

    //===============================
    //====线程处理部分
    //===============================

    @Override
    protected long onLoop(Map<String, Object> context, long lastReturnTimestamp) throws InterruptedException {
        dataPool.consumeEvent(this.eventHandler);
        return 0;
    }

}