package org.songcf.barrier;

import org.songcf.utils.AssertUtils;
import org.songcf.waitstrategy.WaitStrategy;

public class RingBufferDataBarrierImpl extends AbstractBarrier {

    public RingBufferDataBarrierImpl(int bufferSize, WaitStrategy waitStrategy) {
        this(false, bufferSize, waitStrategy);
    }

    public RingBufferDataBarrierImpl(boolean isProducer, int bufferSize, WaitStrategy waitStrategy) {
        //producer在前，所以依赖consumer时，需要减去bufferSize长度
        super(isProducer ? bufferSize : 0, bufferSize, waitStrategy);
        setHead(CursorFactory.newCursor());
        setTail(CursorFactory.newCursor());
        setHeadGatingCache(CursorFactory.newCursor());
    }

    /**
     * 必须满足 maxNum个后才会返回
     *
     * @param maxNum
     * @return
     */
    @Override
    public long next(int maxNum) throws InterruptedException {
        AssertUtils.isGreaterEqualThan(maxNum, 1, "invalid next value: " + maxNum);
        long currentVal, nextVal, gatingVal, nextGatingVal;
        // 需满足如下公式
        // ProducerBarrier < ConsumerBarrier + mask
        // ConsumerBarrier < ProducerBarrier
        do {
            currentVal = head.get();
            nextVal = currentVal + maxNum;
            gatingVal = headGatingCache.get();
            if (nextVal > gatingVal) {
                nextGatingVal = mask + minDependentValue();
                if (nextVal > nextGatingVal) {
                    //TODO wait strategy
                    Thread.sleep(1);
                    continue;
                }
                headGatingCache.compareAndSet(gatingVal, nextGatingVal);
            } else if (head.compareAndSet(currentVal, nextVal)) {
                break;
            }
        } while (true);
        return nextVal;
    }

    /**
     * 立即返回，获取数量不定
     *
     * @param maxNum
     * @return
     */
    @Override
    public long tryNext(int maxNum) {
        AssertUtils.isGreaterEqualThan(maxNum, 1, "invalid next value: " + maxNum);
        long currentVal, nextVal, nextGatingVal;
        do {
            currentVal = head.get();
            nextVal = currentVal + maxNum;
            nextGatingVal = mask + minDependentValue();
            nextVal = Math.min(nextVal, nextGatingVal);
            if (nextVal == currentVal) {
                return Cursor.CURSOR_INITIAL_VALUE;
            }
        } while (!head.compareAndSet(currentVal, nextVal));
        return nextVal;
    }

    @Override
    public void publish(long low, long high) throws InterruptedException {
        AssertUtils.isGreaterEqualThan(low, 0, "invalid publish low: " + low);
        AssertUtils.isGreaterEqualThan(high, low, "invalid publish high: " + high);
        long old = low - 1;
        while (!tail.compareAndSet(old, high)) {
            if (tail.get() > old) {
                throw new IllegalStateException(String.format("publish error, old:%s, low:%s, high:%s", old, low, high));
            }
            Thread.sleep(1);
        }
    }

    /**
     * 获取依赖项中的最小值【即最大可用槽位】
     *
     * @return
     */
    private long minDependentValue() {
        long nextVal = Long.MAX_VALUE;
        for (int i = 0, count = dependentCursors.length; i < count; i++) {
            nextVal = Math.min(nextVal, dependentCursors[i].validIndex());
        }
        return nextVal;
    }
}