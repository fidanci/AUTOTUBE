package com.furkan.autotube;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public final class BrowserActivity extends Activity {
    public static final String EXTRA_INITIAL_URL = "initial_url";

    private LinearLayout toolbar;
    private EditText address;
    private ProgressBar progress;
    private FrameLayout webContainer;
    private WebView webView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF07111F);
        getWindow().setNavigationBarColor(0xFF07111F);
        buildScreen();
        String initialUrl = getIntent().getStringExtra(EXTRA_INITIAL_URL);
        loadUrl((initialUrl == null) ? "https://www.google.com" : initialUrl);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF07111F);

        toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.VERTICAL);
        toolbar.setPadding(dp(12), dp(10), dp(12), dp(8));
        toolbar.setBackgroundColor(0xFF0D1B2E);
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(controls, new LinearLayout.LayoutParams(-1, -2));
        TextView back = control("‹", 0xFF142A43, 42, v -> goBack());
        back.setContentDescription("Önceki sayfaya dön");
        controls.addView(back, new LinearLayout.LayoutParams(dp(42), dp(40)));
        LinearLayout.LayoutParams forwardLp = new LinearLayout.LayoutParams(dp(42), dp(40));
        forwardLp.leftMargin = dp(6);
        controls.addView(control("›", 0xFF142A43, 42, v -> goForward()), forwardLp);

        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(14);
        address.setTextColor(0xFFF5F8FF);
        address.setHintTextColor(0xFF8294AA);
        address.setHint("Web adresi veya arama");
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        address.setPadding(dp(13), 0, dp(13), 0);
        address.setBackground(card(0xFF101D30, 0xFF2A4562, 14));
        LinearLayout.LayoutParams addressLp = new LinearLayout.LayoutParams(0, dp(40), 1);
        addressLp.leftMargin = dp(8);
        controls.addView(address, addressLp);
        address.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = (event != null) && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
            if ((actionId == EditorInfo.IME_ACTION_GO) || enter) {
                loadUrl(address.getText().toString());
                return true;
            }
            return false;
        });

        LinearLayout.LayoutParams goLp = new LinearLayout.LayoutParams(dp(48), dp(40));
        goLp.leftMargin = dp(6);
        controls.addView(control("Git", 0xFF168AAB, 48, v ->
                loadUrl(address.getText().toString())), goLp);
        LinearLayout homeRow = new LinearLayout(this);
        homeRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams homeRowLp = new LinearLayout.LayoutParams(-1, -2);
        homeRowLp.topMargin = dp(8);
        toolbar.addView(homeRow, homeRowLp);
        TextView home = control("⌂   Ana ekran", 0xFF142A43, 112, v -> finish());
        home.setTextSize(12);
        home.setContentDescription("AutoTube ana ekranına dön");
        homeRow.addView(home, new LinearLayout.LayoutParams(dp(112), dp(34)));
        home.setOnClickListener(v -> finish());

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF67D8D0));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF17263A));
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        webContainer = new FrameLayout(this);
        root.addView(webContainer, new LinearLayout.LayoutParams(-1, 0, 1));
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF07111F);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl().getScheme();
                return !"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                address.setText(url);
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                address.setText(url);
                progress.setVisibility(View.GONE);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility((newProgress >= 100) ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                toolbar.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
                webContainer.addView(customView, new FrameLayout.LayoutParams(-1, -1));
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            @Override
            public void onHideCustomView() {
                exitFullScreen();
            }
        });
        webContainer.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    private TextView control(String label, int color, int widthDp, View.OnClickListener listener) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextColor(0xFFF5F8FF);
        button.setTextSize(label.length() > 1 ? 13 : 22);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rounded(color, 14));
        button.setOnClickListener(listener);
        return button;
    }

    private void loadUrl(String raw) {
        if (raw == null) return;
        String value = raw.trim();
        if (value.isEmpty()) return;
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            if (value.contains(" ") || !value.contains(".")) {
                value = "https://www.google.com/search?q="
                        + android.net.Uri.encode(value);
            } else {
                value = "https://" + value;
            }
        }
        address.setText(value);
        webView.loadUrl(value);
    }

    private void goBack() {
        if (webView.canGoBack()) webView.goBack();
        else finish();
    }

    private void goForward() {
        if (webView.canGoForward()) webView.goForward();
    }

    private void exitFullScreen() {
        if (customView == null) return;
        webContainer.removeView(customView);
        customView = null;
        toolbar.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) exitFullScreen();
        else if (webView.canGoBack()) webView.goBack();
        else finish();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webContainer.removeView(webView);
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private GradientDrawable card(int color, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
