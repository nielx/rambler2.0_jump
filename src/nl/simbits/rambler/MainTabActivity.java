package nl.simbits.rambler;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.facebook.Session;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;

import nl.simbits.rambler.holo.TwitterOAuthActivity;
import nl.simbits.rambler.social.SocialService;

public class MainTabActivity extends Activity
{
    public static final String TAG = "MainTabActivity";
    public static final int BLUETOOTH_REQUEST_ENABLE = 1;
    
    private RamblerApplication mRambler;
    private BroadcastReceiver mBroadcastReceiver;

    private Boolean mFbAuthenticated = false;
    private TextView mFbMessages;
    private ProgressBar mFbProgress;
    private Button mFbLoginButton;

    private Boolean mTwAuthenticated = false;
    private TextView mTwMessages;
    private ProgressBar mTwProgress;
    private Button mTwLoginButton;

    private BluetoothAdapter mBtAdapter;
    private TextView mBtMessages;
    private Button mBtConnectButton;
    private ProgressBar mBtProgress;
    private boolean mShoeConnected;

    private boolean mServiceBound;
    private RamblerService mService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((RamblerService.ServiceBinder)service).getService();
            mServiceBound = true;
            mBtConnectButton.setEnabled(true);
        }

        public void onServiceDisconnected(ComponentName className) {
        	mService = null;
        	mServiceBound = false;
            mBtConnectButton.setEnabled(false);
        }
    };
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Crashlytics.start(this);

        setContentView(R.layout.main_tab_layout);

        mRambler = (RamblerApplication) getApplication();
        mBroadcastReceiver = new BluetoothConnectionReceiver();
        
        IntentFilter filter = new IntentFilter(BluetoothSPPConnector.BROADCAST_INTENT_BLUETOOTH);
        registerReceiver(mBroadcastReceiver, filter);

        /**
         * Facebook session
         */
        mFbMessages = (TextView)findViewById(R.id.facebook_message);
        mFbProgress = (ProgressBar)findViewById(R.id.facebook_progress);
        mFbLoginButton = (Button)findViewById(R.id.button_facebook_connect);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mFacebookStatusReceiver, new IntentFilter(SocialService.FACEBOOK_STATUS));

        Intent facebookStatusIntent = new Intent(this, SocialService.class);
        facebookStatusIntent.setAction(SocialService.FACEBOOK_QUERY_STATUS);
        startService(facebookStatusIntent);

        mFbLoginButton.setOnClickListener(mFacebookLoginButtonClick);

        /**
         * Twitter session
         */
        mTwMessages = (TextView)findViewById(R.id.twitter_message);
        mTwProgress = (ProgressBar)findViewById(R.id.twitter_progress);
        mTwLoginButton = (Button)findViewById(R.id.button_twitter_connect);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mTwitterStatusReceiver, new IntentFilter(SocialService.TWITTER_STATUS));

        Intent twitterStatusIntent = new Intent(this, SocialService.class);
        twitterStatusIntent.setAction(SocialService.TWITTER_QUERY_STATUS);
        startService(twitterStatusIntent);

        mTwLoginButton.setOnClickListener(mTwitterLoginButtonClick);

        /**
         * Bluetooth
         */
        mBtMessages = (TextView)findViewById(R.id.bluetooth_message);
        mBtConnectButton = (Button)findViewById(R.id.button_bluetooth_connect);
        mBtProgress = (ProgressBar)findViewById(R.id.bluetooth_progress);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        
        mBtConnectButton.setOnClickListener(new ButtonOnClickListener());
        mBtConnectButton.setEnabled(false);

        if (mBtAdapter == null) {
        	mBtMessages.setText("Bluetooth not available");
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            mBtProgress.setVisibility(View.GONE);
            mBtConnectButton.setVisibility(View.VISIBLE);
            mBtConnectButton.setEnabled(false);
        }
    }

    @Override
    public void onStart() {
    	super.onStart();
    	
    	Log.d(TAG, "onStart()");
    	
    	Intent intent = new Intent(this, RamblerService.class);
    	
        if (ServiceTools.isServiceRunning(this) == false){
            Log.d(TAG,"-->service will be started.");
            startService(intent);
        } else {
            Log.d(TAG,"-->service is running.");
        }
    	getApplicationContext().bindService(intent, mServiceConnection, 0);
    }
    
    public void connectToShoe() {
		mBtMessages.setText("Trying to connect");
        mBtProgress.setVisibility(View.VISIBLE);
        mBtConnectButton.setVisibility(View.GONE);
		mService.startBluetoothConnection();
    }
    
    public void disconnectShoe() {
		mService.stopBluetoothConnection();
		mBtConnectButton.setText("Connect");
		mShoeConnected = false;
		mBtMessages.setText("Disconnected");
    }
    
    private final class ButtonOnClickListener implements OnClickListener {
    	/*
		 * Source Tag: login_tag
		 */
        public void onClick(View arg0) {
        	//ImageButton b = (ImageButton)arg0;
    		if (mServiceBound && mService != null) {
    			if (!mShoeConnected) {
            		Log.d(TAG, "onClick: start bluetooth connection");
            		connectToShoe();
        		} else {
        			Log.d(TAG, "onClick: stopping bluetooth connection");
        			disconnectShoe();
        		}
        	}
        }
    }

    public class BluetoothConnectionReceiver extends BroadcastReceiver {
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		if (intent.getAction().equals(BluetoothSPPConnector.BROADCAST_INTENT_BLUETOOTH)) {
    			Bundle extras = intent.getExtras();
    			
    			int state = extras.getInt("state");
    			String stateText = extras.getString("state_text");
    			String name = extras.getString("name");
    			String address = extras.getString("address");
    			
    			Log.i(TAG, "Received broadcast: " + intent.toString());
    			Log.i(TAG, "extras: state: " + state + " (" + BluetoothSPPConnector.stateToString(state) + "), address: " + address);
    			
    			switch (state) {
    				case BluetoothSPPConnector.BT_CONNECTED: {
    					mBtMessages.setText("Connected to " + name + " (" + address + ")");
    					mBtConnectButton.setText("Disconnect");
    					mShoeConnected = true;
    					break;
    				}
    				case BluetoothSPPConnector.BT_NOT_ENABLED: {
    					mBtMessages.setText("Bluetooth not enabled");
    					Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    			        startActivityForResult(enableIntent, BLUETOOTH_REQUEST_ENABLE);
    					break;
    				}
    				default: {
    					mBtMessages.setText("Unable to connect with " + name + " (" + address + "): " + 
    										((stateText.equals("")) ? "" : ": " + stateText));
    					mBtConnectButton.setText("Connect");
    					mShoeConnected = false;
    				}
    			}

                mBtProgress.setVisibility(View.GONE);
                mBtConnectButton.setVisibility(View.VISIBLE);
    		}
    	}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass through to Facebook
        Session fb_session = Session.getActiveSession();
        if (fb_session != null)
            fb_session.onActivityResult(this, requestCode, resultCode, data);

        switch(requestCode) {
            case Secrets.TWITTER_AUTHORIZE_ACTIVITY_RESULT_CODE: {
                mTwProgress.setVisibility(View.VISIBLE);
                mTwLoginButton.setVisibility(View.GONE);
                mTwMessages.setText("Logging in...");

                Intent twitterStatusIntent = new Intent(this, SocialService.class);
                twitterStatusIntent.setAction(SocialService.TWITTER_QUERY_STATUS);
                startService(twitterStatusIntent);
                break;
            }
            
            case BLUETOOTH_REQUEST_ENABLE: {
            	if (resultCode == RESULT_OK) {
            		connectToShoe();
            	}
            	break;
            }
        }
    }


    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(TAG, "onPause");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
        Log.d(TAG, "onResume");

    	if (mServiceBound) {
    		mService.broadcastBluetoothState();
    	}
    }   

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        
    	unregisterReceiver(mBroadcastReceiver);
        if (mServiceBound) {
            getApplicationContext().unbindService(mServiceConnection);
            mServiceBound = false;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mFacebookStatusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTwitterStatusReceiver);
    }

    private BroadcastReceiver mFacebookStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received new Facebook status");
            if (intent.getBooleanExtra("has_token", false) &&
                    !intent.getBooleanExtra("authenticated", false))
            {
                Log.d(TAG, "Facebook Status: not authenticated but token present");
                // Start session
                _FacebookOpenSession();
                return; // We do not want the progress bar gone and the login button visible in this case
            } else if (intent.getBooleanExtra("authenticated", false)) {
                Log.d(TAG, "Facebook Status: authenticated");
                mFbMessages.setText("Connected as " + intent.getStringExtra("name"));
                mFbLoginButton.setText("Logout");
                mFbAuthenticated = true;
            } else {
                Log.d(TAG, "Facebook Status: not authenticated");
                mFbMessages.setText("Not connected");
                mFbLoginButton.setText("Connect");
                mFbAuthenticated = false;
            }

            // Whatever changed, this should be the standard now
            mFbProgress.setVisibility(View.GONE);
            mFbLoginButton.setVisibility(View.VISIBLE);

        }
    };

    private OnClickListener mFacebookLoginButtonClick = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mFbAuthenticated) {
                Intent cancelFacebookIntent = new Intent(MainTabActivity.this, SocialService.class);
                cancelFacebookIntent.setAction(SocialService.FACEBOOK_LOGOUT);
                startService(cancelFacebookIntent);
                mFbProgress.setVisibility(View.VISIBLE);
                mFbLoginButton.setVisibility(View.GONE);
                mFbMessages.setText("Logging out...");
            } else {
                // Sessions are started in a GUI setting, so we cannot delegate this to the
                // SocialService
                if (Session.getActiveSession() == null ) {
                    // This should never happen!
                    Log.e(TAG, "No active facebook session. This should never happen!");
                    return;
                }
                _FacebookOpenSession();
            }
        }
    };

    private void _FacebookOpenSession() {
        Session session = Session.getActiveSession();
        session.openForRead(
                new Session.OpenRequest(MainTabActivity.this)
                        .setPermissions(SocialService.FACEBOOK_READ_PERMISSIONS)
                        .setLoginBehavior(SessionLoginBehavior.SUPPRESS_SSO)
                        .setCallback(new Session.StatusCallback() {
                            @Override
                            public void call(Session session, SessionState state, Exception exception) {
                                if (state.equals(SessionState.CLOSED_LOGIN_FAILED)) {
                                    // Failed or cancelled login, replace the session with
                                    // a new one
                                    Session.setActiveSession(new Session(MainTabActivity.this));
                                    return;
                                }

                                // Call the service to further handle the session changes
                                Intent facebookStatusIntent = new Intent(MainTabActivity.this, SocialService.class);
                                facebookStatusIntent.setAction(SocialService.FACEBOOK_QUERY_STATUS);
                                startService(facebookStatusIntent);
                            }
                        }));

    }

    private BroadcastReceiver mTwitterStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received new Twitter status");

            if (intent.getBooleanExtra("authenticated", false)) {
                Log.d(TAG, "Twitter Status: authenticated");
                mTwMessages.setText("Connected as " + intent.getStringExtra("name"));
                mTwLoginButton.setText("Logout");
                mTwAuthenticated = true;
            } else {
                Log.d(TAG, "Twitter Status: not authenticated");
                mTwMessages.setText("Not connected");
                mTwLoginButton.setText("Connect");
                mTwAuthenticated = false;
            }

            // Whatever changed, this should be the standard now
            mTwProgress.setVisibility(View.GONE);
            mTwLoginButton.setVisibility(View.VISIBLE);

        }
    };

    private OnClickListener mTwitterLoginButtonClick = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mTwAuthenticated) {
                mTwProgress.setVisibility(View.VISIBLE);
                mTwLoginButton.setVisibility(View.GONE);
                mTwMessages.setText("Logging out...");

                // Use the SocialService to Log Out
                Intent cancelTwitterIntent = new Intent(MainTabActivity.this, SocialService.class);
                cancelTwitterIntent.setAction(SocialService.TWITTER_LOGOUT);
                startService(cancelTwitterIntent);

            } else {
                startActivityForResult(new Intent().setClass(MainTabActivity.this,
                        TwitterOAuthActivity.class),
                        Secrets.TWITTER_AUTHORIZE_ACTIVITY_RESULT_CODE);
            }
        }
    };


}
