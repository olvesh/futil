package no.oshansen.futil;

import java.util.concurrent.TimeoutException;

/**
 * User: ab35340
 * Date: 08.08.12
 * Time: 14:12
 */
public class FutilTimeoutException extends RuntimeException {
    public FutilTimeoutException(TimeoutException e) {
        super(e);
    }
}
