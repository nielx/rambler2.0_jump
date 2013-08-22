package nl.simbits.rambler;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import twitter4j.auth.AccessToken;
import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterMethod;

import com.ecs.sample.store.CredentialStore;
import com.ecs.sample.store.SharedPreferencesCredentialStore;

import java.io.InputStream;

public class TwitterUtilities 
{
    public static final String TAG = "TwitterUtilities";

	public static final int AUTHORIZE_ACTIVITY_RESULT_CODE = 00000;  /* Auth result code here */

    public static final String CREDENTIALS_FILE = "tcredentials";
    public static final String CONSUMER_KEY = "conumer_key_here";
	public static final String CONSUMER_SECRET = "consumer_secret_here" ;

	public static final String REQUEST_URL = "http://api.twitter.com/oauth/request_token";
	public static final String ACCESS_URL = "http://api.twitter.com/oauth/access_token";
	public static final String AUTHORIZE_URL = "http://api.twitter.com/oauth/authorize";
	public static final String OAUTH_CALLBACK_URL = "http://rambler.projects.simbits.nl/oauth";

    public static void authorizeCallback(int requestCode, int resultCode, Intent data, SharedPreferences prefs) {
        if (requestCode == AUTHORIZE_ACTIVITY_RESULT_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    CredentialStore credentialStore = new SharedPreferencesCredentialStore(prefs);
				    credentialStore.write(new String[] {data.getStringExtra("token"),
                                                        data.getStringExtra("secret")});
                    Log.d(TAG, "authorizeCallback: " + data.getStringExtra("token") + " | " + data.getStringExtra("secret"));
                    TwitterSessionEvents.onLoginSuccess();
                    break;
                case Activity.RESULT_CANCELED:
                    Log.w(TAG, "authorizeCallback: request failed"); 
                    TwitterSessionEvents.onLoginError("Request failed");
                    break;
            }
        }
    }

    public static boolean isAuthenticated(SharedPreferences prefs) {

		String[] tokens = new SharedPreferencesCredentialStore(prefs).read();
        Log.d(TAG, "Tokens: " + tokens[0] + ", " + tokens[1]);

        AccessToken a;

        try {
            a = new AccessToken(tokens[0],tokens[1]);
        } catch (java.lang.IllegalArgumentException e) {
            Log.d(TAG, "Failed to get AccessToken: " + e.getMessage());
            return false;
        }

		Twitter twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(TwitterUtilities.CONSUMER_KEY, TwitterUtilities.CONSUMER_SECRET);
		twitter.setOAuthAccessToken(a);
		
		try {
			twitter.verifyCredentials();
			return true;
		} catch (TwitterException e) {
			return false;
		}
	}

    public static String getScreenName(SharedPreferences prefs) throws Exception {
        String[] tokens = new SharedPreferencesCredentialStore(prefs).read();
        
        AccessToken a;

        try {
            a = new AccessToken(tokens[0],tokens[1]);
        } catch (java.lang.IllegalArgumentException e) {
            Log.d(TAG, "Failed to get AccessToken: " + e.getMessage());
            throw e;
        }

		Twitter twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(TwitterUtilities.CONSUMER_KEY, TwitterUtilities.CONSUMER_SECRET);
		twitter.setOAuthAccessToken(a);
        return twitter.getScreenName();
    }
	
	public static void sendTweet(SharedPreferences prefs, String msg) throws Exception {
		String[] tokens = new SharedPreferencesCredentialStore(prefs).read();
        
        AccessToken a;

        try {
            a = new AccessToken(tokens[0],tokens[1]);
        } catch (java.lang.IllegalArgumentException e) {
            Log.d(TAG, "Failed to get AccessToken: " + e.getMessage());
            return;
        }

		Twitter twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(TwitterUtilities.CONSUMER_KEY, TwitterUtilities.CONSUMER_SECRET);
		twitter.setOAuthAccessToken(a);
        twitter.updateStatus(msg);
	}	  

    public static void sendTweetAsync(SharedPreferences prefs, String msg, double lat, double lon, String name, InputStream media) throws Exception {
        String[] tokens = new SharedPreferencesCredentialStore(prefs).read();
        
        AccessToken a;

        try {
            a = new AccessToken(tokens[0],tokens[1]);
        } catch (java.lang.IllegalArgumentException e) {
            Log.d(TAG, "Failed to get AccessToken: " + e.getMessage());
            return;
        }

        AsyncTwitterFactory factory = new AsyncTwitterFactory();
        AsyncTwitter twitter = factory.getInstance();

        twitter.addListener(new TwitterAdapter() {
                @Override public void updatedStatus(Status status) {
                    Log.d(TAG, "Successfully updated the status to [" + status.getText() + "].");
                }

                @Override public void onException(TwitterException e, TwitterMethod method) {
                    if (method == TwitterMethod.UPDATE_STATUS) {
                        Log.e(TAG, e.getMessage());
                    } else {
                        Log.d(TAG, "Should not happen");
                    }
               }
        });

        StatusUpdate su = new StatusUpdate(msg);
        
        if (lat != 0 && lon != 0) {
            su.setLocation(new GeoLocation(lat, lon));
        }

        if (media != null) {
            su.setMedia(name, media);
        }

        Log.d(TAG, "sending tweet: " + su.toString());
		twitter.setOAuthConsumer(TwitterUtilities.CONSUMER_KEY, TwitterUtilities.CONSUMER_SECRET);
		twitter.setOAuthAccessToken(a);
        twitter.updateStatus(su);
    }

    public static void sendTweetAsync(SharedPreferences prefs, String msg, double lat, double lon) throws Exception {
        sendTweetAsync(prefs, msg, lat, lon, "", null);
    }

    public static void sendTweetAsync(SharedPreferences prefs, String msg) throws Exception {
        sendTweetAsync(prefs, msg, 0., 0., "", null);
    }

    public static interface TwitterRequestListener {
        public void onComplete(final String response);
    }
}

