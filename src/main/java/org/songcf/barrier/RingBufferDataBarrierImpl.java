package org.songcf.barrier;

import javafx.util.Pair;
import org.songcf.utils.AssertUtils;
import org.songcf.waitstrategy.WaitStrategy;

import java.util.List;
import java.util.concurrent.locks.LockSupport;

public class RingBufferDataBarrierImpl extends AbstractBarrier {

    public final ConditionVal conditionVal = new ConditionVal();

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

    private void await(DataBarrier barrier) {
        boolean aw = ((RingBufferDataBarrierImpl)barrier).conditionVal.await();
        //boolean aw = false;
        //for (DataBarrier tmp : this.getParentCursors()) {
        //    aw |= ((RingBufferDataBarrierImpl)tmp).conditionVal.await();
        //}
        LockSupport.park(this);
    }

    private void signal() {
        conditionVal.signal();
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
        int spin = 0;
        do {
            currentVal = head.get();
            nextVal = currentVal + maxNum;
            gatingVal = headGatingCache.get();
            if (nextVal > gatingVal) {
                Pair<Long, DataBarrier> p = minDependentValue();
                nextGatingVal = mask + p.getKey();
                if (nextVal > nextGatingVal) {
                    //TODO wait strategy
//                    await(p.getValue());
                    Thread.yield();
                    //LockSupport.parkNanos(1L);
//                    Thread.sleep(0);
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
            nextGatingVal = mask + minDependentValue().getKey();
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
        int spin = 0;
        while (!tail.compareAndSet(old, high)) {
            if (tail.get() > old) {
                throw new IllegalStateException(String.format("publish error, old:%s, low:%s, high:%s", old, low, high));
            }
//            Thread.sleep(0);
//            Thread.yield();
            //LockSupport.parkNanos(1L);
//            await(this);
                Thread.yield();
        }
//        signal();
    }

    /**
     * 获取依赖项中的最小值【即最大可用槽位】
     *
     * @return
     */
    private Pair<Long, DataBarrier> minDependentValue() {
        //long nextVal = Long.MAX_VALUE;
        //for (int i = 0, count = dependentCursors.length; i < count; i++) {
        //    nextVal = Math.min(nextVal, dependentCursors[i].validIndex());
        //}
        //return nextVal;
        long nextVal = Long.MAX_VALUE;
        long tmp = 0;
        DataBarrier minBarrier = null;
        List<DataBarrier> l = getChildCursors();
        for (DataBarrier dataBarrier : l) {
            tmp = dataBarrier.validIndex();
            if (tmp < nextVal) {
                nextVal = tmp;
                minBarrier = dataBarrier;
            }
        }
        return new Pair<>(nextVal, minBarrier);
    }
}