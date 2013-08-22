package nl.simbits.rambler;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.ecs.sample.store.SharedPreferencesCredentialStore;

public class TwitterLoginButton extends ImageButton {
    private Activity mActivity;
    private int mActivityCode;
    private SharedPreferences mPrefs;
    private SessionListener mSessionListener = new SessionListener();

    public TwitterLoginButton(Context context) {
        super(context);
    }

    public TwitterLoginButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TwitterLoginButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(final Activity activity, final int activityCode)
    {
        mActivity = activity;
        mActivityCode = activityCode;
        new Handler();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

        setBackgroundColor(Color.TRANSPARENT);
        setImageResource(TwitterUtilities.isAuthenticated(mPrefs) ?
                         R.drawable.twitter_logout_button : 
                         R.drawable.twitter_login_button);
        drawableStateChanged();

        TwitterSessionEvents.addAuthListener(mSessionListener);
        TwitterSessionEvents.addLogoutListener(mSessionListener);
        setOnClickListener(new ButtonOnClickListener());
    }
    
    private final class ButtonOnClickListener implements OnClickListener {
    	/*
		 * Source Tag: login_tag
		 */
        public void onClick(View arg0) {
            if (TwitterUtilities.isAuthenticated(mPrefs)) {
                TwitterSessionEvents.onLogoutBegin();
                new SharedPreferencesCredentialStore(mPrefs).clearCredentials();
                TwitterSessionEvents.onLogoutFinish();
            } else {
                mActivity.startActivityForResult(new Intent().
                        setClass(mActivity, 
                                 TwitterOAuthActivity.class), 
                        mActivityCode);
            }
        }
    }

    private class SessionListener implements TwitterSessionEvents.TwitterAuthListener, 
                                             TwitterSessionEvents.TwitterLogoutListener {
        
        public void onAuthSucceed() {
            setImageResource(R.drawable.twitter_logout_button);
        }

        public void onAuthFail(String error) { }
        public void onLogoutBegin() { }
        
        public void onLogoutFinish() {
            setImageResource(R.drawable.twitter_login_button);
        }
    }
}

