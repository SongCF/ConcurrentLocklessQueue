package org.songcf.utils;

public class ThreadFactoryUtil {

    public static Thread newThread(final Runnable r) {
        Thread t = new Thread(r);
        return t;
    }
}