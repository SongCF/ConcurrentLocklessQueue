package org.songcf.handler;

import org.songcf.barrier.DataBarrier;

/**
 * 事件处理器
 */
public interface EventHandler {

    /**
     * 事件处理回调 【线程loop】
     *
     * @param event
     */
    void onEvent(Object event);

    /**
     * 等待事件超时回调
     *
     * @param index
     */
    default void onTimeout(long index) { }

    /**
     * 获取事件消费屏障
     *
     * @return
     */
    DataBarrier getConsumeBarrier();

    /**
     * 设置事件消费屏障
     *
     * @return
     */
    void setConsumeBarrier(DataBarrier dataBarrier);

    /**
     * 并发数量
     *
     * @param num
     */
    void setConcurrentNum(int num);

    /**
     * 并发数量
     *
     * @return
     */
    int getConcurrentNum();
}