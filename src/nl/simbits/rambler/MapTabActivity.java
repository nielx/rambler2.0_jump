package nl.simbits.rambler;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class MapTabActivity extends Activity {
	WebView mWebView;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_tab_layout);

        mWebView = (WebView) findViewById(R.id.ramblerMapWebview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl("http://rambler.projects.simbits.nl/map/rambler.html?update=10");
    }
}
