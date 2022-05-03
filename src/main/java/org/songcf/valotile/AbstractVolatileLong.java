package org.songcf.valotile;

import org.songcf.utils.UnsafeUtil;
import sun.misc.Unsafe;

public abstract class AbstractVolatileLong {

    protected static final Unsafe unsafe = UnsafeUtil.getUnsafe();
    protected final        long   offset;

    //这里是抽象类，要用真实类的value字段，所以不能用static块
    {
        try {
            offset = unsafe.objectFieldOffset(this.getClass().getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    public AbstractVolatileLong() {
        set(0);
    }

    public abstract long get();

    /**
     * Perform an ordered write of this cursor. The intent is a Store/Store barrier between this write and any previous store.
     *
     * @param value The new value for the cursor.
     */
    public final void set(final long value) {
        unsafe.putOrderedLong(this, offset, value);
    }

    /**
     * Performs a volatile write of this cursor. The intent is a Store/Store barrier between this write and any previous write and a
     * Store/Load barrier between this write and any subsequent volatile read.
     *
     * @param value The new value for the cursor.
     */
    public final void setVolatile(final long value) {
        unsafe.putLongVolatile(this, offset, value);
    }

    public final long incrementAndGet() {
        return addAndGet(1);
    }

    public final long addAndGet(long increment) {
        return unsafe.getAndAddLong(this, offset, increment);
    }

    public final boolean compareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, offset, expect, update);
    }

    @Override
    public String toString() {
        return Long.toString(get());
    }
}