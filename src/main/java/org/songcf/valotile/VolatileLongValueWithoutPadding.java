package org.songcf.valotile;

public class VolatileLongValueWithoutPadding extends AbstractVolatileLong {

    protected volatile long value;

    public final long get() {
        return value;
    }

}