package info.kgeorgiy.ja.churakova.bank;

import java.io.Serializable;

/**
 * For local instances
 *
 * @param <T> class of object that implements this interface
 */
@FunctionalInterface
public interface Local<T> extends Serializable {
    /**
     * Copies all parameters from given sample
     *
     * @param sample object from which makes copy
     */
    void copyDataFromSample(T sample);
}
