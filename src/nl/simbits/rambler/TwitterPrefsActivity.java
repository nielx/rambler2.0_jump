package nl.simbits.rambler;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class TwitterPrefsActivity extends Activity {
    public static final String TAG = "TwitterPrefsActivity";

    private TwitterLoginButton mLoginButton;
    private SharedPreferences mPrefs;
    private Handler mHandler;
    private SessionListener mSessionListener = new SessionListener();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.twitter_tab);

        mHandler = new Handler();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        TwitterSessionEvents.addAuthListener(mSessionListener);
        TwitterSessionEvents.addLogoutListener(mSessionListener);
        mLoginButton = (TwitterLoginButton)findViewById(R.id.twitterLoginButton);
        mLoginButton.init(this, TwitterUtilities.AUTHORIZE_ACTIVITY_RESULT_CODE);

        requestScreenName();
    }
 
    public void requestScreenName() {
        mHandler.post(new Runnable() {
            public void run() {
                if (TwitterUtilities.isAuthenticated(mPrefs)) {
                    try {
                        String name = TwitterUtilities.getScreenName(mPrefs);
                        Toast.makeText(getApplicationContext(), 
                                       "Twitter Logged in as " + name, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.d(TAG, "Failed to get screen name: " + e.toString());
                    }
                }
            }
        });
    }
   
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case TwitterUtilities.AUTHORIZE_ACTIVITY_RESULT_CODE: {
                TwitterUtilities.authorizeCallback(requestCode, resultCode, data, mPrefs);
                break;
            }
        }
    }

    private class SessionListener implements TwitterSessionEvents.TwitterAuthListener, 
                                             TwitterSessionEvents.TwitterLogoutListener {
        @Override
        public void onAuthSucceed() {
            requestScreenName();
        }

        public void onAuthFail(String error) { }
        public void onLogoutBegin() { }

        @Override
        public void onLogoutFinish() { 
            Toast.makeText(getApplicationContext(), 
                                   "Twitter logged out", Toast.LENGTH_SHORT).show();
 
        }
    }
}
