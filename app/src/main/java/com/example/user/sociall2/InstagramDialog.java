package com.example.user.sociall2;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/////////////////////////PLEASE NOTE
////////////////////////I USED THE FOLLOWING SOURCES FOR THE INSTAGRAM APP AND DIALOG CLASSES
////////////////////////http://grishma102.blogspot.co.uk/2014/01/instagram-api-integration-in-android.html
////////////////////////http://stackoverflow.com/questions/20290309/how-to-get-instagram-data-from-a-user-on-android?rq=1

public class InstagramDialog extends Dialog {

    //dimensions of instagram log in webview
    static final float[] DIMENSIONS_LANDSCAPE = { 250, 300 };
    static final float[] DIMENSIONS_PORTRAIT = { 300, 250 };
    static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    static final int MARGIN = 4;
    static final int PADDING = 2;

    private String url;
    private OAuthDialogListener listener;
    private ProgressDialog progressDialog;
    private WebView instagramWebView;
    private LinearLayout layout;
    private TextView title;

    public InstagramDialog(Context context, String url, OAuthDialogListener listener) {
        super(context);

        this.url = url;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //textbox which appears to inform user of loading progress
         progressDialog = new ProgressDialog(getContext());
         progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
         progressDialog.setMessage("Loading...");

        layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        //set up the web view
        setUpTitle();
        setUpWebView();

        //get dimensions
        Display display = getWindow().getWindowManager().getDefaultDisplay();
        final float scale = getContext().getResources().getDisplayMetrics().density;
        float[] dimensions = (display.getWidth() < display.getHeight()) ? DIMENSIONS_PORTRAIT
                : DIMENSIONS_LANDSCAPE;

        addContentView(layout, new FrameLayout.LayoutParams(
                (int) (dimensions[0] * scale + 0.5f), (int) (dimensions[1]
                * scale + 0.5f)));

        CookieSyncManager.createInstance(getContext());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

    }

    private void setUpTitle() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        title = new TextView(getContext());
        title.setText("Instagram");
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setBackgroundColor(Color.BLACK);
        title.setPadding(MARGIN + PADDING, MARGIN, MARGIN, MARGIN);
        layout.addView(title);
    }

    private void setUpWebView() {
        instagramWebView = new WebView(getContext());
        instagramWebView.setVerticalScrollBarEnabled(false);
        instagramWebView.setHorizontalScrollBarEnabled(false);
        instagramWebView.setWebViewClient(new OAuthWebViewClient());
        instagramWebView.getSettings().setJavaScriptEnabled(true);
        instagramWebView.loadUrl(url);
        instagramWebView.setLayoutParams(FILL);
        layout.addView(instagramWebView);
    }

    //when instagram returns url, compare with callback
    private class OAuthWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url.startsWith(InstagramApp.callbackURL)) {
                String urls[] = url.split("=");
                listener.onComplete(urls[1]);
                InstagramDialog.this.dismiss();
                return false;
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);

            listener.onError(String.valueOf(error));
            InstagramDialog.this.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {

            super.onPageStarted(view, url, favicon);
            progressDialog.show();
        }

        //page is now open and user can type in log in details
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            String title = instagramWebView.getTitle();
            if (title != null && title.length() > 0) {
                InstagramDialog.this.title.setText(title);
            }
            progressDialog.dismiss();
        }

    }

    public interface OAuthDialogListener {
        public abstract void onComplete(String accessToken);

        public abstract void onError(String error);
    }


}
