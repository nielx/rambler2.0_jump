package nl.simbits.rambler;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
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
    private WebView mWebView;
    private View mProgressView;
    private OAuthHmacSigner mSigner;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting task to retrieve request token.");
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplication());
        setContentView(R.layout.activity_twitter_oauth);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        Log.i(TAG, "Retrieving request token from Twitter servers");

        mWebView = (WebView)findViewById(R.id.twitter_oauth_webview);
        mProgressView = findViewById(R.id.twitter_oauth_progress);
        mSigner = new OAuthHmacSigner();

        /* WebViewClient must be set BEFORE calling loadUrl! */
        mWebView.setWebViewClient(new TwitterWebViewClient());

        // Start execution
        new GetTokenURLTask().execute(mSigner);
	}

    private class GetTokenURLTask extends AsyncTask<OAuthHmacSigner, Void, String> {

        @Override
        protected String doInBackground(OAuthHmacSigner... params) {
            OAuthHmacSigner signer = params[0];

            signer.clientSharedSecret = Secrets.TWITTER_CONSUMER_SECRET;

            OAuthGetTemporaryToken temporaryToken = new OAuthGetTemporaryToken(TwitterUtilities.REQUEST_URL);
            temporaryToken.transport = new ApacheHttpTransport();
            temporaryToken.signer = signer;
            temporaryToken.consumerKey = Secrets.TWITTER_CONSUMER_KEY;
            temporaryToken.callback = TwitterUtilities.OAUTH_CALLBACK_URL;

            OAuthCredentialsResponse tempCredentials;
            try {
                tempCredentials = temporaryToken.execute();
                signer.tokenSharedSecret = tempCredentials.tokenSecret;
            } catch (IOException e) {
                Log.e(TAG, "Error fetching temporary URL");
                return "";
            }

            OAuthAuthorizeTemporaryTokenUrl authorizeUrl = new OAuthAuthorizeTemporaryTokenUrl(TwitterUtilities.AUTHORIZE_URL);
            authorizeUrl.temporaryToken = tempCredentials.token;
            return authorizeUrl.build();
        }

        @Override
        protected void onPostExecute(String url) {
            if (url.equals("")) {
                setResult(RESULT_CANCELED);
                finish ();
            }
            mWebView.loadUrl(url);
        }
    }


    private class AccessTokenTask extends AsyncTask<OAuthGetAccessToken, Void, OAuthCredentialsResponse> {
        @Override
        protected OAuthCredentialsResponse doInBackground(OAuthGetAccessToken... oAuthGetAccessTokens) {
            OAuthGetAccessToken accessToken = oAuthGetAccessTokens[0];
            OAuthCredentialsResponse credentials;
            try {
                credentials = accessToken.execute();
            } catch (IOException e) {
                Log.e(TAG, "Error validating access token");
                credentials = null;
            }
            return credentials;
        }

        @Override
        protected void onPostExecute(OAuthCredentialsResponse credentials) {
            if (credentials == null) {
                setResult(RESULT_CANCELED);
                finish();
            } else {
                mSigner.tokenSharedSecret = credentials.tokenSecret;

                Intent in = new Intent();
                Bundle b = new Bundle();
                b.putString("token", credentials.token);
                b.putString("secret", credentials.tokenSecret);
                in.putExtras(b);
                setResult(RESULT_OK, in);
                finish();
            }
        }
    }

    private class TwitterWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url,Bitmap bitmap)  {
            System.out.println("onPageStarted : " + url);
        }
        @Override
        public void onPageFinished(WebView view, String url)  {
            // Hide the progress view
            // TODO: animate in Android 4
            mProgressView.setVisibility(View.GONE);
            mWebView.setVisibility(View.VISIBLE);

            if (url.startsWith(TwitterUtilities.OAUTH_CALLBACK_URL)) {
                if (url.indexOf("oauth_token=")!=-1) {

                    String requestToken  = extractParamFromUrl(url,"oauth_token");
                    String verifier= extractParamFromUrl(url,"oauth_verifier");

                    mSigner.clientSharedSecret = Secrets.TWITTER_CONSUMER_SECRET;

                    OAuthGetAccessToken accessToken = new OAuthGetAccessToken(TwitterUtilities.ACCESS_URL);
                    accessToken.transport = new ApacheHttpTransport();
                    accessToken.temporaryToken = requestToken;
                    accessToken.signer = mSigner;
                    accessToken.consumerKey = Secrets.TWITTER_CONSUMER_KEY;
                    accessToken.verifier = verifier;


                    mProgressView.setVisibility(View.VISIBLE);
                    mWebView.setVisibility(View.GONE);

                    new AccessTokenTask().execute(accessToken);

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
            }
            Log.d(TAG, "onPageFinished : " + url);

        }
        private String extractParamFromUrl(String url,String paramName) {
            String queryString = url.substring(url.indexOf("?", 0)+1,url.length());
            QueryStringParser queryStringParser = new QueryStringParser(queryString);
            return queryStringParser.getQueryParamValue(paramName);
        }

    }
}
