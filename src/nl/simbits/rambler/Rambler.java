package nl.simbits.rambler;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.content.res.Resources;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;

public class Rambler extends TabActivity
{
    final static String TAG = "Rambler";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        
        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost(); // The activity TabHost
        TabHost.TabSpec spec;           // Resusable TabSpec for each tab
        Intent intent;                  // Reusable Intent for each tab
   
        if (ServiceTools.isServiceRunning(this) == false){
            Log.d(TAG,"-->service will be started.");
            startService(new Intent(this, RamblerService.class));
        }
        
        intent = new Intent().setClass(this, MainTabActivity.class);
        spec = tabHost.newTabSpec("Main").setIndicator("Main",
                        res.getDrawable(R.drawable.main_tab))
                      .setContent(intent);
        tabHost.addTab(spec);
        
        intent = new Intent().setClass(this, ShoeTabActivity.class);
        spec = tabHost.newTabSpec("Shoe").setIndicator("Shoe",
                        res.getDrawable(R.drawable.shoe_tab))
                      .setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, MapTabActivity.class);
        spec = tabHost.newTabSpec("Map").setIndicator("Map",
                        res.getDrawable(R.drawable.google_tab))
                      .setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	Log.d(TAG, "onStart");
    }
 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_exit:
                stopService(new Intent(this, RamblerService.class));
                finish();
                break;
        }
        
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
 
    @Override
    public void onResume() {
    	super.onResume();
    }   

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
