package nl.simbits.rambler;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.System;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.List;
import java.util.ListIterator;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import nl.simbits.rambler.social.FacebookEvent;
import nl.simbits.rambler.social.FacebookUtilities;

public class RamblerService extends Service 
{
    static final String TAG = "RamblerService";
    private static final String BLUETOOTH_NAME_SHOE_1 = "Rambler";

    public static final int STEP_VIB_LENGTH	= 100;
    public static final int JUMP_VIB_LENGTH	= 1000;
    public static final int EVENT_UPDATE_INTERVAL = 5 * 60 * 1000; /* 5 minutes */
    public static final int EVENT_ARRIVAL_DISTANCE = 200; /* meters;
    
    /* Handler messages */
    public static final int SHOE_POST_STEPS = 1;
    public static final int SHOE_RECEIVED_STEP = 2;
    public static final int SHOE_RECEIVED_JUMPS = 3;
    public static final int SHOE_CONNECTION_STATE = 4;
    public static final int LOCATION_NEW = 5;

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public static final int STEP_POST_THRESHOLD = 6;
    public static final int STEP_POST_DELAY = 20 * 60 * 1000;
    public static final int SG_SAMPLE_BUFFER = 32;
    public static final int SG_WINDOW = 15;

    private final IBinder mServiceBinder = new ServiceBinder();
    
    private boolean isRunning = false;
    private RamblerThread ramblerThread;
    private RamblerApplication rambler;
    private SavitzkyGolayStepDetector mJumpStepDetector;
    private RamblerShoeConnector mShoeConnector;
    private RamblerLocationManager mLocationManager;
    private RamblerLocation mLastBestLocation = null;
    private FacebookUtilities mFacebook = null;
    private SharedPreferences mPreferences = null;
    private OnSharedPreferenceChangeListener mPreferenceListener;
    private int mStepsWalked = 0;
    private boolean mVibrateOnStep;
    private boolean mVibrateOnJump;
    private Notification mNotification;
    
