package org.songcf;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.songcf.barrier.DataBarrier;
import org.songcf.barrier.RingBufferDataBarrierImpl;
import org.songcf.handler.AbstractEventHandler;
import org.songcf.handler.EventHandler;
import org.songcf.waitstrategy.BlockingWaitStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * test case
 * 1w: 60-70ms
 * 10w: 170 - 230ms
 * 100w: 560 - 2100ms [pool x4  <1000ms]
 * @version : ConcurrentLocklessQueueTest.java
 */
public class PerformanceTest003Disruptor {

    static              Disruptor<EventModel>     dataPool;

    static final    int            poolSize       = Const.POOL_SIZE;
    static final    int            CNT            = Const.COUNT;
    static final    int            producers      = Const.PRODUCERS;
    static final    int            consumers      = Const.CONSUMERS;
    static          CountDownLatch countDownLatch = new CountDownLatch(producers + consumers);
    static volatile AtomicInteger  cNum           = new AtomicInteger(0);
    static volatile AtomicInteger  pNum           = new AtomicInteger(0);
    static          long           bt             = 0;

    public static void main(String[] args) {
        bt = System.currentTimeMillis();
        //create queue
        dataPool = new Disruptor<>(EventModel::new, poolSize, DaemonThreadFactory.INSTANCE);
        //consumer
        List<Consumer> consumerList = new ArrayList<>();
        for (int i=1; i<=consumers; i++) {
            consumerList.add(new Consumer("Consumer-0"+i));
        }
        dataPool.handleEventsWithWorkerPool(consumerList.toArray(new Consumer[0]));
        dataPool.start();
        //create producers
        for (int i = 1; i <= producers; i++) {
            final String name = "Producer-0" + i;
            new Thread(new Producer(name)).start();
        }

        //waiting
        try {
            countDownLatch.await();
            System.out.println("p&c event end ");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * event object
     */
    public static class EventModel {
        public int    id;
        public String msg;
    }

    public static class Producer implements Runnable {
        String name;

        public Producer(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            long bt = System.currentTimeMillis();
            int num = 0;
            for (int idx = pNum.getAndIncrement(); idx < CNT; idx=pNum.getAndIncrement()) {
                final int finalIdx = idx;
                try {
                    dataPool.publishEvent((cachedEventModel, index) -> {
                        cachedEventModel.id = finalIdx;
                        cachedEventModel.msg = name;
                    });
                } catch (Exception e) {
                    System.err.println("publishEvent error");
                    e.printStackTrace();
                }
                num++;
            }
            long dt = System.currentTimeMillis() - bt;
            System.out.println("P-END:(" + dt + ")[" + num + "] " + name);
            countDownLatch.countDown();
        }
    }

    public static class Consumer implements com.lmax.disruptor.WorkHandler<EventModel>  {
        String name;

        public Consumer(String name) {
            this.name = name;
        }

        @Override
        public void onEvent(EventModel eventModel) throws Exception {
            //System.out.println("======onEvent[" + name + "]: " + eventModel.id + ", msg=" + eventModel.msg);
            if (cNum.incrementAndGet() >= CNT) {
                long et = System.currentTimeMillis();
                System.out.println("===================cost: " + (et - bt));
                System.exit(0);
            }
        }
    }
}