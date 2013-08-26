package nl.simbits.rambler.social;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphObjectList;

/**
 * Provide tools for other classes (RamblerService) to execute Facebook commands.
 *  - The session management of the Facebook SDK handles the storage and retrieval of the session
 *  - The SocialService is used by the Main Activity to get and create the Facebook session
 */
public class FacebookUtilities
{

    public static final String TAG = "FacebookUtilities";

    public static final String[] permissions = {"offline_access", "publish_stream", "read_stream",
                                                "user_photos", "user_likes", "user_events", "photo_upload"};

    public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm";
    public static SimpleDateFormat dateFormat = new SimpleDateFormat(ISO8601_FORMAT);

    private FacebookEventsManager mEventsManager;

    private static FacebookUtilities instance; 


    private FacebookUtilities()
    {
        super();
        mEventsManager = new FacebookEventsManager();
    }


    public static synchronized FacebookUtilities getInstance()
    {
        if (instance == null) {
            instance = new FacebookUtilities();
        }
        return instance;
    }


    public void post(String message) 
    {
        this.post(message, false);
    }


    public void post(final String message, final boolean like)
    {
        Session session = Session.getActiveSession();
        if (session == null || session.getState() != SessionState.OPENED) {
            Log.i(TAG, "FacebookUtilities.post() cancelled because there is no active Facebook session");
            return;
        }

        Request.newStatusUpdateRequest(session, message, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                if (response.getError() != null) {
                    Log.e(TAG, "There was an error posting the message " + message);
                    return;
                }

                String id = (String) response.getGraphObject().getProperty("id");

                if (like) {
                    like(id);
                }
            }
        }).executeAsync();
    }


    public void postPhoto(final String caption, String url, final boolean like)
    {
        Session session = Session.getActiveSession();
        if (session == null || session.getState() != SessionState.OPENED) {
            Log.i(TAG, "FacebookUtilities.like() cancelled because there is no active Facebook session");
            return;
        }

        Bundle parameters = new Bundle();
        parameters.putString("message", caption);
        parameters.putString("url", url);
        parameters.putString("caption", caption);

        new Request(Session.getActiveSession(), "me/photos",
                parameters, HttpMethod.POST, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                Log.i(TAG, "Post photo completed with response: " + response.toString());
                if (response.getError() != null) {
                    Log.e(TAG, "There was an error posting the photo with caption " + caption);
                    return;
                }

                String id = (String) response.getGraphObject().getProperty("id");
                if (like) {
                    like(id);
                }
            }
        }).executeAsync();
    }


    public void like(String id)
    {
        Session session = Session.getActiveSession();
        if (session == null || session.getState() != SessionState.OPENED) {
            Log.i(TAG, "FacebookUtilities.like() cancelled because there is no active Facebook session");
            return;
        }

        Request likeRequest = new Request(Session.getActiveSession(), id + "/likes",
                null, HttpMethod.POST, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                Log.i(TAG, "Like completed with response: " + response.toString());
            }
        });
        likeRequest.executeAsync();
    }


    public void updateEvents()
    {
        Session session = Session.getActiveSession();
        if (session == null || session.getState() != SessionState.OPENED) {
            Log.i(TAG, "FacebookUtilities.updateEvents() cancelled because there is no active Facebook session");
            return;
        }

        mEventsManager.clearAttendingEvents();
        mEventsManager.cleanAttendedEvents();

        Request.newGraphPathRequest(session, "me/events", mEventsRequestCallback)
                .executeAndWait();
        Log.i(TAG, "FacebookUtilities.updateEvents() finished");
    }


    private Request.Callback mEventsRequestCallback = new Request.Callback() {
        @Override
        public void onCompleted(Response response) {
            Log.d(TAG, "Events completed with response: " + response.toString());

            if (response.getError() != null) {
                Log.e(TAG, "Error fetching events for user");
                return;
            }

            // Handle empty data sets
            Date now = new Date();
            GraphObjectList<GraphObject> list = response.getGraphObjectList();

            if (list == null) {
                Log.i(TAG, "No events on Facebook");
                return;
            }

            for (GraphObject object: list) {
                JSONObject jso = object.getInnerJSONObject();

                FacebookEvent event;
                try {
                    event = new FacebookEvent(jso);
                } catch (org.json.JSONException e) {
                    Log.w(TAG, "Could not create event: " + e.getMessage());
                    continue;
                } catch (ParseException e) {
                    Log.w(TAG, "Could not create event: " + e.getMessage());
                    continue;
                }

                if (event.getEndDate().after(now) && event.isAttending()) {
                    FacebookUtilities.getInstance().addEvent(event.getGraphId());
                }

                Log.d(TAG, event.getGraphId() + " - " + event.getName());
            }
        }
    };


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
        Session session = Session.getActiveSession();
        if (session == null || session.getState() != SessionState.OPENED) {
            Log.i(TAG, "FacebookUtilities.updateEvents() cancelled because there is no active Facebook session");
            return;
        }

        Request.newGraphPathRequest(session, id, mAddEventRequestCallback);
    }

    private Request.Callback mAddEventRequestCallback = new Request.Callback() {
        @Override
        public void onCompleted(Response response) {
            Log.d(TAG, "Event completed with response: " + response);

            if (response.getError() != null) {
                Log.e(TAG, "Error fetching event for user");
                return;
            }

            FacebookEvent event;
            try {
                event = new FacebookEvent(response.getGraphObject().getInnerJSONObject());

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
            } catch (ParseException e) {
                Log.e(TAG, "Could not create event: " + e.getMessage());
            }
        }
    };
}