    void startInForeground()
    {
        Intent intent = new Intent(this, Rambler.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

        mNotification = new Notification(R.drawable.icon, "Rambler service", System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_NO_CLEAR;
        mNotification.setLatestEventInfo(this, "Rambler", "Listening for steps", pi);

        startForeground(9999, mNotification);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind"); 
        return mServiceBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "onCreate");

        rambler = (RamblerApplication) getApplication();
        ramblerThread = new RamblerThread(mHandler);        
        mLocationManager = new RamblerLocationManager(getApplicationContext());
        mFacebook = FacebookUtilities.getInstance();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mPreferenceListener = new PreferencesChangedListener();
        mPreferences.registerOnSharedPreferenceChangeListener(mPreferenceListener);
        rambler.setService(this);
       
        mJumpStepDetector = createSGJumpStepDetector(mPreferences, new ShoeEventListener(mHandler));
        mShoeConnector = new RamblerShoeConnector(this, mJumpStepDetector);
        
        mHandler.sendMessageDelayed(mHandler.obtainMessage(SHOE_POST_STEPS), STEP_POST_DELAY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand");

        if (isRunning)
        	return START_STICKY;
        
        this.isRunning = true;
        this.ramblerThread.start();
        this.rambler.setRamblerServiceRunning(true);

        if (!mShoeConnector.running()) {
        	mShoeConnector.connect(BLUETOOTH_NAME_SHOE_1);
        }
            
        mLocationManager.startListening(new LocationListener(mHandler));
        
        startInForeground();
        
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        this.isRunning = false;
        this.ramblerThread.interrupt();
        this.ramblerThread = null;
        this.rambler.setRamblerServiceRunning(false);

        if (mShoeConnector.running()) {
        	mShoeConnector.cancel();
        	mShoeConnector = null;
        }
       
        mLocationManager.stopListening();

        stopForeground(true);
    }

    public void updateNotification(String msg) {
        Intent intent = new Intent(this, Rambler.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

       // mNotification = new Notification(R.drawable.icon, "Rambler service", System.currentTimeMillis());
       // mNotification.flags |= Notification.FLAG_NO_CLEAR;
        mNotification.setLatestEventInfo(RamblerService.this, "Rambler", msg, pi);
        synchronized(mNotification) {
        	mNotification.notify();
        }
    }
    
	public void startBluetoothConnection() {
 		Log.d(TAG, "start bluetooth connection");

     	if (mShoeConnector != null) {
     		mShoeConnector.cancel();
     		mShoeConnector = null;
     		mJumpStepDetector = null;
     	}    
     	
        mJumpStepDetector = createSGJumpStepDetector(mPreferences, new ShoeEventListener(mHandler));
     	mShoeConnector = new RamblerShoeConnector(RamblerService.this, mJumpStepDetector); 
     	mShoeConnector.connect(BLUETOOTH_NAME_SHOE_1);
	}
	
	public void stopBluetoothConnection() {
		if (mShoeConnector != null) {
			mShoeConnector.cancel();
			mShoeConnector = null;
			mJumpStepDetector = null;
		}
	}
	
	public void broadcastBluetoothState() {
		if (mShoeConnector != null) {
			mShoeConnector.broadcastState();
		}
	}
	
	private SavitzkyGolayStepDetector createSGJumpStepDetector(SharedPreferences prefs, ShoeEventListener listener) {
		int buffer = prefs.getInt("SGSampleBuffer", SG_SAMPLE_BUFFER);
		int window = prefs.getInt("SGWindow", SG_WINDOW);
		int pos = prefs.getInt("SGPosThreshold", 30);
		int neg = prefs.getInt("SGNegThreshold", -30);
		int jumpWindow = prefs.getInt("SGJumpEventWindow", 1000);
		int interval = prefs.getInt("SGJumpInterval", 3000);

		Log.d(TAG, "Set SG Samples: " + buffer);
		Log.d(TAG, "Set SG Window: " + window);
		Log.d(TAG, "Set SG Thresholds: " + pos + ", " + neg);
		Log.d(TAG, "Set SG Jump Event Window: " + jumpWindow);
		Log.d(TAG, "Set SG Jump Interval: " + interval);
		
		SavitzkyGolayStepDetector detector = new SavitzkyGolayStepDetector(buffer, window, listener);
		
		detector.setPeakThresholds(pos, neg);
		detector.setJumpEventWindow(jumpWindow);
		detector.setJumpInterval(interval);

		mVibrateOnStep = prefs.getBoolean("SDVibrateOnStep", false);
		mVibrateOnJump = prefs.getBoolean("SDVibrateOnJump", false);
		
		Log.d(TAG, "Set vibrate on step: " + mVibrateOnStep);
		Log.d(TAG, "Set vibrate on jump: " + mVibrateOnJump);
		
		
		return detector;
	}
	 
    private class RamblerThread extends Thread
    {
        public RamblerThread(Handler handler)
        {
            super("RamblerService-RamblerThread");
        }

        @Override
        public void run()
        {
            Looper.prepare();
            
            RamblerService ramblerService = RamblerService.this;

            try {
                while (ramblerService.isRunning) {
                    Log.d(TAG, "ramblerThread update facebook events");

                    mFacebook.updateEvents();

                    Thread.sleep(EVENT_UPDATE_INTERVAL);
                    Log.d(TAG, "ramblerThread check facebook events");

                    List<FacebookEvent> events = mFacebook.getEventsInVicinityOf(mLastBestLocation,
                    															EVENT_ARRIVAL_DISTANCE);
                    if (!events.isEmpty()) {
                        ListIterator<FacebookEvent> i = events.listIterator();
                        while (i.hasNext()) {
                            FacebookEvent event = i.next();
                            mFacebook.attendEvent(event);
                            String msg = "Just arrived at " + event.getName() + ": http://www.facebook.com/events/" + event.getGraphId() + "/";
                            mFacebook.post(msg);
                            try {
                                TwitterUtilities.sendTweetAsync(mPreferences, msg,
                                                                mLastBestLocation.getLatitude(), 
                                                                mLastBestLocation.getLongitude());
                            } catch (Exception e) {
                                Log.w(TAG, "Failed sending tweet: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                ramblerService.isRunning = false;
            }
        }
    }
  
    private class LocationListener implements RamblerLocationListener {
        public static final String TAG = "RamblerLocationListener";
        private static final int TWO_MINUTES = 1000 * 30;

        private Location mmCurrentLocation = null;
        private Handler mmHandler = null;

        public LocationListener(Handler handler) {
            mmHandler = handler;
        }

        protected boolean isBetterLocation(Location location, Location currentBestLocation) {
            if (currentBestLocation == null) {
                // A new location is always better than no location
                return true;
            }

            // Check whether the new location fix is newer or older
            long timeDelta = location.getTime() - currentBestLocation.getTime();
            boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
            boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
            boolean isNewer = timeDelta > 0;

            // If it's been more than two minutes since the current location, use the new location
            // because the user has likely moved
            if (isSignificantlyNewer) {
                return true;
                // If the new location is more than two minutes older, it must be worse
            } else if (isSignificantlyOlder) {
                return false;
            }

            // Check whether the new location fix is more or less accurate
            int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
            boolean isLessAccurate = accuracyDelta > 0;
            boolean isMoreAccurate = accuracyDelta < 0;
            boolean isSignificantlyLessAccurate = accuracyDelta > 200;

            // Check if the old and new location are from the same provider
            boolean isFromSameProvider = isSameProvider(location.getProvider(),

            currentBestLocation.getProvider());
            // Determine location quality using a combination of timeliness and accuracy
 
            if (isMoreAccurate) {
                return true;
            } else if (isNewer && !isLessAccurate) {
                return true;
            } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
                return true;
            }
            return false;
        }

        /** Checks whether two providers are the same */
        private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
            return provider1.equals(provider2);
        }

        @Override
        public void onLocationChanged(Location location) {
            if (isBetterLocation(location, mmCurrentLocation)) {
                Log.d(TAG, "New better location [" + location.getLatitude() + ", " + location.getLongitude() + "]");
            } else {
                Log.d(TAG, "Old location still better");
                return;
            }
            
            mmCurrentLocation = location;
            mmHandler.obtainMessage(LOCATION_NEW, location).sendToTarget();
        }

        @Override
        public void onReachedDestination(Location location) { }
    }
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
               case SHOE_CONNECTION_STATE:
                   switch(msg.arg1) {
                       case BluetoothSPPConnector.BT_CONNECTED:
                            Toast.makeText(getApplicationContext(), "Rambler connected to shoe", Toast.LENGTH_SHORT).show();
                            break;
                       default:
                            Toast.makeText(getApplicationContext(), "Rambler bluetooth  " + BluetoothSPPConnector.stateToString(msg.arg1), Toast.LENGTH_SHORT).show();
                            break;
                   }
                   break;
               case SHOE_POST_STEPS:
                   Toast.makeText(getApplicationContext(), "Rambler posting steps", Toast.LENGTH_SHORT).show();
                   if (mStepsWalked > 0) {
                        /* post twitter message */
                       if (mLastBestLocation != null) {

                           String tweet = new Formatter().format("%ts: I completed %d steps here!", 
                                                                 System.currentTimeMillis(),
                                                                 mStepsWalked).out().toString();

                           try {
                               TwitterUtilities.sendTweetAsync(mPreferences, tweet, mLastBestLocation.getLatitude(), mLastBestLocation.getLongitude());
                           } catch (Exception e) {
                               Log.e(TAG, "Failed posting tweet: " + e.getMessage());
                           }        
                       }
                       mStepsWalked = 0;
                   }
                   sendMessageDelayed(obtainMessage(SHOE_POST_STEPS), STEP_POST_DELAY);
                   break;
               case SHOE_RECEIVED_STEP: 
                   mStepsWalked++;
                   break;
               case SHOE_RECEIVED_JUMPS: 
                   Log.d(TAG, "Rambler received jumps: " + msg.arg1);
                   Toast.makeText(getApplicationContext(), "Rambler received jumps: " + msg.arg1, Toast.LENGTH_SHORT).show();

                   if (mLastBestLocation == null) {
                	   Log.i(TAG, "No location yet");
                	   break;
                   }
                   
                   String mapURLFormat = "http://maps.googleapis.com/maps/api/streetview?size=640x480&location=%.5f,%.5f&fov=90&heading=0&pitch=0&sensor=true";
                   String mapURL = new Formatter().format(mapURLFormat, 
                                                          mLastBestLocation.getLatitude(), 
                                                          mLastBestLocation.getLongitude())
                                                  .out()
                                                  .toString();

                   String addr = mLastBestLocation.getAddressLine().toString();
                   if (addr.equals("")) {
                       addr = new Formatter().format("lat: %.5f, lon: %.5f", 
                                                     mLastBestLocation.getLatitude(), 
                                                     mLastBestLocation.getLongitude())
                                                     .out()
                                                     .toString(); 
                   }
                   Log.i(TAG, "Best location address: " + addr);

                   switch (msg.arg1) {
                       case 3:
                           Log.i(TAG, "Received 3 jumps");

                           String message = "";

                           try {
                               message = URLEncoder.encode("Just arrived at " + addr,"UTF-8");
                           } catch (java.io.UnsupportedEncodingException e) {
                               Log.w(TAG, "Could not encode message: " + e.getMessage());
                           }

                           String storeURLFormat = "http://rambler.projects.simbits.nl/map/update.php?lat=%.5f&lon=%.5f&msg=%s";
                           String storeURL = new Formatter().format(storeURLFormat, 
                                                                    mLastBestLocation.getLatitude(), 
                                                                    mLastBestLocation.getLongitude(),
                                                                    message)
                                                            .out()
                                                            .toString();

                           AsyncHttpClient client = new AsyncHttpClient();
                           client.get(storeURL, new AsyncHttpResponseHandler() {
                                   @Override
                                   public void onSuccess(String response) {
                                               Log.d(TAG, "google map update response: " + response);
                                           }
                           });
                       case 2:
                           Log.i(TAG, "Received 2 jumps");
                           try {
                               mFacebook.postPhoto("Just arrived at " + addr, mapURL, true);
                           } catch (Exception e) {
                               Log.e(TAG, "Failed posting to facebook: " + e.getMessage());
                           }
                           try {
                               TwitterUtilities.sendTweetAsync(mPreferences, 
                                                               "Just arrived at " + addr +": " + mapURL,
                                                               mLastBestLocation.getLatitude(), 
                                                               mLastBestLocation.getLongitude());
                           } catch (Exception e) {
                               Log.e(TAG, "Failed posting tweet: " + e.getMessage());
                           } 
                           break;
                       case 1:
                        Log.i(TAG, "Received 1 jump");
                           try {
                               TwitterUtilities.sendTweetAsync(mPreferences, 
                                                               "I am here: " + mapURL,
                                                               mLastBestLocation.getLatitude(), 
                                                               mLastBestLocation.getLongitude());

                               /*
                               URL url = new URL(mapURL);
                               URLConnection ucon = url.openConnection();
                               TwitterUtilities.sendTweetAsync(mPreferences, 
                                                               "Stepping around " + addr, 
                                                               mLastBestLocation.getLatitude(), 
                                                               mLastBestLocation.getLongitude(), 
                                                               "place", ucon.getInputStream());
                               */
                           } catch (Exception e) {
                               Log.e(TAG, "Failed posting tweet: " + e.getMessage());
                           } 
                           break;
                       default:
                           Log.i(TAG, "Received " + msg.arg1 + " jumps");
                   }
                   break;
               case LOCATION_NEW:
                   Location location = (Location)msg.obj;
                   
                   if (mLastBestLocation == null) {
                	   updateNotification("Received location");
                   }
                   mLastBestLocation = new RamblerLocation(getApplicationContext(), location);
                   break;
            }
        }
    };
    
	private class ShoeEventListener implements JumpStepEventDetector.eventListener {
		private Handler mHandler;

		public ShoeEventListener(Handler handler) {
			mHandler = handler;
		}
		
		@Override 
		public void onJump(long nanoSeconds) {
			if (mVibrateOnJump) {
				Vibrator v = (Vibrator)rambler.getSystemService(VIBRATOR_SERVICE);
				v.vibrate(JUMP_VIB_LENGTH);
			}
		}
		
		@Override 
		public void onJumps(int jumps, long nanoSeconds) {
            mHandler.obtainMessage(SHOE_RECEIVED_JUMPS, jumps, -1).sendToTarget();
		}

		@Override
		public void onStep(long nanoSeconds) {
			if (mVibrateOnStep) {
				Vibrator v = (Vibrator)rambler.getSystemService(VIBRATOR_SERVICE);
				v.vibrate(STEP_VIB_LENGTH);
			}
            mHandler.obtainMessage(SHOE_RECEIVED_STEP).sendToTarget();
		}
	}
	
	public class PreferencesChangedListener implements OnSharedPreferenceChangeListener {

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			Log.d(TAG, "on shared prefereneces changed: " + key);
			if ("SGPosThreshold".equals(key) || "SGNegThreshold".equals(key)) {
				int pos = sharedPreferences.getInt("SGPosThreshold", 30);
				int neg = sharedPreferences.getInt("SGPosThreshold", -30);
				Log.d(TAG, "Set SG Thresholds: " + pos + ", " + neg);
				mJumpStepDetector.setPeakThresholds(pos, neg);
			} else if ("SGJumpEventWindow".equals(key)) {
				int window = sharedPreferences.getInt("SGJumpEventWindow", 1000);
				Log.d(TAG, "Set SG Jump Event Window: " + window);
				mJumpStepDetector.setJumpEventWindow(window);
			} else if ("SGJumpInterval".equals(key)) {
				int interval = sharedPreferences.getInt("SGJumpInterval", 3000);
				Log.d(TAG, "Set SG Jump Interval: " + interval);
				mJumpStepDetector.setJumpInterval(interval);
			} else if ("SDVibrateOnStep".equals(key)) {
				mVibrateOnStep = sharedPreferences.getBoolean("SDVibrateOnStep", false);
				Log.d(TAG, "Set vibrate on step: " + mVibrateOnStep);
			} else if ("SDVibrateOnJump".equals(key)) {
				mVibrateOnJump = sharedPreferences.getBoolean("SDVibrateOnJump", false);
				Log.d(TAG, "Set vibrate on jump: " + mVibrateOnJump);
			}
		}	
	}
	
    public class ServiceBinder extends Binder {
        public RamblerService getService() {
            return RamblerService.this;
        }    
    }
}

