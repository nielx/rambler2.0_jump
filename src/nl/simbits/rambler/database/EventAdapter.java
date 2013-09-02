package nl.simbits.rambler.database;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import nl.simbits.rambler.R;

public class EventAdapter extends BaseAdapter {
    private static final String TAG = "EventAdapter";

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
    }

    // Insides (data management)
    ArrayList<Event> mItems;
    final Handler mHander;

    public void addItem(final Event event) {
        if (event.getPictureUrl() != null) {
            // We have to get the picture first
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(event.getPictureUrl(), new BinaryHttpResponseHandler() {
                @Override
                public void onFailure(Throwable throwable, byte[] bytes) {
                    Log.e(TAG, "Unable to fetch the picture for " + event.getMessage());
                    mHander.post(new Runnable() {
                        @Override
                        public void run() {
                            mItems.add(event);
                            notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onSuccess(byte[] bytes) {
                    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    event.setPicture(bitmap);

                    mHander.post(new Runnable() {
                        @Override
                        public void run() {
                            mItems.add(event);
                            notifyDataSetChanged();
                        }
                    });
                }
            });
        } else {

            // Enqueue work on the main thread
            mHander.post(new Runnable() {
                @Override
                public void run() {
                    mItems.add(event);
                    notifyDataSetChanged();
                }
            });
        }
    }

    public void clear() {
        mItems = new ArrayList<Event>();
        notifyDataSetChanged();
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
        if (currentView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            currentView = inflater.inflate(R.layout.row_event, null);
        }

        // Set image
        ImageView image = (ImageView)currentView.findViewById(R.id.event_logo);

        switch (mItems.get(position).getType()) {
            case FACEBOOK:
                image.setImageResource(R.drawable.logo_facebook);
                break;
            case TWITTER:
                image.setImageResource(R.drawable.logo_twitter);
                break;
            case BLUETOOTH:
                image.setImageResource(R.drawable.ic_device_access_bluetooth_connected);
                break;
        }

        // Set Text
        TextView text = (TextView) currentView.findViewById(R.id.text1);
        text.setText(mItems.get(position).getMessage());

        // Print date
        TextView date = (TextView) currentView.findViewById(R.id.event_date);
        date.setText(mItems.get(position).getDate().toLocaleString());

        ImageView picture = (ImageView) currentView.findViewById(R.id.event_picture);
        // If there is a picture, show it
        if (mItems.get(position).getPicture() != null) {
            picture.setVisibility(View.VISIBLE);
            picture.setAdjustViewBounds(true);
            picture.setImageBitmap(mItems.get(position).getPicture());
        } else {
            // The listview reuses old listitems so prevent that unintended images turn up
            picture.setVisibility(View.GONE);
        }

        return currentView;
    }
}
