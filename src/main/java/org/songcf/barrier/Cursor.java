package org.songcf.barrier;

import org.songcf.valotile.AbstractVolatileLong;

public class Cursor {

    /**
     * cursor initial value
     */
    public static final long CURSOR_INITIAL_VALUE = -1L;

    AbstractVolatileLong value;

    public Cursor(AbstractVolatileLong relValue, long initValue) {
        value = relValue;
        value.set(initValue);
    }

    public final long get() {
        return value.get();
    }

    public final void set(final long value) {
        this.value.set(value);
    }

    public final void setVolatile(final long value) {
        this.value.setVolatile(value);
    }

    public final long addAndGet(long increment) {
        return value.addAndGet(increment);
    }

    public final boolean compareAndSet(long expect, long update) {
        return value.compareAndSet(expect, update);
    }

    @Override
    public String toString() {
        return Long.toString(get());
    }

}