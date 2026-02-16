package ru.yandex.myapplication;

import static ru.yandex.myapplication.Utils.saveToken;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity4 extends Activity {

    private static final String CLIENT_ID = "23cabbbdc6cd418abb4b39c32c41195d";

    private static final String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id=" + CLIENT_ID;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        setContentView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (url.contains("access_token=")) {

                    String token = url.split("access_token=")[1].split("&")[0];
                    Log.i("Access token", token);
                  onAuthSuccess(token);

                    return true;
                }
                return false;
            }
        });

        webView.loadUrl(AUTH_URL);
    }

    private void onAuthSuccess(String token) {
        saveToken(this, token); // сохраняем в SharedPreferences


        setResult(Activity.RESULT_OK);


        finish();
    }
}
