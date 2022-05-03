package org.songcf;

public interface EventFactory<T> {
    /**
     * new
     *
     * @return
     */
    T newInstance();
}