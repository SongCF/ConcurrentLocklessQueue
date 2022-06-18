package org.songcf;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * test case
 *
 * @version : ConcurrentLocklessQueueTest.java
 */
public class PerformanceTest002ArrayBlockQueue {

    public static final EventFactory<EventModel>       EVENT_FACTORY = () -> new EventModel();
    static              ArrayBlockingQueue<EventModel> dataPool;

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
        dataPool = new ArrayBlockingQueue<EventModel>(poolSize);
        //create Consumers
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
        public int    id;
        public String msg;
    }

    static EventModel cachedEventModel = new EventModel();
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
                try {
                    EventModel em = new EventModel();
                    em.id = idx;
                    em.msg = name;
                    dataPool.put(em);
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

    public static class Consumer implements Runnable {
        String name;

        public Consumer(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            //initial handler
            // producer barrier dependent all leaf consumer barriers
            // consumer barrier dependent all producer barriers
            while (true) {
                try {
                    EventModel eventModel = dataPool.take();
                    //System.out.println("======onEvent[" + name + "]: " + eventModel.id + ", msg=" + eventModel.msg);
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