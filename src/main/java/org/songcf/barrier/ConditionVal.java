/**
 * Alipay.com Inc. Copyright (c) 2004-2022 All Rights Reserved.
 */
package org.songcf.barrier;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.LockSupport;

/**
 *
 * @author dx
 * @version : ConditionVal.java, v 0.1 2022年05月05日 5:51 下午 dx Exp $
 */
public class ConditionVal {

    private static CopyOnWriteArraySet<Thread> waitingSet = new CopyOnWriteArraySet<>();

    public boolean await() {
        return waitingSet.add(Thread.currentThread());
    }

    //public void signal() {
    //    Thread t = waitingSet.poll();
    //    if (t != null) {
    //        LockSupport.unpark(t);
    //    }
    //}

    public void signal() {
        LockSupport.unpark(waitingSet.stream().findAny().get());
//        waitingSet.forEach(LockSupport::unpark);
    }
}