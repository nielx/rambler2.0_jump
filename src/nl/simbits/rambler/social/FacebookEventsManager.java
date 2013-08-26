package nl.simbits.rambler.social;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class FacebookEventsManager
{
    public static final String TAG = "FacebookEventsManager";    

    private List<FacebookEvent> mAttendingEvents;
    private List<FacebookEvent> mAttendedEvents;

    public FacebookEventsManager() 
    {
        mAttendingEvents = new ArrayList<FacebookEvent>();
        mAttendedEvents = new ArrayList<FacebookEvent>();
    }

    public synchronized void clearAttendingEvents()
    {
        mAttendingEvents.clear();
    }

    public synchronized void cleanAttendedEvents()
    {
        ListIterator<FacebookEvent> i = mAttendedEvents.listIterator();        

        while (i.hasNext()) {
            FacebookEvent e = i.next();
            if (!e.isOngoing()) {
                i.remove();
            }
        }
    }

    public synchronized void attendEvent(FacebookEvent event)
    {
        if (event == null)
            return;

        if (!mAttendingEvents.contains(event)) {
            Log.d(TAG, "No such event to attend: " + event);
            return;
        }

        if (mAttendedEvents.contains(event)) {
            Log.d(TAG, "Event already visited: " + event);
            return;
        }

        if (!event.isOngoing()) {
            Log.d(TAG, "Event is not ongoing: " + event);
            return;
        }

        mAttendedEvents.add(event);
        mAttendingEvents.remove(event);
    }

    public synchronized void add(FacebookEvent event)
    {
        if (event == null)
            return;

        if (mAttendingEvents.contains(event)) {
            Log.d(TAG, "--- Event already added: " + event);
            return;
        }

        if (mAttendedEvents.contains(event)) {
            Log.d(TAG, "--- Event already attended: " + event);
            return; 
        }

        mAttendingEvents.add(event);
        Log.d(TAG, "--- Event added: " + event);
    }
    
    public synchronized List<FacebookEvent> getEventsInVicinityOf(Location location, float distance)
    {
        List<FacebookEvent> events = new ArrayList<FacebookEvent>();
        ListIterator<FacebookEvent> i = mAttendingEvents.listIterator();        

        while (i.hasNext()) {
            FacebookEvent e = i.next();

            if (!e.hasLocation())
                continue;

            if (!e.isOngoing()) {
                continue;
            }

            if (mAttendedEvents.contains(e)) {
                i.remove();
                continue;
            }

            Location l = e.getLocation();
            float d = location.distanceTo(l);
            if (d <= distance) 
                events.add(e);
        }

        return events;
    }

    @Override
    public String toString()
    {
        String msg = this.getClass().getName() + ": " + mAttendingEvents.toString();
        return msg;
    }
}

