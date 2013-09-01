package nl.simbits.rambler.database;


public class Event {
    public enum EventType {
        FACEBOOK,
        TWITTER,
        MAP,
        STEP
    }

    private String mEventMessage;
    private EventType mEventType;

    public Event(EventType type, String message) {
        mEventMessage = message;
        mEventType = type;
    }

    public EventType getType() {
        return mEventType;
    }

    public String getMessage() {
        return mEventMessage;
    }
}
