package nl.simbits.rambler.social;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ecs.sample.store.SharedPreferencesCredentialStore;

import nl.simbits.rambler.Secrets;
import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;


public class TwitterSession {
    private static TwitterSession sActiveSession = null;
    private final static String TAG = "TwitterSession";

    public enum State {
        OPENED,
        CLOSED
    }

    private State mState = State.CLOSED;
    private String mScreenName;
    private SharedPreferencesCredentialStore mCredentialStore;
    private Twitter mTwitter;

    public static TwitterSession getActiveSession() {
        return sActiveSession;
    }

    public static void setActiveSession(TwitterSession session) {
        sActiveSession = session;
    }

    /**
     * A new Session should only be made outside of the main thread
     * @param context The context of the Service
     */
    public TwitterSession(Context context) {
        // Get the active session
        mCredentialStore = new SharedPreferencesCredentialStore(
                PreferenceManager.getDefaultSharedPreferences(context));
        String[] tokens = mCredentialStore.read();
        Log.d(TAG, "Tokens: " + tokens[0] + ", " + tokens[1]);

        if (tokens[0].equals("") && tokens[1].equals("")) {
            // No tokens stored, so no active session
            mState = State.CLOSED;
            return;
        }

        AccessToken a;

        try {
            a = new AccessToken(tokens[0],tokens[1]);
        } catch (java.lang.IllegalArgumentException e) {
            Log.d(TAG, "Failed to get AccessToken: " + e.getMessage());
            mState = State.CLOSED;
            return;
        }

        mTwitter = new TwitterFactory().getInstance();
        mTwitter.setOAuthConsumer(Secrets.TWITTER_CONSUMER_KEY, Secrets.TWITTER_CONSUMER_SECRET);
        mTwitter.setOAuthAccessToken(a);

        try {
            mTwitter.verifyCredentials();
            mState = State.OPENED;
            mScreenName = mTwitter.getScreenName();
        } catch (TwitterException e) {
            Log.d(TAG, "Twitter Exception: " + e.getMessage());
            mState = State.CLOSED;
        }
    }

    public void logOut() {
        mCredentialStore.clearCredentials();
        mState = State.CLOSED;
        TwitterSession.setActiveSession(null);
    }

    public State getState() {
        return mState;
    }

    public String getScreenName() {
        return mScreenName;
    }

    public Twitter getTwitterInstance() {
        return mTwitter;
    }
}


