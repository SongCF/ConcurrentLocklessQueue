package org.songcf;

import io.netty.util.internal.shaded.org.jctools.queues.MpscUnboundedArrayQueue;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * test case
 * 1w: 60-70ms
 * 10w: 170 - 230ms
 * 100w: 560 - 2100ms [pool x4  <1000ms]
 *
 * @version : ConcurrentLocklessQueueTest.java
 */
public class PerformanceTest004MpscQueue {

    static MpscUnboundedArrayQueue<EventModel> dataPool;

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
        dataPool = new MpscUnboundedArrayQueue<>(poolSize);
        //consumer
        for (int i = 1; i <= consumers; i++) {
            new Thread(new Consumer("Consumer-0" + i)).start();
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
                try {
                    EventModel em = new EventModel();
                    em.id = idx;
                    em.msg = name;
                    dataPool.add(em);
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
                    EventModel eventModel = dataPool.poll();
                    if (eventModel == null) {
                        Thread.yield();
                        //LockSupport.parkNanos(10);
                        continue;
                    }
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