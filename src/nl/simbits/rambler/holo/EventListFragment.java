package nl.simbits.rambler.holo;

import android.app.ListFragment;
import android.widget.AbsListView;

public class EventListFragment extends ListFragment {
    @Override
    public void onStart() {
        super.onStart();
        getListView().setDivider(null);
        getListView().setDividerHeight(0);
    }
}
