package nl.simbits.rambler.social;

import android.content.SharedPreferences;
import android.util.Log;

import nl.simbits.rambler.social.TwitterSession;
import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
import twitter4j.TwitterMethod;

import java.io.InputStream;

public class TwitterUtilities 
{
    public static final String TAG = "TwitterUtilities";

	public static final String REQUEST_URL = "http://api.twitter.com/oauth/request_token";
	public static final String ACCESS_URL = "http://api.twitter.com/oauth/access_token";
	public static final String AUTHORIZE_URL = "http://api.twitter.com/oauth/authorize";
	public static final String OAUTH_CALLBACK_URL = "http://rambler.projects.simbits.nl/oauth";


	public static void sendTweet(String msg) throws Exception {
        TwitterSession session = TwitterSession.getActiveSession();
        if (session == null || session.getState().equals(TwitterSession.State.CLOSED)) {
            Log.d(TAG, "sendTweet failed: not logged in");
            return;
        }

		Twitter twitter = session.getTwitterInstance();
        twitter.updateStatus(msg);
	}	  

    public static void sendTweetAsync(String msg, double lat, double lon, String name, InputStream media) throws Exception {
        TwitterSession session = TwitterSession.getActiveSession();
        if (session == null || session.getState().equals(TwitterSession.State.CLOSED)) {
            Log.d(TAG, "sendTweet failed: not logged in");
            return;
        }

        AsyncTwitterFactory factory = new AsyncTwitterFactory();
        AsyncTwitter twitter = factory.getInstance(session.getTwitterInstance());

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
        twitter.updateStatus(su);
    }

    public static void sendTweetAsync(String msg, double lat, double lon) throws Exception {
        sendTweetAsync(msg, lat, lon, "", null);
    }

    public static void sendTweetAsync(String msg) throws Exception {
        sendTweetAsync(msg, 0., 0., "", null);
    }
}

