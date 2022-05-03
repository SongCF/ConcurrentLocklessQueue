package org.songcf;

import org.songcf.barrier.DataBarrier;
import org.songcf.barrier.RingBufferDataBarrierImpl;
import org.songcf.handler.EventHandler;
import org.songcf.handler.EventProcessor;
import org.songcf.utils.AssertUtils;
import org.songcf.utils.LogUtil;
import org.songcf.waitstrategy.BlockingWaitStrategy;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * concurrentLocklessQueue的封装，使用更简便
 *
 * @author songcf
 * @version : EventCenter.java, v 1.0.0
 */
public class EventCenter<E> {

    final AtomicBoolean started = new AtomicBoolean(false);

    int                               poolSize;
    BlockingWaitStrategy              blockingWaitStrategy;
    DataPool<E>                       dataPool;
    Map<EventHandler, EventProcessor> handlerMap;
    Map<EventHandler, Boolean>        leafMap;

    public void initEventHandlerWith(EventFactory<E> eventFactory, int poolSize, final EventHandler... handlers) {
        this.poolSize = poolSize;
        this.blockingWaitStrategy = new BlockingWaitStrategy();
        this.dataPool = ConcurrentLocklessQueue.create(eventFactory, poolSize, blockingWaitStrategy);
        handlerMap = new HashMap<>(handlers.length);
        leafMap = new HashMap<>(handlers.length);

        for (int i = 0, eventHandlersLength = handlers.length; i < eventHandlersLength; i++) {
            EventHandler eventHandler = handlers[i];
            //初始化
            DataBarrier barrier = new RingBufferDataBarrierImpl(poolSize, blockingWaitStrategy);
            barrier.setDependentCursors(Arrays.asList(this.dataPool.getPublishBarrier()).toArray(new DataBarrier[0]));
            eventHandler.setConsumeBarrier(barrier);
            EventProcessor<E> processor = new EventProcessor<E>(eventHandler.getConcurrentNum(), dataPool, eventHandler);
            //标记
            handlerMap.put(eventHandler, processor);
            leafMap.put(eventHandler, Boolean.TRUE);
        }
    }

    public void appendEventHandlerAfter(final EventHandler toAdd, final EventHandler... handlers) {
        RingBufferDataBarrierImpl barrier = new RingBufferDataBarrierImpl(poolSize, blockingWaitStrategy);
        DataBarrier[] dependentCursors = new DataBarrier[handlers.length];
        leafMap.put(toAdd, Boolean.TRUE);
        for (int i = 0, eventHandlersLength = handlers.length; i < eventHandlersLength; i++) {
            dependentCursors[i] = handlers[i].getConsumeBarrier();
            leafMap.put(handlers[i], Boolean.FALSE);
        }
        barrier.setDependentCursors(dependentCursors);
        toAdd.setConsumeBarrier(barrier);
        EventProcessor<E> processor = new EventProcessor<>(toAdd.getConcurrentNum(), dataPool, toAdd);
        handlerMap.put(toAdd, processor);
    }

    public void start() {
        checkOnlyStartedOnce();
        // check has processor
        AssertUtils.isTrue(this.handlerMap != null && !this.handlerMap.isEmpty(), "eventCenter handler is empty");
        //check leaf handler
        List<DataBarrier> list = new ArrayList<>();
        leafMap.forEach((k, v) -> {
            if (v) {
                list.add(k.getConsumeBarrier());
            }
        });
        if (list.isEmpty()) {
            throw new IllegalStateException("invalid handler order, has not leaf handler in ringBuffer");
        } else {
            this.dataPool.getPublishBarrier().setDependentCursors(list.toArray(new DataBarrier[0]));
        }
        //check dependent is valid
        this.handlerMap.forEach((k, v) -> {
            AssertUtils.isTrue(k.getConsumeBarrier() != null
                    && k.getConsumeBarrier().getDependentCursors() != null
                    && k.getConsumeBarrier().getDependentCursors().length > 0, "invalid handler dependent");
        });
        //start processor
        this.handlerMap.forEach((k, v) -> v.startThread());
    }

    public void shutdown() {
        int size = 0;
        if (this.handlerMap != null && this.handlerMap.size() > 0) {
            this.handlerMap.forEach((k, v) -> v.stopThread());
            size = this.handlerMap.size();
        }
        this.handlerMap = null;
        this.leafMap = null;
        this.dataPool = null;
        LogUtil.info("event center shutdown, processor size = " + size);
    }

    public void publishEvent(DataPool.EventTranslator<E> translator) throws InterruptedException {
        this.dataPool.publishEvent(translator);
    }

    private void checkOnlyStartedOnce() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("EventCenter.start() must only be called once.");
        }
    }
}