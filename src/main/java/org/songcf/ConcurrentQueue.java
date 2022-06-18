package org.songcf;

import org.songcf.barrier.DataBarrier;
import org.songcf.barrier.RingBufferDataBarrierImpl;
import org.songcf.handler.EventHandler;
import org.songcf.utils.AssertUtils;
import org.songcf.waitstrategy.WaitStrategy;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author songcf
 * @version : ConcurrentQueue.java, v 1.0.0
 */
public class ConcurrentQueue<E> implements DataPool<E> {

    @Override
    public void publishEvent(EventTranslator<E> translator) throws InterruptedException {
        translateAndPublish(translator, true);
    }

    @Override
    public void consumeEvent(EventHandler eventHandler) throws InterruptedException {
        DataBarrier barrier = eventHandler.getConsumeBarrier();
        translateAndPublish((cachedEventModel, index) -> {
            eventHandler.onEvent(cachedEventModel);
        }, false);
    }

    @Override
    public DataBarrier getPublishBarrier() {
        throw new IllegalStateException("UNSUPPORTED getPublishBarrier.");
    }

    /**
     * 创建ringbuffer
     *
     * @param factory
     * @param bufferSize
     * @param <E>
     * @return
     */
    public static <E> ConcurrentQueue<E> create(EventFactory<E> factory, int bufferSize) {
        return new ConcurrentQueue<>(factory, bufferSize);
    }

    //=================================
    //=================================
    //=================================

    private final EventFactory<E> eventFactory;

    ReentrantLock lock = new ReentrantLock();
    Condition fullCond = lock.newCondition();
    Condition emptyCond = lock.newCondition();
    int putIdx, getIdx;

    private final long indexMask;
    private final E[] entries;
    protected final int bufferSize;

    //TODO 测试pad == 0 或其它大小的效果
    private static int BUFFER_PAD = 32;

    ConcurrentQueue(EventFactory<E> factory, int size) {
        this.eventFactory = factory;

        bufferSize = size;
        AssertUtils.isGreaterEqualThan(bufferSize, 1, "bufferSize must not be less than 1");
        AssertUtils.isPower2(bufferSize, "bufferSize must be a power of 2");
        indexMask = bufferSize - 1;
        // arr padding size -- head and tail
        entries = (E[]) new Object[bufferSize + 2 * BUFFER_PAD];
        putIdx = BUFFER_PAD - 1;
        getIdx = BUFFER_PAD;
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
    private void translateAndPublish(EventTranslator<E> translator, boolean put) throws InterruptedException {
        lock.lock();
        try {
            long index;
            if (put) {
                while (putIdx >= getIdx + bufferSize) {
                    fullCond.await();
                }
                putIdx++;
                index = putIdx;
                translator.translateTo(elementAt(index), index);
                emptyCond.signal();
            } else {
                while (getIdx > putIdx) {
                    emptyCond.await();
                }
                getIdx++;
                index = getIdx;
                translator.translateTo(elementAt(index), index);
                fullCond.signal();
            }
        } catch (Exception e) {
            throw new RuntimeException("translateAndPublish failed");
        } finally {
            lock.unlock();
        }
    }

    private void translateAndPublishBatch() {
        throw new IllegalStateException("UNSUPPORTED translateAndPublishBatch.");
    }

}