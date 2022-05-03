package org.songcf.utils;

public class AssertUtils {

    /**
     * val >= comp
     * @param val
     * @param comp
     * @param errorMsg
     */
    public static void isGreaterEqualThan(long val, long comp, String errorMsg) {
        if (val < comp) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * val = 2^N
     * @param val
     * @param errorMsg
     */
    public static void isPower2(int val, String errorMsg) {
        if (Integer.bitCount(val) != 1) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * arg is true
     * @param b
     */
    public static void isTrue(boolean b, String errorMsg) {
        if (!b) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

}