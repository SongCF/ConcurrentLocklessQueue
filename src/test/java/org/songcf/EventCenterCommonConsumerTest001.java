package org.songcf;

import org.songcf.handler.AbstractEventHandler;
import org.songcf.handler.EventHandler;

import java.util.concurrent.CountDownLatch;

/**
 * test case
 *
 * @version : EventCenterTest002.java
 */
public class EventCenterCommonConsumerTest001 {

    public static final EventFactory<EventModel> EVENT_FACTORY = () -> new EventModel();
    static              EventCenter<EventModel>  eventCenter;

    static final int            poolSize       = 1024;
    static final int            CNT            = 10000;
    static final int            producers      = 2;
    static final int            consumers      = 4;
    static       CountDownLatch countDownLatch = new CountDownLatch(producers);

    public static void main(String[] args) {
        //create eventCenter and register concurrent consumer/handler
        //consumer并发消费，每个消息只会被消费一次
        eventCenter = new EventCenter<>();
        EventHandler handler = new MyHandler();
        handler.setConcurrentNum(consumers);
        eventCenter.initEventHandlerWith(EVENT_FACTORY, poolSize, handler);
        eventCenter.start();
        //create producers
        for (int i = 1; i <= producers; i++) {
            final String name = "Producer-0" + i;
            new Thread(new Producer(name)).start();
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

        public Producer(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            for (int idx = 0; idx < CNT; idx++) {
                final int finalIdx = idx;
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
        @Override
        public void onEvent(Object event) {
            EventModel eventModel = (EventModel) event;
            System.out.println("======onEvent[" + Thread.currentThread() + "]: " + eventModel.id + ", msg=" + eventModel.msg);
        }
    }
}