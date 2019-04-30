package com.kustomer.kustomersdk;

import android.os.Build;
import android.os.Handler;

import com.kustomer.kustomersdk.Helpers.KUSTimer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O_MR1)
public class TimerTests {

    private Handler handlerSpy;

    private int removeCallbackCount;
    private int postDelayedCount;

    private Long postDelayedInterval;

    private int taskCalled;

    private KUSTimer.KUSTimerListener timerListener = new KUSTimer.KUSTimerListener() {
        @Override
        public void onTimerComplete() {
            taskCalled++;
        }
    };

    //region Private methods

    private Answer getRemoveCallbacksAndMessagesAnswer() {
        return new Answer() {
            public Object answer(InvocationOnMock invocation) {
                //increment requestCount to track method invocation
                removeCallbackCount++;

                return null;
            }
        };
    }

    private Answer getPostDelayedAnswer() {
        return new Answer() {
            public Object answer(InvocationOnMock invocation) {
                //increment requestCount to track method invocation
                postDelayedCount++;

                Object[] args = invocation.getArguments();
                Runnable runnable = (Runnable) args[0];
                long delay = (long) args[1];
                postDelayedInterval = delay;
                if (delay == 0)
                    runnable.run();

                return true;
            }
        };
    }

    private void clearData() {
        postDelayedCount = 0;
        removeCallbackCount = 0;
        postDelayedInterval = null;
        taskCalled = 0;
    }

    //endregion

    @Before
    public void setHandlerSpy() {
        clearData();

        handlerSpy = Mockito.spy(Handler.class);

        Mockito.doAnswer(getPostDelayedAnswer())
                .when(handlerSpy).postDelayed(
                any(Runnable.class),
                any(long.class));

        Mockito.doAnswer(getRemoveCallbacksAndMessagesAnswer())
                .when(handlerSpy).removeCallbacksAndMessages(
                any(Object.class));
    }

    //region Test

    @Test
    public void testTimerConstructor() {
        //startTime should be null and should not call postDelayed
        // when only created timer object
        try {
            Field startTimeField = KUSTimer.class.getDeclaredField("startTime");
            startTimeField.setAccessible(true);

            long delay = 2;

            KUSTimer testTimer = new KUSTimer(handlerSpy, delay, timerListener);

            assertNull(startTimeField.get(testTimer));
            assertEquals(0, postDelayedCount);
            assertNull(postDelayedInterval);

        } catch (Exception e) {
        }
    }

    @Test
    public void testTimerStart() {
        //startTime should be nonNull,postDelayed should call 1 time and postDelayedInterval should be equal to delay
        //when start timer called of new timer object
        try {
            Field startTimeField = KUSTimer.class.getDeclaredField("startTime");
            startTimeField.setAccessible(true);

            long delay = 2;

            KUSTimer testTimer = new KUSTimer(handlerSpy, delay, timerListener);
            testTimer.start();

            assertNotNull(startTimeField.get(testTimer));

            assertEquals(1, postDelayedCount);
            assertNotNull(postDelayedInterval);
            assertEquals(postDelayedInterval.longValue(), delay);
            assertTrue(testTimer.isRunning());
        } catch (Exception e) {
        }
    }

    @Test
    public void testTimerResume() {
        //postDelayed should call 2 times and removeCallback should call 1 time
        //when timer resumed after being paused
        long delay = 2;

        KUSTimer testTimer = new KUSTimer(handlerSpy, delay, timerListener);
        testTimer.start();
        testTimer.pause();
        testTimer.resume();

        assertEquals(1, removeCallbackCount);
        assertEquals(2, postDelayedCount);
        assertTrue(testTimer.isRunning());


        //postDelayed should call 1 time, removeCallback should call 1 time and tast should run
        //when timer resumed after being paused and delay is 0
        clearData();
        delay = 0;

        testTimer = new KUSTimer(handlerSpy, delay, timerListener);
        testTimer.start();
        testTimer.pause();
        testTimer.resume();

        assertEquals(1, removeCallbackCount);
        assertEquals(1, postDelayedCount);
        assertEquals(1, taskCalled);

        //should not call task twice
        //when resume is called consecutively
        clearData();
        delay = 0;

        testTimer = new KUSTimer(handlerSpy, delay, timerListener);
        testTimer.start();
        testTimer.pause();
        testTimer.resume();
        testTimer.resume();

        assertEquals(1, removeCallbackCount);
        assertEquals(1, postDelayedCount);
        assertEquals(1, taskCalled);


        //should not call postDelayed more then once
        //when resume is called after start
        clearData();
        delay = 2;

        testTimer = new KUSTimer(handlerSpy, delay, timerListener);
        testTimer.start();
        testTimer.resume();

        assertEquals(1, postDelayedCount);
    }

    @Test
    public void testTimerPause() {
        //should call removeCallback
        //when pause is called after start

        long delay = 2;

        KUSTimer testTimer = new KUSTimer(handlerSpy, delay, timerListener);
        testTimer.start();
        testTimer.pause();

        assertEquals(1, removeCallbackCount);
        assertFalse(testTimer.isRunning());
    }

    @Test
    public void testTimerStop() {
        //should call removeCallback
        //when stop is called after start

        long delay = 2;

        KUSTimer testTimer = new KUSTimer(handlerSpy, delay, timerListener);
        testTimer.start();

        testTimer.stop();
        assertEquals(1, removeCallbackCount);
        assertFalse(testTimer.isRunning());


        //should not call resume timer and startTime should be null
        //when stop is called

        clearData();
        try {
            Field startTimeField = KUSTimer.class.getDeclaredField("startTime");
            startTimeField.setAccessible(true);

            delay = 2;

            testTimer = new KUSTimer(handlerSpy, delay, timerListener);
            testTimer.start();
            testTimer.pause();
            testTimer.stop();
            testTimer.resume();

            assertEquals(1, removeCallbackCount);
            assertEquals(1, postDelayedCount);

            assertNull(startTimeField.get(testTimer));
        } catch (Exception e) {

        }
    }

    //endregion
}
