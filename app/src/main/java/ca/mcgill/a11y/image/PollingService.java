package ca.mcgill.a11y.image;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import org.json.JSONException;

import java.io.IOException;

public class PollingService extends Service {
    private Handler handler;
    public static final long DEFAULT_SYNC_INTERVAL = 5 * 1000;

    private Runnable runnableService = new Runnable() {
        @Override
        public void run() {
            //create AsyncTask here
            try {
                DataAndMethods.getFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            handler.postDelayed(runnableService, DEFAULT_SYNC_INTERVAL);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler = new Handler();
        handler.post(runnableService);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnableService);
        stopSelf();
        super.onDestroy();
    }
}