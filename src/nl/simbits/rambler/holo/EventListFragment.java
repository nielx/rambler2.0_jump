package nl.simbits.rambler.holo;

import android.app.ListFragment;

public class EventListFragment extends ListFragment {
    @Override
    public void onStart() {
        super.onStart();
        getListView().setDivider(null);
        getListView().setDividerHeight(0);
        getListView().setClickable(false);
    }
}
