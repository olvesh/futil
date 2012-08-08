package no.oshansen.futil;


import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.*;

import static no.oshansen.futil.Futil.Flags.*;


/**
 * Handles exceptions, and deals with associated tasks, cancelling them if something happens with the one being unwrapped.
 * Will
 * User: ab35340
 * Date: 3/15/12
 * Time: 8:51 AM
 */
public class Futil {

    private static final long NONE = -1;
    public static final FutureTask[] EMPTY_FUTURES = new FutureTask[0];

    public enum Flags {
        /**
         * Ignores InterruptedException, but will still cancel other tasks.
         */
        ignoreInterruptedReturnNull,
        /**
         * Will avoid cancelling other tasks on interrupt
         */
        noCancelOnInterrupt,
        /**
         * Will avoid cancelling other tasks on timeout
         */
        noCancelOnTimeout,
        /**
         * Ignores TimeoutException, but will still cancel other tasks.
         */
        ignoreTimeoutReturnNull,
        /**
         * Prevents cancelling other tasks if they have started running, cancels if they not yet have started.
         */
        noInterruptRunningTask,
        /**
         * If a task to be unwrapped was cancel, default is to return null - this will rethrow the CancellationException
         */
        rethrowCancellationException,
        /**
         * Prevents cancelling other tasks if the task to be unwrapped was cancelled.
         */
        noCancelAssociatedTasksOnCancellationException;

        boolean isFlagSet(Set<Flags> flags) {
            return flags.contains(this);
        }

        boolean isFlagNotSet(Set<Flags> flags) {
            return !flags.contains(this);
        }
    }

    /**
     * Call this method to unwrap your future, it will handle exceptions for you.
     *
     * @param future          the future to unwrap
     * @param timeout         how long can we wait for the result
     * @param timeUnit        time unit for timeout
     * @param futileOptions   Flags for the call
     * @param cancelOnFailure other dependant futures we want to cancel on failure of this task.
     * @param <T>             the type of the future
     * @return the unwrapped future, may be null depending on flags
     */
    public static <T> T unwrap(Future<T> future, long timeout, TimeUnit timeUnit, Set<Flags> futileOptions, Future... cancelOnFailure) {
        T t = null;
        try {
            t = timeout == NONE ? future.get() : future.get(timeout, timeUnit);
        } catch (InterruptedException e) {
            cancel(cancelOnFailure, futileOptions);
            handleException(future, futileOptions, e);
        } catch (ExecutionException e) {
            cancel(cancelOnFailure, futileOptions);
            throw new RuntimeException(e.getCause());
        } catch (TimeoutException e) {
            cancel(cancelOnFailure, futileOptions);
            handleException(future, futileOptions, e);
        } catch (CancellationException e) {
            cancel(cancelOnFailure, futileOptions);

            if (rethrowCancellationException.isFlagSet(futileOptions))
                throw e;
        }
        return t;
    }

    protected static <T> void handleException(Future<T> future, Set<Flags> futileOptions, TimeoutException e) {
        if (noCancelOnTimeout.isFlagNotSet(futileOptions))
            future.cancel(noInterruptRunningTask.isFlagNotSet(futileOptions));
        if (ignoreTimeoutReturnNull.isFlagNotSet(futileOptions))
            throw new FutilTimeoutException(e);
    }

    protected static <T> void handleException(Future<T> future, Set<Flags> futileOptions, InterruptedException e) {
        if (noCancelOnInterrupt.isFlagNotSet(futileOptions))
            cancel(future, futileOptions);
        if (ignoreInterruptedReturnNull.isFlagNotSet(futileOptions))
            throw new FutilInterruptedException(e);
    }

    protected static void cancel(Future[] cancelOnFailure, Set<Flags> futileOptions) {
        if (noCancelAssociatedTasksOnCancellationException.isFlagNotSet(futileOptions))
            for (Future futureTask : cancelOnFailure) {
                boolean didCancel = cancel(futureTask, futileOptions);
                //LOG.warning(didCancel futureTask)
            }
    }

    public static <T> T unwrap(Future<T> future, long timeout) {
        return unwrap(future, timeout, EnumSet.noneOf(Flags.class));
    }

    public static <T> T unwrap(Future<T> future, long timeout, EnumSet<Flags> futileOptions) {
        return unwrap(future, timeout, TimeUnit.SECONDS, futileOptions);
    }

    public static <T> T unwrap(Future<T> future, long timeout, TimeUnit timeUnit, EnumSet<Flags> futileOptions) {
        return unwrap(future, timeout, timeUnit, futileOptions, EMPTY_FUTURES);
    }

    public static <T> T unwrap(Future<T> future, long timeout, TimeUnit timeUnit, Future... cancelOnException) {
        return unwrap(future, timeout, timeUnit, EnumSet.noneOf(Flags.class), cancelOnException);
    }

    public static <T> T unwrapNoTimeout(Future<T> future, EnumSet<Flags> futileOptions) {
        return unwrap(future, NONE, TimeUnit.SECONDS, futileOptions);
    }

    public static <T> T unwrapNoTimeout(Future<T> future, Future... cancelOnException) {
        return unwrap(future, NONE, TimeUnit.SECONDS, EnumSet.noneOf(Flags.class), cancelOnException);
    }

    private static boolean cancel(Future<?> future, Set<Flags> futileOptions) {
        return future.cancel(noInterruptRunningTask.isFlagNotSet(futileOptions));
    }

}
