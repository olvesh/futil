package no.oshansen.futil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static java.util.EnumSet.of;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.oshansen.futil.Futil.*;
import static no.oshansen.futil.Futil.Flags.*;
import static org.mockito.Mockito.*;

/**
 * User: ab35340
 * Date: 3/15/12
 * Time: 11:16 AM
 */
@SuppressWarnings("unchecked")
public class FutilTest {


    public static final String GOT = "Got";
    private Future<String> future;
    private Future<String> associated1;
    private Future<String> associated2;

    @Before
    public void setUp() throws Exception {
        future = Mockito.mock(Future.class);
        associated1 = Mockito.mock(Future.class);
        associated2 = Mockito.mock(Future.class);
    }

    @Test
    public void testBaselineUnwrap() throws Exception {
        when(future.get()).thenReturn(GOT);

        assert GOT.equals(unwrapNoTimeout(future));
    }

    @Test
    public void testExceptionUnwrap() throws Exception {
        when(future.get()).thenThrow(new ExecutionException(new RuntimeException()));
        RuntimeException exc = null;
        try {
            unwrapNoTimeout(future, associated1, associated2);
        } catch (RuntimeException e) {
            exc = e;
        }
        assert exc != null;
        verify(associated1).cancel(true);
        verify(associated2).cancel(true);
    }

    @Test
    public void testCancellationExceptionUnwrap() throws Exception {
        when(future.get()).thenThrow(new CancellationException());
        CancellationException exc = null;
        try{
            unwrapNoTimeout(future, associated1, associated2);
        } catch (CancellationException e) {
            exc = e;
        }
        assert exc == null;
        verify(associated1).cancel(true);
        verify(associated2).cancel(true);
    }

    @Test
    public void testCancellationExceptionNoAssociatedCancellationUnwrap() throws Exception {
        when(future.get()).thenThrow(new CancellationException());
        RuntimeException exc = null;
        try {
            unwrap(future, -1, SECONDS,
                    of(noCancelAssociatedTasksOnCancellationException, rethrowCancellationException),
                    associated1, associated2);
        } catch (CancellationException e) {
            exc = e;
        }
        assert exc != null;
        verify(associated1, never()).cancel(true);
        verify(associated1, never()).cancel(false);
        verify(associated2, never()).cancel(false);
        verify(associated2, never()).cancel(true);
    }



    @Test
    public void testTimeoutExceptionUnwrap() throws Exception {
        when(future.get(1L, SECONDS)).thenThrow(new TimeoutException());
        RuntimeException exc = null;
        try {
            unwrap(future, 1L, SECONDS, associated1, associated2);
        } catch (FutilTimeoutException e) {
            exc = e;
        }
        verify(future).get(1L, SECONDS);
        verify(future).cancel(true);
        verify(associated1).cancel(true);
        verify(associated2).cancel(true);
        assert exc != null;
    }


    @Test
    public void testInterruptedExceptionUnwrap() throws Exception {
        when(future.get(1L, SECONDS)).thenThrow(new InterruptedException());
        RuntimeException exc = null;
        try {
            unwrap(future, 1L, SECONDS, associated1, associated2);
        } catch (FutilInterruptedException e) {
            exc = e;
        }
        verify(future).get(1L, SECONDS);
        verify(future).cancel(true);
        verify(associated1).cancel(true);
        verify(associated2).cancel(true);

        assert exc != null;
    }

    @Test
    public void testInterruptedNoInterruptTaskExceptionUnwrap() throws Exception {
        when(future.get(1L, SECONDS)).thenThrow(new InterruptedException());
        RuntimeException exc = null;
        try {
            unwrap(future, 1L, SECONDS, of(noInterruptRunningTask), associated1, associated2);
        } catch (FutilInterruptedException e) {
            exc = e;
        }
        verify(future).get(1L, SECONDS);
        verify(future).cancel(false);
        verify(associated1).cancel(false);
        verify(associated2).cancel(false);
        assert exc != null;
    }


