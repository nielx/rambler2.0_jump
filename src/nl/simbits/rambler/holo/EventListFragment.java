package nl.simbits.rambler.holo;

import android.app.ListFragment;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Locale;

import nl.simbits.rambler.database.Event;

public class EventListFragment extends ListFragment {
    @Override
    public void onStart() {
        super.onStart();
        getListView().setDivider(null);
        getListView().setDividerHeight(0);
        getListView().setClickable(false);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Event event = (Event)getListAdapter().getItem(position);
        if (event.getLocation() != null) {
            String uri = String.format(Locale.ENGLISH, "geo:%f,%f", event.getLocation().getLatitude(),
                    event.getLocation().getLongitude());
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            getActivity().startActivity(intent);
        } else {
            Toast.makeText(getActivity(), "No location available", Toast.LENGTH_SHORT).show();
        }
    }
}
