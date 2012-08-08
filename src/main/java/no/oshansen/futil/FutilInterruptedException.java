package no.oshansen.futil;

/**
 * User: ab35340
 * Date: 08.08.12
 * Time: 14:12
 */
public class FutilInterruptedException extends RuntimeException {
    public FutilInterruptedException(InterruptedException e) {
        super(e);
    }
}
