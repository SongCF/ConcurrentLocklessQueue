package org.songcf;

import org.songcf.handler.AbstractEventHandler;
import org.songcf.handler.EventHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * test case
 *
 * @version : EventCenterTest002.java
 */
public class EventCenterDagConsumerTest002 {

    public static final EventFactory<EventModel> EVENT_FACTORY = () -> new EventModel();
    static              EventCenter<EventModel>  eventCenter;

    static final int            poolSize       = 1024;
    static final int            CNT            = 100;
    static final int            producers      = 2;
    static       CountDownLatch countDownLatch = new CountDownLatch(producers);

    public static void main(String[] args) {
        //create eventCenter and register DAG consumer/handler
        //Dag consumer: A -> B, B -> C, C -> D, B -> D；每个消息会被ABCD都执行一遍，执行顺序按照dag依赖顺序
        eventCenter = new EventCenter<>();
        EventHandler hdlA = new MyHandler("A");
        EventHandler hdlB = new MyHandler("B");
        EventHandler hdlC = new MyHandler("C");
        EventHandler hdlD = new MyHandler("D");
        eventCenter.initEventHandlerWith(EVENT_FACTORY, poolSize, hdlA);
        eventCenter.appendEventHandlerAfter(hdlB, hdlA);
        eventCenter.appendEventHandlerAfter(hdlC, hdlB);
        eventCenter.appendEventHandlerAfter(hdlD, hdlC);
        eventCenter.appendEventHandlerAfter(hdlD, hdlB);
        eventCenter.start();
        //create producers
        AtomicInteger atomicInteger = new AtomicInteger(0);
        for (int i = 1; i <= producers; i++) {
            final String name = "Producer-0" + i;
            new Thread(new Producer(name, atomicInteger)).start();
        }

        //waiting
        try {
            countDownLatch.await();
            System.out.println("produce event end, waiting consume 5s");
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
        AtomicInteger atomicInteger;

        public Producer(String name, AtomicInteger atomicInteger) {
            this.name = name;
            this.atomicInteger = atomicInteger;
        }

        @Override
        public void run() {
            for (int idx = 0; idx < CNT; idx++) {
                final int finalIdx = atomicInteger.incrementAndGet();
                try {
                    eventCenter.publishEvent((cachedEventModel, index) -> {
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

    public static class MyHandler extends AbstractEventHandler {
        String name;
        public MyHandler(String name) {
            this.name = name;
        }
        @Override
        public void onEvent(Object event) {
            EventModel eventModel = (EventModel) event;
            System.out.println("======onEvent[" + name + "]: " + eventModel.id + ", msg=" + eventModel.msg);
        }
    }
}