    @Test
    public void testInterruptedNoCancelTaskExceptionUnwrap() throws Exception {
        when(future.get(1L, SECONDS)).thenThrow(new InterruptedException());
        RuntimeException exc = null;
        try {
            unwrap(future, 1L, SECONDS, of(Futil.Flags.noCancelOnInterrupt), associated1, associated2);
        } catch (FutilInterruptedException e) {
            exc = e;
        }
        verify(future).get(1L, SECONDS);
        verify(future, never()).cancel(false);
        verify(future, never()).cancel(true);
        verify(associated1).cancel(true);
        verify(associated1, never()).cancel(false);
        verify(associated2).cancel(true);
        verify(associated2, never()).cancel(false);
        assert exc != null;
    }

    @Test
    public void testTimeoutNoInterruptTaskExceptionUnwrap() throws Exception {
        when(future.get(1L, SECONDS)).thenThrow(new TimeoutException());
        RuntimeException exc = null;
        try {
            unwrap(future, 1L, SECONDS, of(noInterruptRunningTask), associated1, associated2);
        } catch (FutilTimeoutException e) {
            exc = e;
        }
        verify(future).get(1L, SECONDS);
        verify(future).cancel(false);
        verify(associated1).cancel(false);
        verify(associated2).cancel(false);
        assert exc != null;
    }


    @Test
    public void testTimeoutNoCancelTaskExceptionUnwrap() throws Exception {
        when(future.get(1L, SECONDS)).thenThrow(new TimeoutException());
        RuntimeException exc = null;
        try {
            unwrap(future, 1L, SECONDS,
                    of(Futil.Flags.noCancelOnTimeout, noInterruptRunningTask),
                    associated1, associated2);
        } catch (FutilTimeoutException e) {
            exc = e;
        }
        verify(future).get(1L, SECONDS);
        verify(future, never()).cancel(false);
        verify(future, never()).cancel(true);

        verify(associated1, never()).cancel(true);
        verify(associated1).cancel(false);
        verify(associated2, never()).cancel(true);
        verify(associated2).cancel(false);

        assert exc != null;
    }

    @Test
    public void testInterruptedNoExceptionUnwrap() throws Exception {
        when(future.get(1L, SECONDS)).thenThrow(new InterruptedException());

        final String unwrap = unwrap(future, 1L, SECONDS,
                of(noInterruptRunningTask, ignoreInterruptedReturnNull),
                associated1, associated2);
        assert null == unwrap;

        verify(future).get(1L, SECONDS);
        verify(future).cancel(false);
        verify(associated1).cancel(false);
        verify(associated2).cancel(false);
    }


    @Test
    public void testInterruptedNoCancelTaskNoExceptionUnwrap() throws Exception {
        when(future.get(1L, SECONDS)).thenThrow(new InterruptedException());

        final String unwrap = unwrap(future, 1L, SECONDS,
                of(Futil.Flags.noCancelOnInterrupt, ignoreInterruptedReturnNull),
                associated1, associated2);
        assert null == unwrap;

        verify(future).get(1L, SECONDS);
        verify(future, never()).cancel(false);
        verify(future, never()).cancel(true);


        verify(associated1).cancel(true);
        verify(associated1, never()).cancel(false);
        verify(associated2).cancel(true);
        verify(associated2, never()).cancel(false);

    }

    @Test
    public void testTimeoutNoInterruptTaskNoExceptionUnwrap() throws Exception {
        when(future.get(1L, SECONDS)).thenThrow(new TimeoutException());

        final String unwrap = unwrap(future, 1L, SECONDS,
                of(noInterruptRunningTask, ignoreTimeoutReturnNull),
                associated1, associated2);
        assert null == unwrap;

        verify(future).get(1L, SECONDS);
        verify(future).cancel(false);

        verify(associated1).cancel(false);
        verify(associated2).cancel(false);
    }


    @Test
    public void testTimeoutNoCancelTaskNoExceptionUnwrap() throws Exception {
        when(future.get(1L, SECONDS)).thenThrow(new TimeoutException());

        final String unwrap = unwrap(future, 1L, SECONDS,
                of(ignoreTimeoutReturnNull, noCancelOnTimeout, noInterruptRunningTask),
                associated1, associated2);
        assert null == unwrap;

        verify(future).get(1L, SECONDS);
        verify(future, never()).cancel(false);
        verify(future, never()).cancel(true);


        verify(associated1, never()).cancel(true);
        verify(associated1).cancel(false);
        verify(associated2, never()).cancel(true);
        verify(associated2).cancel(false);

    }



}
