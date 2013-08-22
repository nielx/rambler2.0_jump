package nl.simbits.rambler;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ecs.sample.store.SharedPreferencesCredentialStore;
import com.ecs.sample.util.QueryStringParser;

import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.google.api.client.auth.oauth.OAuthGetTemporaryToken;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.http.apache.ApacheHttpTransport;

public class TwitterOAuthActivity extends Activity {

	final String TAG = getClass().getName();
	
	private SharedPreferences prefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting task to retrieve request token.");
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplication());
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		WebView webview = new WebView(this);
        webview.getSettings().setJavaScriptEnabled(true);  
        webview.setVisibility(View.VISIBLE);
        setContentView(webview);
        
        Log.i(TAG, "Retrieving request token from Google servers");

        try {
        	
	        final OAuthHmacSigner signer = new OAuthHmacSigner();
	        signer.clientSharedSecret = TwitterUtilities.CONSUMER_SECRET;
	        
			OAuthGetTemporaryToken temporaryToken = new OAuthGetTemporaryToken(TwitterUtilities.REQUEST_URL);
			temporaryToken.transport = new ApacheHttpTransport();
			temporaryToken.signer = signer;
			temporaryToken.consumerKey = TwitterUtilities.CONSUMER_KEY;
			temporaryToken.callback = TwitterUtilities.OAUTH_CALLBACK_URL;
			
			OAuthCredentialsResponse tempCredentials = temporaryToken.execute();
			signer.tokenSharedSecret = tempCredentials.tokenSecret;
			
			OAuthAuthorizeTemporaryTokenUrl authorizeUrl = new OAuthAuthorizeTemporaryTokenUrl(TwitterUtilities.AUTHORIZE_URL);
			authorizeUrl.temporaryToken = tempCredentials.token;
			String authorizationUrl = authorizeUrl.build();
			
	        
	        /* WebViewClient must be set BEFORE calling loadUrl! */  
	        webview.setWebViewClient(new WebViewClient() {  
	
	        	@Override  
	            public void onPageStarted(WebView view, String url,Bitmap bitmap)  {  
	        		System.out.println("onPageStarted : " + url);
	            }
	        	@Override  
	            public void onPageFinished(WebView view, String url)  {  
	            	
	            	if (url.startsWith(TwitterUtilities.OAUTH_CALLBACK_URL)) {
	            		try {
							
	            			if (url.indexOf("oauth_token=")!=-1) {
	            			
		            			String requestToken  = extractParamFromUrl(url,"oauth_token");
		            			String verifier= extractParamFromUrl(url,"oauth_verifier");
								
		            			signer.clientSharedSecret = TwitterUtilities.CONSUMER_SECRET;
	
		            			OAuthGetAccessToken accessToken = new OAuthGetAccessToken(TwitterUtilities.ACCESS_URL);
		            			accessToken.transport = new ApacheHttpTransport();
		            			accessToken.temporaryToken = requestToken;
		            			accessToken.signer = signer;
		            			accessToken.consumerKey = TwitterUtilities.CONSUMER_KEY;
		            			accessToken.verifier = verifier;
	
		            			OAuthCredentialsResponse credentials = accessToken.execute();
		            			signer.tokenSharedSecret = credentials.tokenSecret;
	
					  		    view.setVisibility(View.INVISIBLE);
                                
                                Intent in = new Intent();
                                Bundle b = new Bundle();
                                b.putString("token", credentials.token);
                                b.putString("secret", credentials.tokenSecret);
                                in.putExtras(b);
                                setResult(RESULT_OK, in);
                                finish();
	            			} else if (url.indexOf("error=")!=-1) {
	            				view.setVisibility(View.INVISIBLE);
	            				new SharedPreferencesCredentialStore(prefs).clearCredentials();
                                setResult(RESULT_CANCELED);
                                finish();
	            			} else if (url.indexOf("denied=")!=-1) {
	            				view.setVisibility(View.INVISIBLE);
	            				new SharedPreferencesCredentialStore(prefs).clearCredentials();
                                setResult(RESULT_CANCELED);
                                finish();
	            			}
						} catch (IOException e) {
							e.printStackTrace();
						}
	
	            	}
	                System.out.println("onPageFinished : " + url);
	  		      
	            }
				private String extractParamFromUrl(String url,String paramName) {
					String queryString = url.substring(url.indexOf("?", 0)+1,url.length());
					QueryStringParser queryStringParser = new QueryStringParser(queryString);
					return queryStringParser.getQueryParamValue(paramName);
				}  
	
	        });  
	        
	        webview.loadUrl(authorizationUrl);	
        } catch (Exception ex) {
        	ex.printStackTrace();
        }

	}

}
