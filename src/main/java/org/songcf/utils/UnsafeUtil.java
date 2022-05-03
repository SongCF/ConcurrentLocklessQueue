package org.songcf.utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

public class UnsafeUtil {

    private static final Unsafe THE_UNSAFE;

    ///**
    // * 通过反射方式获取
    // *
    // * @return
    // */
    //public static Unsafe reflectGetUnsafe() {
    //    try {
    //        Field field = Unsafe.class.getDeclaredField("theUnsafe");
    //        field.setAccessible(true);
    //        THE_UNSAFE = (Unsafe) field.get(null);
    //    } catch (NoSuchFieldException | IllegalAccessException e) {
    //        throw new RuntimeException("Unable to load unsafe", e);
    //    }
    //    return THE_UNSAFE;
    //}

    static {
        try {
            final PrivilegedExceptionAction<Unsafe> action = new PrivilegedExceptionAction<Unsafe>() {
                public Unsafe run() throws Exception {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    return (Unsafe) theUnsafe.get(null);
                }
            };

            THE_UNSAFE = AccessController.doPrivileged(action);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load unsafe", e);
        }
    }

    /**
     * Get a handle on the Unsafe instance, used for accessing low-level concurrency and memory constructs.
     *
     * @return The Unsafe
     */
    public static Unsafe getUnsafe() {
        return THE_UNSAFE;
    }
}