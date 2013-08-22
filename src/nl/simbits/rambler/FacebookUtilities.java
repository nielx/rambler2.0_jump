package nl.simbits.rambler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.BaseRequestListener;
import com.facebook.android.Facebook;

public class FacebookUtilities extends AsyncFacebookRunner
{

    public static final String TAG = "FacebookUtilities";
	public static final int AUTHORIZE_ACTIVITY_RESULT_CODE = 12345; /* Auth result code here */

    private static final String SP_TOKEN = "access_token";
    private static final String SP_EXPIRES = "expires_in";
    private static final String SP_KEY = "facebook-session";

    public static final String APP_ID = "app_id_here";
    public static final String[] permissions = {"offline_access", "publish_stream", "read_stream",
                                                "user_photos", "user_likes", "user_events", "photo_upload"};

    public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm";
    public static SimpleDateFormat dateFormat = new SimpleDateFormat(ISO8601_FORMAT);

    private FacebookEventsManager mEventsManager;

    private static FacebookUtilities instance; 
    private static Facebook fb;

    private FacebookUtilities(Facebook fb) 
    {
        super(fb);
        mEventsManager = new FacebookEventsManager();
    }

    public static synchronized FacebookUtilities getInstance()
    {
        if (instance == null) {
            fb = new Facebook(APP_ID);
            instance = new FacebookUtilities(fb);
        }
        return instance;
    }

    public Facebook getSession()
    {
        return FacebookUtilities.fb;
    }

    public synchronized boolean saveSession(Context context) {
        Editor editor = context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(SP_TOKEN, FacebookUtilities.fb.getAccessToken());
        editor.putLong(SP_EXPIRES, FacebookUtilities.fb.getAccessExpires());
        return editor.commit();
    }

    /*
     * Restore the access token and the expiry date from the shared preferences.
     */
    public synchronized boolean restoreSession(Context context) {
        SharedPreferences savedSession =
            context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE);
        FacebookUtilities.fb.setAccessToken(savedSession.getString(SP_TOKEN, null));
        FacebookUtilities.fb.setAccessExpires(savedSession.getLong(SP_EXPIRES, 0));
        return FacebookUtilities.fb.isSessionValid();
    }

    public synchronized void clearSession(Context context) {
        Editor editor = context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
    }

    public void post(String message) 
    {
        this.post(message, false);
    }

    public void post(String message, boolean like) 
    {
        Bundle parameters = new Bundle();

        try {
            FbRequestTraits traits = new FbRequestTraits();
            traits.like = like;

            parameters.putString("message", message);
            parameters.putString("description", "rambler post");
            request("me/feed", parameters, "POST", new PostRequestListener(), traits);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void postPhoto(String caption, String url, boolean like) 
    {
        Bundle parameters = new Bundle();

        try {
            FbRequestTraits traits = new FbRequestTraits();
            traits.like = like;
 
            parameters.putString("message", caption);
            parameters.putString("url", url); 
            parameters.putString("caption", caption);
            request("me/photos", parameters, "POST", new PostRequestListener(), traits);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private class PostRequestListener extends BaseRequestListener
    {
        @Override
        public void onComplete(String response, Object state)
        {
            FacebookUtilities fb = FacebookUtilities.getInstance();
            FbRequestTraits traits;
            traits = (state == null) ?  new FbRequestTraits() : (FbRequestTraits)state;

            Log.d(TAG, "Post completed with response: " + response);

            if (traits.like) {
                try {
                    fb.like(new JSONObject(response).getString("id"));
                } catch (Throwable e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void like(String id)
    {
        FacebookUtilities fb = FacebookUtilities.getInstance();

        if (id == null || id.equals(""))
            return;

        try {
            fb.request(id + "/likes", new Bundle(), "POST", new LikeRequestListener(), null);
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private class LikeRequestListener extends BaseRequestListener
    {
        @Override
        public void onComplete(String response, Object state) 
        { 
            Log.d(TAG, "Like completed with response: " + response);
        }
    }

    public void updateEvents()
    {
        FacebookUtilities fb = FacebookUtilities.getInstance();

        mEventsManager.clearAttendingEvents();
        mEventsManager.cleanAttendedEvents();

        try {
            fb.request("me/events", new Bundle(), new EventsRequestListener());
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private class EventsRequestListener extends BaseRequestListener
    {
        @Override
        public void onComplete(String response, Object state) 
        { 
            FacebookUtilities.getInstance();
            Date now = new Date();

            Log.d(TAG, "Events completed with response: " + response);
            try {
                JSONArray events = new JSONObject(response).getJSONArray("data");
                for (int i=0; i<events.length(); i++) {
                    JSONObject jso = events.getJSONObject(i);
                    FacebookEvent event;

                    try {
                        event = new FacebookEvent(jso);
                    } catch (org.json.JSONException e) {
                        Log.w(TAG, "Could not create event: " + e.getMessage());
                        continue;
                    }

                    if (event.getEndDate().after(now) && event.isAttending()) {
                        FacebookUtilities.getInstance().addEvent(event.getGraphId());
                    }

                    Log.d(TAG, event.getGraphId() + " - " + event.getName());
                }
            } catch (Exception e) {
                //Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void attendEvent(FacebookEvent event)
    {
        mEventsManager.attendEvent(event);
    }

    public List<FacebookEvent> getEventsInVicinityOf(Location location, float distance)
    {
        return mEventsManager.getEventsInVicinityOf(location, distance);
    }


    private void addEvent(String id)
    {
        FacebookUtilities fb = FacebookUtilities.getInstance();

        try {
            fb.request(id, new Bundle(), new AddEventRequestListener());
        } catch (Throwable e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private class AddEventRequestListener extends BaseRequestListener
    {
        @Override
        public void onComplete(String response, Object state) 
        { 
            FacebookUtilities.getInstance();

            Log.d(TAG, "Event completed with response: " + response);
            try {
                FacebookEvent event;

                try {
                    event = new FacebookEvent(new JSONObject(response));

                    /* only attending events are requested for adding */
                    event.setAttending(true);

                    if (event.isSecret()) {
                        Log.d(TAG, "For privacy reasons we do not add secret events to the events manager");
                        return;
                    }

                    if (!event.hasLocation()) {
                        Log.d(TAG, "No need to add events without location");
                        return;
                    }

                    mEventsManager.add(event);
                } catch (org.json.JSONException e) {
                    Log.w(TAG, "Could not create event: " + e.getMessage());
                }
            } catch (Exception e) {
                //Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private class FbRequestTraits 
    {
        public boolean like;
    }
}
