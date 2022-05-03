package org.songcf.valotile;

import org.songcf.utils.UnsafeUtil;
import sun.misc.Unsafe;

public abstract class AbstractVolatileLong {

    protected static final Unsafe unsafe = UnsafeUtil.getUnsafe();
    protected static final long   offset;

    static {
        try {
            offset = unsafe.objectFieldOffset(VolatileLongValueWithLongPadding.class.getDeclaredField("value"));
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