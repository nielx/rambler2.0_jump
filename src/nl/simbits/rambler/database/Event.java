package nl.simbits.rambler.database;


import java.util.Date;

public class Event {
    public enum EventType {
        FACEBOOK,
        TWITTER,
        MAP,
        STEP
    }

    private String mEventMessage;
    private EventType mEventType;
    private Date mDate;

    public Event(EventType type, String message) {
        mEventMessage = message;
        mEventType = type;
        mDate = new Date();
    }

    public EventType getType() {
        return mEventType;
    }

    public String getMessage() {
        return mEventMessage;
    }

    public Date getDate() {
        return mDate;
    }
}
