package nl.simbits.rambler.holo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.crashlytics.android.Crashlytics;
import com.facebook.Session;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;

import nl.simbits.rambler.BluetoothSPPConnector;
import nl.simbits.rambler.R;
import nl.simbits.rambler.RamblerService;
import nl.simbits.rambler.Secrets;
import nl.simbits.rambler.ServiceTools;
import nl.simbits.rambler.database.EventAdapter;
import nl.simbits.rambler.social.SocialService;

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    public static final int BLUETOOTH_REQUEST_ENABLE = 1;

    // Save the state of the buttons
    final static String KEY_SHOE_CONNECTED = "KEY_SHOE_CONNECTED";
    final static String KEY_BLUETOOTH_TOGGLE_ENABLED = "KEY_BLUETOOTH_TOGGLE_ENABLED";
    final static String KEY_FACEBOOK_AUTHENTICATED = "KEY_FACEBOOK_AUTHENTICATED";
    final static String KEY_FACEBOOK_TOGGLE_ENABLED = "KEY_FACEBOOK_TOGGLE_ENABLED";
    final static String KEY_TWITTER_AUTHENTICATED = "KEY_TWITTER_AUTHENTICATED";
    final static String KEY_TWITTER_TOGGLE_ENABLED = "KEY_TWITTER_TOGGLE_ENABLED";

    // Actionbar
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    // Facebook
    private Boolean mFacebookAuthenticated = false;
    private TextView mFacebookText;
    private ToggleButton mFacebookToggle;

    // Twitter
    private Boolean mTwitterAuthenticated = false;
    private TextView mTwitterText;
    private ToggleButton mTwitterToggle;

    // Bluetooth UI
    private TextView mBluetoothText;
    private ToggleButton mBluetoothToggle;
    private BluetoothAdapter mBluetoothAdapter;

    // Bluetooth Broadcast
    private boolean mShoeConnected = false;
    private BroadcastReceiver mBroadcastReceiver;

    // RamblerService
    private boolean mServiceBound;
    private RamblerService mService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((RamblerService.ServiceBinder)service).getService();
            mServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mServiceBound = false;
            mBluetoothToggle.setEnabled(false);
        }
    };


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        if (savedInstanceState != null)
            Log.d(TAG, savedInstanceState.toString());
        else
            Log.d(TAG, "No saved instance state");
        setContentView(R.layout.activity_main);

        // Enable the action bar app icon to toggle the settings
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // Set up drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Facebook
        mFacebookText = (TextView)findViewById(R.id.facebook_message);
        mFacebookToggle = (ToggleButton)findViewById(R.id.facebook_togglebutton);

        if (savedInstanceState != null) {
            mFacebookAuthenticated = savedInstanceState.getBoolean(KEY_FACEBOOK_AUTHENTICATED,
                    false);
            mFacebookToggle.setEnabled(savedInstanceState.getBoolean(KEY_FACEBOOK_TOGGLE_ENABLED,
                    false));
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mFacebookStatusReceiver, new IntentFilter(SocialService.FACEBOOK_STATUS));

        Intent facebookStatusIntent = new Intent(this, SocialService.class);
        facebookStatusIntent.setAction(SocialService.FACEBOOK_QUERY_STATUS);
        startService(facebookStatusIntent);

        mFacebookToggle.setOnClickListener(mFacebookLoginButtonClick);

        // Twitter
        mTwitterText = (TextView)findViewById(R.id.twitter_message);
        mTwitterToggle = (ToggleButton)findViewById(R.id.twitter_togglebutton);

        if (savedInstanceState != null) {
            mTwitterAuthenticated = savedInstanceState.getBoolean(KEY_TWITTER_AUTHENTICATED, false);
            mTwitterToggle.setEnabled(savedInstanceState.getBoolean(KEY_TWITTER_AUTHENTICATED,
                    false));
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mTwitterStatusReceiver, new IntentFilter(SocialService.TWITTER_STATUS));

        Intent twitterStatusIntent = new Intent(this, SocialService.class);
        twitterStatusIntent.setAction(SocialService.TWITTER_QUERY_STATUS);
        startService(twitterStatusIntent);

        mTwitterToggle.setOnClickListener(mTwitterLoginButtonClick);

        // Bluetooth UI
        mBluetoothText = (TextView)findViewById(R.id.bluetooth_message);
        mBluetoothToggle = (ToggleButton)findViewById(R.id.bluetooth_togglebutton);
        mBluetoothToggle.setOnClickListener(mBluetoothButtonClick);

        if (savedInstanceState != null) {
            mShoeConnected = savedInstanceState.getBoolean(KEY_SHOE_CONNECTED, false);
            mBluetoothToggle.setEnabled(savedInstanceState.getBoolean(KEY_BLUETOOTH_TOGGLE_ENABLED,
                    false));
        }

        // Bluetooth wiring
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            mBluetoothText.setText("Bluetooth is not available");
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

        mBroadcastReceiver = new BluetoothConnectionReceiver();

        IntentFilter filter = new IntentFilter(BluetoothSPPConnector.BROADCAST_INTENT_BLUETOOTH);
        registerReceiver(mBroadcastReceiver, filter);


        // Hook up Settings
        Button settingsButton = (Button)findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsFragment fragment = new SettingsFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.main_content, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
                mDrawerLayout.closeDrawers();
            }
        });

        // Set up initial fragment
        ListFragment fragment = new ListFragment();
        fragment.setListAdapter(EventAdapter.getInstance());
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, fragment);
        transaction.commit();
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent intent = new Intent(this, RamblerService.class);
        if (ServiceTools.isServiceRunning(this) == false){
            Log.d(TAG,"-->service will be started.");
            startService(intent);
        } else {
            Log.d(TAG,"-->service is running.");
        }
        getApplicationContext().bindService(intent, mServiceConnection, 0);
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;

        if (item.getItemId() == R.id.item_exit) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Stop Rambler");
            builder.setMessage("Are you sure you want disconnect form the shoe and stop listening for steps?");
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            builder.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    stopService(new Intent(MainActivity.this, RamblerService.class));
                    finish();
                }
            });
            builder.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.syncState();
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
                mTwitterToggle.setEnabled(false);
                mTwitterToggle.setChecked(true);
                mTwitterText.setText("Logging in...");

                Intent twitterStatusIntent = new Intent(this, SocialService.class);
                twitterStatusIntent.setAction(SocialService.TWITTER_QUERY_STATUS);
                startService(twitterStatusIntent);
                break;
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_BLUETOOTH_TOGGLE_ENABLED, mBluetoothToggle.isEnabled());
        outState.putBoolean(KEY_FACEBOOK_AUTHENTICATED, mFacebookAuthenticated);
        outState.putBoolean(KEY_FACEBOOK_TOGGLE_ENABLED, mFacebookToggle.isEnabled());
        outState.putBoolean(KEY_TWITTER_AUTHENTICATED, mTwitterAuthenticated);
        outState.putBoolean(KEY_TWITTER_TOGGLE_ENABLED, mTwitterToggle.isEnabled());
        outState.putBoolean(KEY_SHOE_CONNECTED, mShoeConnected);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver);
        if (mServiceBound) {
            getApplicationContext().unbindService(mServiceConnection);
            mServiceBound = false;
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mFacebookStatusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTwitterStatusReceiver);
    }

    /* Facebook methods */
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
            } else if (intent.getBooleanExtra("authenticated", false)) {
                Log.d(TAG, "Facebook Status: authenticated");
                mFacebookText.setText(intent.getStringExtra("name"));
                mFacebookToggle.setChecked(true);
                mFacebookToggle.setEnabled(true);
                mFacebookAuthenticated = true;
            } else {
                Log.d(TAG, "Facebook Status: not authenticated");
                mFacebookText.setText("Not connected");
                mFacebookToggle.setChecked(false);
                mFacebookToggle.setEnabled(true);
                mFacebookAuthenticated = false;
            }
        }
    };

    private View.OnClickListener mFacebookLoginButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mFacebookAuthenticated) {
                Intent cancelFacebookIntent = new Intent(MainActivity.this, SocialService.class);
                cancelFacebookIntent.setAction(SocialService.FACEBOOK_LOGOUT);
                startService(cancelFacebookIntent);
                mFacebookToggle.setEnabled(false);
                mFacebookText.setText("Logging out...");
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
                new Session.OpenRequest(MainActivity.this)
                        .setPermissions(SocialService.FACEBOOK_READ_PERMISSIONS)
                        .setLoginBehavior(SessionLoginBehavior.SUPPRESS_SSO)
                        .setCallback(new Session.StatusCallback() {
                            @Override
                            public void call(Session session, SessionState state, Exception exception) {
                                if (state.equals(SessionState.CLOSED_LOGIN_FAILED)) {
                                    // Failed or cancelled login, replace the session with
                                    // a new one
                                    Session.setActiveSession(new Session(MainActivity.this));
                                    return;
                                }

                                // Call the service to further handle the session changes
                                Intent facebookStatusIntent = new Intent(MainActivity.this,
                                        SocialService.class);
                                facebookStatusIntent.setAction(SocialService.FACEBOOK_QUERY_STATUS);
                                startService(facebookStatusIntent);
                            }
                        }));

    }

    /* Twitter methods */
    private BroadcastReceiver mTwitterStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received new Twitter status");

            if (intent.getBooleanExtra("authenticated", false)) {
                Log.d(TAG, "Twitter Status: authenticated");
                mTwitterText.setText("Connected as " + intent.getStringExtra("name"));
                mTwitterToggle.setEnabled(true);
                mTwitterToggle.setChecked(true);
                mTwitterAuthenticated = true;
            } else {
                Log.d(TAG, "Twitter Status: not authenticated");
                mTwitterText.setText("Not connected");
                mTwitterToggle.setEnabled(true);
                mTwitterToggle.setChecked(false);
                mTwitterAuthenticated = false;
            }
        }
    };

    private View.OnClickListener mTwitterLoginButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mTwitterAuthenticated) {
                mTwitterToggle.setEnabled(false);
                mTwitterText.setText("Logging out...");

                // Use the SocialService to Log Out
                Intent cancelTwitterIntent = new Intent(MainActivity.this, SocialService.class);
                cancelTwitterIntent.setAction(SocialService.TWITTER_LOGOUT);
                startService(cancelTwitterIntent);

            } else {
                startActivityForResult(new Intent().setClass(MainActivity.this,
                        TwitterOAuthActivity.class),
                        Secrets.TWITTER_AUTHORIZE_ACTIVITY_RESULT_CODE);
            }
        }
    };

    /* Bluetooth methods */
    private class BluetoothConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothSPPConnector.BROADCAST_INTENT_BLUETOOTH)) {
                Bundle extras = intent.getExtras();

                int state = extras.getInt("state");
                String stateText = extras.getString("state_text");
                String name = extras.getString("name");
                String address = extras.getString("address");

                Log.i(TAG, "Received broadcast: " + intent.toString());
                Log.i(TAG, "extras: state: " + state + " (" +
                        BluetoothSPPConnector.stateToString(state) + "), address: " + address);

                switch (state) {
                    case BluetoothSPPConnector.BT_CONNECTED: {
                        mBluetoothText.setText("Connected to " + name);
                        mBluetoothToggle.setChecked(true);
                        mBluetoothToggle.setEnabled(true);
                        mShoeConnected = true;
                        break;
                    }
                    case BluetoothSPPConnector.BT_NOT_ENABLED: {
                        mBluetoothText.setText("Bluetooth not enabled");
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent, BLUETOOTH_REQUEST_ENABLE);
                        break;
                    }
                    case BluetoothSPPConnector.BT_NOT_CONNECTED: {
                        mBluetoothText.setText("Not connected");
                        mBluetoothToggle.setEnabled(true);
                        mBluetoothToggle.setChecked(false);
                        mShoeConnected = false;
                        break;
                    }
                    default: {
                        mBluetoothText.setText("Unable to connect with " + name);
                        mBluetoothToggle.setChecked(false);
                        mBluetoothToggle.setEnabled(true);
                        mShoeConnected = false;
                    }
                }
            }
        }
    }

    private View.OnClickListener mBluetoothButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mServiceBound && mService != null) {
                if (!mShoeConnected) {
                    Log.d(TAG, "start bluetooth connection");
                    mBluetoothText.setText("Connecting...");
                    mBluetoothToggle.setEnabled(false);
                    mService.startBluetoothConnection();
                } else {
                    Log.d(TAG, "stop bluetooth connection");
                    mBluetoothText.setText("Not connected");
                    mBluetoothToggle.setChecked(false);
                    mBluetoothToggle.setEnabled(true);
                    mShoeConnected = false;
                    mService.stopBluetoothConnection();
                }
            }
        }
    };

}