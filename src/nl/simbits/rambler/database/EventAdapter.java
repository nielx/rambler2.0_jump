package nl.simbits.rambler.database;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class EventAdapter extends BaseAdapter {

    // Singleton
    private static EventAdapter gEventAdapter;

    public static EventAdapter getInstance() {
        if (gEventAdapter == null) {
            gEventAdapter = new EventAdapter();
        }

        return gEventAdapter;
    }

    private EventAdapter() {
        super();
        mItems = new ArrayList<Event>();
        mHander = new Handler();

        Event e = new Event(Event.EventType.STEP, "Testmessage");
        mItems.add(e);
    }

    // Insides (data management)
    ArrayList<Event> mItems;
    final Handler mHander;

    void addItem(final Event event) {
        // Enqueue work on the main thread
        mHander.post(new Runnable() {
            @Override
            public void run() {
                mItems.add(event);
                notifyDataSetChanged();
            }
        });
    }

    //
    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Event getItem(int i) {
        return mItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View currentView, ViewGroup parent) {
        View itemView = null;
        if (currentView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = inflater.inflate(R.layout.simple_list_item_1, null);
        } else
            itemView = currentView;

        // Set Text
        TextView text = (TextView) itemView.findViewById(R.id.text1);
        text.setText(mItems.get(position).getMessage());
        return itemView;
    }
}
