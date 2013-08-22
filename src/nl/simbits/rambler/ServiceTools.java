package nl.simbits.rambler;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

public class ServiceTools {
	public static boolean isServiceRunning(Context context) {
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        boolean isServiceFound = false;

        for(RunningServiceInfo service : services) {
            if ("nl.simbits.rambler".equals(service.service.getPackageName())) {
                if ("nl.simbits.rambler.RamblerService".equals(service.service.getClassName())) {
                    isServiceFound = true;
                }
            }
        }
        return isServiceFound;
    }
}
