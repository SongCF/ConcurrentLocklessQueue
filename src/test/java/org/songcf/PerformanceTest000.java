package org.songcf;

import org.songcf.handler.AbstractEventHandler;
import org.songcf.waitstrategy.BlockingWaitStrategy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FIXME
 * @version : ConcurrentLocklessQueueTest.java
 */
public class PerformanceTest000 {

    public static final EventFactory<EventModel> EVENT_FACTORY = () -> new EventModel();
    static BlockingWaitStrategy blockingWaitStrategy = new BlockingWaitStrategy();
    static DataPool<EventModel> dataPool;

    static final int poolSize = Const.POOL_SIZE;
    static final int CNT = Const.COUNT;
    static final int producers = Const.PRODUCERS;
    static final int consumers = Const.CONSUMERS;
    static CountDownLatch countDownLatch = new CountDownLatch(producers + consumers);
    static volatile AtomicInteger cNum = new AtomicInteger(0);
    static volatile AtomicInteger pNum = new AtomicInteger(0);
    static long bt = 0;

    public static void main(String[] args) {
        bt = System.currentTimeMillis();
        //create queue
        dataPool = ConcurrentQueue.create(EVENT_FACTORY, poolSize);

        for (int i = 1; i <= consumers; i++) {
            final String name = "Consumer-0" + i;
            new Thread(new Consumer(name)).start();
        }
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
        public int id;
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
            for (int idx = pNum.getAndIncrement(); idx < CNT; idx = pNum.getAndIncrement()) {
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

    public static class Consumer extends AbstractEventHandler implements Runnable {
        String name;

        public Consumer(String name) {
            this.name = name;
        }

        @Override
        public void onEvent(Object event) {
            EventModel eventModel = (EventModel) event;
            //System.out.println("======onEvent[" + name + "]: " + eventModel.id + ", msg=" + eventModel.msg);
        }

        @Override
        public void run() {
            //initial handler
            // producer barrier dependent all leaf consumer barriers
            // consumer barrier dependent all producer barriers
            while (true) {
                try {
                    dataPool.consumeEvent(this);
                    if (cNum.incrementAndGet() >= CNT) {
                        long et = System.currentTimeMillis();
                        System.out.println("===================cost: " + (et - bt));
                        System.exit(0);
                    }
                } catch (Exception e) {
                    System.err.println("publishEvent error");
                    e.printStackTrace();
                }
            }
        }
    }
}