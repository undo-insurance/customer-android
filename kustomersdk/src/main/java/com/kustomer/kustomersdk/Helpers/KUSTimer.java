package com.kustomer.kustomersdk.Helpers;

import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;

public class KUSTimer {

    //region Properties

    private long delay;
    @NonNull
    private Handler handler;
    @NonNull
    private KUSTimerListener timerLister;
    @Nullable
    private Date startTime;
    private boolean running;

    //endregion

    //region Lifecycle

    public KUSTimer(@NonNull Handler handler, long delay, @NonNull KUSTimerListener timerLister) {
        this.delay = delay;
        this.timerLister = timerLister;
        this.handler = handler;
    }
    //endregion

    //region private

    private void runTaskWithDelay(long delay) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                timerLister.onTimerComplete();
                startTime = null;
                running = false;
            }
        }, delay);
    }

    //endregion

    //region Public Methods

    public boolean isRunning() {
        return running;
    }

    public void start() {
        if (startTime == null) {
            startTime = Calendar.getInstance().getTime();
            runTaskWithDelay(delay);
            running = true;
        }
    }

    public void resume() {
        if (startTime != null && !running) {
            long resumeDiff = Calendar.getInstance().getTimeInMillis() - startTime.getTime();

            if (resumeDiff < delay) {
                runTaskWithDelay(delay - resumeDiff);
            } else {
                runTaskWithDelay(0);
            }
            running = true;
        }
    }

    public void pause() {
        if (running) {
            handler.removeCallbacksAndMessages(null);
            running = false;
        }
    }

    public void stop() {
        if (running) {
            handler.removeCallbacksAndMessages(null);
            running = false;
        }
        startTime = null;
    }

    //endregion

    //region Interface

    public interface KUSTimerListener {
        void onTimerComplete();
    }

    //endregion
}
