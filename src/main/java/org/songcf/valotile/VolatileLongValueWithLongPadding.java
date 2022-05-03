package org.songcf.valotile;

public class VolatileLongValueWithLongPadding extends AbstractVolatileLong {

    //TODO 检查一下padding是否会被编译优化

    protected long p1, p2, p3, p4, p5, p6, p7;

    protected volatile long value;

    protected long p9, p10, p11, p12, p13, p14, p15;

    /**
     * Perform a volatile read of this cursor's value.
     *
     * @return The current value of the cursor.
     */
    @Override
    public final long get() {
        return value;
    }

}