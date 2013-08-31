package nl.simbits.rambler.holo;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.crashlytics.android.Crashlytics;
import com.facebook.Session;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;

import nl.simbits.rambler.R;
import nl.simbits.rambler.Secrets;
import nl.simbits.rambler.social.SocialService;

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private Boolean mFacebookAuthenticated = false;
    private TextView mFacebookText;
    private ToggleButton mFacebookToggle;

    private Boolean mTwitterAuthenticated = false;
    private TextView mTwitterText;
    private ToggleButton mTwitterToggle;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

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

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mFacebookStatusReceiver, new IntentFilter(SocialService.FACEBOOK_STATUS));

        Intent facebookStatusIntent = new Intent(this, SocialService.class);
        facebookStatusIntent.setAction(SocialService.FACEBOOK_QUERY_STATUS);
        startService(facebookStatusIntent);

        mFacebookToggle.setOnClickListener(mFacebookLoginButtonClick);

        // Twitter
        mTwitterText = (TextView)findViewById(R.id.twitter_message);
        mTwitterToggle = (ToggleButton)findViewById(R.id.twitter_togglebutton);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mTwitterStatusReceiver, new IntentFilter(SocialService.TWITTER_STATUS));

        Intent twitterStatusIntent = new Intent(this, SocialService.class);
        twitterStatusIntent.setAction(SocialService.TWITTER_QUERY_STATUS);
        startService(twitterStatusIntent);

        mTwitterToggle.setOnClickListener(mTwitterLoginButtonClick);

        // Hook up Settings
        Button settingsButton = (Button)findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsFragment fragment = new SettingsFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.add(R.id.main_content, fragment);
                transaction.commit();
                mDrawerLayout.closeDrawers();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
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
    protected void onDestroy() {
        super.onDestroy();
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
                                Intent facebookStatusIntent = new Intent(MainActivity.this, SocialService.class);
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


}