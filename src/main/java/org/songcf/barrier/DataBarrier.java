package org.songcf.barrier;

public interface DataBarrier {

    /**
     * 【一阶段】获取下一有效位置【阻塞】
     *
     * @return
     */
    default long next() throws InterruptedException { return next(1);}

    /**
     * 【一阶段】获取下n个有效位置
     *
     * @return
     */
    long next(int maxNum) throws InterruptedException;

    /**
     * 【一阶段】获取下一有效位置【非阻塞】，获取不到返回 CURSOR_INITIAL_VALUE
     *
     * @return
     */
    default long tryNext() {return tryNext(1);}

    /**
     * 【一阶段】获取下n个有效位置【非阻塞】，获取不到返回 CURSOR_INITIAL_VALUE
     *
     * @return
     */
    long tryNext(int maxNum);

    /**
     * 【二阶段】提交
     *
     * @param index
     */
    default void publish(long index) throws InterruptedException { publish(index, index); }

    /**
     * 【二阶段】批量提交
     *
     * @param low
     * @param high
     */
    void publish(long low, long high) throws InterruptedException;

    /**
     * pool size
     *
     * @return
     */
    int getBufferSize();

    /**
     * 最大有效索引
     *
     * @return
     */
    long validIndex();

    /**
     * 设置依赖的屏障
     *
     * @param dependentCursors
     */
    void setDependentCursors(DataBarrier[] dependentCursors);

    /**
     * 获取依赖的屏障
     */
    DataBarrier[] getDependentCursors();
}