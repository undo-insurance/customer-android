package com.kustomer.kustomersdk.Managers;

import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;

import com.kustomer.kustomersdk.Helpers.KUSTimer;
import com.kustomer.kustomersdk.Interfaces.KUSVolumeControlTimerListener;

import java.util.HashMap;
import java.util.Map;

public class KUSVolumeControlTimerManager {

    //region Properties

    private static KUSVolumeControlTimerManager volumeControlTimerManager;

    @NonNull
    private HandlerThread thread;
    @NonNull
    private Map<String, KUSTimer> timerMap;

    //endregion

    //region Lifecycle

    private KUSVolumeControlTimerManager() {
        timerMap = new HashMap<>();
        thread = new HandlerThread("TimerHandlerThread");
    }

    @NonNull
    public static KUSVolumeControlTimerManager getSharedInstance() {
        if (volumeControlTimerManager == null)
            volumeControlTimerManager = new KUSVolumeControlTimerManager();

        return volumeControlTimerManager;
    }

    //endregion

    //region Public Methods

    public void createVolumeControlTimer(final String sendToSessionId, long delay,
                                         final KUSVolumeControlTimerListener listener) {
        if (!thread.isAlive())
            thread.start();
        Handler handler = new Handler(thread.getLooper());
        KUSTimer timer = new KUSTimer(handler, delay, new KUSTimer.KUSTimerListener() {
            @Override
            public void onTimerComplete() {
                listener.onDelayComplete();
                timerMap.remove(sendToSessionId);
            }
        });

        timer.start();
        timerMap.put(sendToSessionId, timer);
    }

    public boolean hasAnyTimer() {
        return !timerMap.isEmpty();
    }

    public void removeVcTimers() {
        for (KUSTimer timer : timerMap.values())
            timer.stop();
        timerMap.clear();
        if (timerMap.isEmpty())
            thread.interrupt();
    }

    public boolean sessionHasVcTimer(@NonNull String sessionId) {
        return timerMap.get(sessionId) != null;
    }

    public void removeVcTimer(@NonNull String sessionId) {
        KUSTimer timer = timerMap.get(sessionId);
        if (timer != null) {
            timer.stop();
        }
        timerMap.remove(sessionId);
        if (timerMap.isEmpty())
            thread.interrupt();
    }

    void resumeVcTimers() {
        for (KUSTimer timer : timerMap.values())
            timer.resume();
    }

    public void resumeVcTimer(@NonNull String sessionId) {
        KUSTimer timer = timerMap.get(sessionId);
        if (timer != null)
            timer.resume();
    }

    void pauseVcTimers() {
        for (KUSTimer timer : timerMap.values())
            timer.pause();
    }

    //endregion

}
