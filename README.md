# @Deprecated
同disruptor 基本不需要再用了。
JDK经过几版迭代优化，目前ArrayBlockingQueue的性能在多Producer多Consumer的情况下性能也很高。


# ConcurrentLocklessQueue

高性能并发无锁队列，同时支持多生产者、多消费者。使用二阶段提交、cas、循环队列、缓存行填充等优化手段。
 
---


## 使用方式

多中使用方式，详情参考测试用例。

#### 1.直接使用队列
```
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
```

---

#### 2.使用封装的EventCenter
```
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
```

---

#### 3.基于DAG的消费模式
```
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
```



