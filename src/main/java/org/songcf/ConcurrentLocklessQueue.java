package org.songcf;

import org.songcf.barrier.DataBarrier;
import org.songcf.barrier.RingBufferDataBarrierImpl;
import org.songcf.handler.EventHandler;
import org.songcf.utils.AssertUtils;
import org.songcf.waitstrategy.WaitStrategy;

/**
 * 并发多消费者多生产者无锁队列。使用二阶段提交、消费屏障、缓存行等提升性能。
 *
 * @author songcf
 * @version : ConcurrentLocklessQueue.java, v 1.0.0
 */
public class ConcurrentLocklessQueue<E> implements DataPool<E> {
    @Override
    public void publishEvent(EventTranslator<E> translator) throws InterruptedException {
        translateAndPublish(translator, producerBarrier);
    }

    @Override
    public void consumeEvent(EventHandler eventHandler) throws InterruptedException {
        DataBarrier barrier = eventHandler.getConsumeBarrier();
        translateAndPublish((cachedEventModel, index) -> {
            eventHandler.onEvent(cachedEventModel);
        }, barrier);
    }

    @Override
    public DataBarrier getPublishBarrier() {
        return this.producerBarrier;
    }

    /**
     * 创建ringbuffer
     *
     * @param factory
     * @param bufferSize
     * @param waitStrategy
     * @param <E>
     * @return
     */
    public static <E> ConcurrentLocklessQueue<E> create(EventFactory<E> factory, int bufferSize, WaitStrategy waitStrategy) {
        DataBarrier barrier = new RingBufferDataBarrierImpl(true, bufferSize, waitStrategy);
        return new ConcurrentLocklessQueue<>(factory, barrier);
    }

    //=================================
    //=================================
    //=================================

    private final DataBarrier     producerBarrier;
    private final EventFactory<E> eventFactory;

    private final   long indexMask;
    private final   E[]  entries;
    protected final int  bufferSize;

    //TODO 测试pad == 0 或其它大小的效果
    private static int BUFFER_PAD = 32;

    ConcurrentLocklessQueue(EventFactory<E> factory, DataBarrier barrier) {
        this.eventFactory = factory;
        this.producerBarrier = barrier;

        bufferSize = barrier.getBufferSize();
        AssertUtils.isGreaterEqualThan(bufferSize, 1, "bufferSize must not be less than 1");
        AssertUtils.isPower2(bufferSize, "bufferSize must be a power of 2");
        indexMask = bufferSize - 1;
        // arr padding size -- head and tail
        entries = (E[]) new Object[bufferSize + 2 * BUFFER_PAD];
        for (int i = 0; i < bufferSize; i++) {
            entries[BUFFER_PAD + i] = eventFactory.newInstance();
        }
    }

    private E elementAt(long index) {
        return entries[BUFFER_PAD + (int) (index & indexMask)];
    }

    /**
     * 二阶段提交
     */
    private void translateAndPublish(EventTranslator<E> translator, DataBarrier barrier) throws InterruptedException {
        long index = barrier.next();
        try {
            translator.translateTo(elementAt(index), index);
        } catch (Exception e) {
            throw new RuntimeException("translateAndPublish failed");
            //LogUtil.error("translateAndPublish failed", LogUtil.getStackTrace(e));
        } finally {
            barrier.publish(index);
        }
    }

    private void translateAndPublishBatch() {
        throw new IllegalStateException("UNSUPPORTED translateAndPublishBatch.");
    }

}