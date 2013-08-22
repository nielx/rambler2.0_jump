package nl.simbits.rambler;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

public class RamblerApplication extends Application implements
    OnSharedPreferenceChangeListener
{
    private static final String TAG = RamblerApplication.class.getSimpleName();
 
    private RamblerService mService;
    private boolean ramblerServiceRunning; 
    
    @Override 
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
        Log.d(TAG, "onTerminate");
    }

    public synchronized void setService(RamblerService service)
    {
        mService = service;
    }

    public synchronized RamblerService getService()
    {
        return mService;
    }

    public synchronized void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key)
    {
    }

    public boolean isRamblerServiceRunning() 
    {
        return ramblerServiceRunning;
    }

    public void setRamblerServiceRunning(boolean running)
    {
        ramblerServiceRunning = running;
    }
}
