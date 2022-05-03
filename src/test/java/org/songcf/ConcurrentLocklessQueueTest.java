package org.songcf;

import org.songcf.barrier.DataBarrier;
import org.songcf.barrier.RingBufferDataBarrierImpl;
import org.songcf.handler.AbstractEventHandler;
import org.songcf.handler.EventHandler;
import org.songcf.waitstrategy.BlockingWaitStrategy;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * test case
 *
 * @author dx
 * @version : ConcurrentLocklessQueueTest.java
 */
public class ConcurrentLocklessQueueTest {

    public static final EventFactory<EventModel> EVENT_FACTORY        = () -> new EventModel();
    static              BlockingWaitStrategy     blockingWaitStrategy = new BlockingWaitStrategy();
    static              DataPool<EventModel>     dataPool;

    static final int            poolSize       = 1024;
    static final int            CNT            = 10;
    static final int            producers      = 2;
    static final int            consumers      = 4;
    static       CountDownLatch countDownLatch = new CountDownLatch(producers);

    public static void main(String[] args) {
        //create queue
        dataPool = ConcurrentLocklessQueue.create(EVENT_FACTORY, poolSize, blockingWaitStrategy);
        //create Consumers
        DataBarrier barrier = new RingBufferDataBarrierImpl(poolSize, blockingWaitStrategy);
        barrier.setDependentCursors(Arrays.asList(dataPool.getPublishBarrier()).toArray(new DataBarrier[0]));
        dataPool.getPublishBarrier().setDependentCursors(Arrays.asList(barrier).toArray(new DataBarrier[0]));
        for (int i = 1; i <= consumers; i++) {
            final String name = "Consumer-0" + i;
            new Thread(new Consumer(name, barrier)).start();
        }
        //create producers
        for (int i = 1; i <= producers; i++) {
            final String name = "Producer-0" + i;
            new Thread(new Producer(name)).start();
        }

        //waiting
        try {
            countDownLatch.await();
            Thread.sleep(5000);
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
            for (int idx = 0; idx < CNT; idx++) {
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
            }
            System.out.println("END: " + name);
            countDownLatch.countDown();
        }
    }

    public static class Consumer extends AbstractEventHandler implements Runnable {
        String name;

        public Consumer(String name, DataBarrier barrier) {
            this.name = name;
            this.setConsumeBarrier(barrier);
        }

        @Override
        public void onEvent(Object event) {
            EventModel eventModel = (EventModel) event;
            System.out.println("======onEvent[" + name + "]: " + eventModel.id + ", msg=" + eventModel.msg);
        }

        @Override
        public void run() {
            //initial handler
            // producer barrier dependent all leaf consumer barriers
            // consumer barrier dependent all producer barriers
            EventHandler handler = this;
            while (true) {
                try {
                    dataPool.consumeEvent(handler);
                } catch (Exception e) {
                    System.err.println("publishEvent error");
                    e.printStackTrace();
                }
            }
        }
    }
}