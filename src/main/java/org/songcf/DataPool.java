package org.songcf;

import org.songcf.barrier.DataBarrier;
import org.songcf.handler.EventHandler;

public interface DataPool<E> {

    interface EventTranslator<E> {
        void translateTo(E cachedEventModel, long index);
    }

    /**
     * 发布 【复用旧对象堆内存】
     *
     * @param translator
     */
    void publishEvent(EventTranslator<E> translator) throws InterruptedException;

    /**
     * 获取
     *
     * @return
     */
    void consumeEvent(EventHandler eventHandler) throws InterruptedException;

    /**
     * 获取事件发布屏障
     *
     * @return
     */
    default DataBarrier getPublishBarrier() { return null;}

}