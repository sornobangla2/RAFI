package com.mrrafi.rafiv2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * RAFI V2 — Pikachu Tools 2.0
 * Developer: MR RAFI
 *
 * Real login + submit to https://pikachu-tools.my.id
 * Handles CSRF token (Laravel 419 fix) with retry loop.
 */
public final class MainActivity extends Activity {

    // ── Config ────────────────────────────────────────────────────────────
    private static final String BASE     = "https://pikachu-tools.my.id";
    private static final String EMAIL    = "rox@gmail.com";
    private static final String PASSWORD = "rox123";

    // ── Colors ────────────────────────────────────────────────────────────
    private static final int BG    = Color.rgb(5,  13, 10);
    private static final int CARD  = Color.rgb(10, 26, 18);
    private static final int FIELD = Color.rgb(7,  19, 13);
    private static final int GREEN = Color.rgb(0,  255, 136);
    private static final int CYAN  = Color.rgb(0,  255, 204);
    private static final int WHITE = Color.rgb(224,255, 232);
    private static final int MUTED = Color.rgb(80, 140, 105);
    private static final int RED   = Color.rgb(255, 51, 85);
    private static final int GOLD  = Color.rgb(255,215,  0);

    // ── UI ────────────────────────────────────────────────────────────────
    private EditText   numberInput;
    private EditText   amountInput;
    private Button     startButton;
    private ProgressBar progressBar;
    private TextView   statusText;
    private TextView   resultText;

    // ── Network ───────────────────────────────────────────────────────────
    private final OkHttpClient     http     = new OkHttpClient.Builder()
            .followRedirects(true)
            .cookieJar(new MemoryCookieJar())   // session cookies
            .build();
    private final ExecutorService  pool     = Executors.newSingleThreadExecutor();
    private final Handler          ui       = new Handler(Looper.getMainLooper());

    private boolean loggedIn = false;

    // ─────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildScreen());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pool.shutdown();
    }

    // ── Build UI ──────────────────────────────────────────────────────────
    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = vbox();
        root.setPadding(dp(20), dp(28), dp(20), dp(36));

        // ─ Header ─
        LinearLayout header = card();
        header.setBackground(gradient());
        add(header, label("⚡  PIKACHU TOOLS 2.0", 11, CYAN, true), MATCH, WRAP, 0);
        gap(header, 8);
        add(header, label("RAFI V2", 36, GREEN, true), MATCH, WRAP, 0);
        gap(header, 4);
        add(header, label("Developer: MR RAFI", 12, GOLD, true), MATCH, WRAP, 0);
        add(root, header, MATCH, WRAP, 0);

        gap(root, 20);

        // ─ Form ─
        LinearLayout form = card();
        add(form, label("▸  NUMBER", 12, GREEN, true), MATCH, WRAP, 0);
        gap(form, 8);
        numberInput = field("01XXXXXXXXX", InputType.TYPE_CLASS_PHONE);
        add(form, numberInput, MATCH, 52, 0);
        gap(form, 16);
        add(form, label("▸  AMOUNT", 12, GREEN, true), MATCH, WRAP, 0);
        gap(form, 8);
        amountInput = field("10", InputType.TYPE_CLASS_NUMBER);
        add(form, amountInput, MATCH, 52, 0);
        gap(form, 20);

        startButton = new Button(this);
        startButton.setText("▶   EXECUTE");
        startButton.setAllCaps(false);
        startButton.setTextSize(16);
        startButton.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        startButton.setTextColor(BG);
        startButton.setBackground(rounded(GREEN, GREEN, 14));
        startButton.setMinHeight(dp(54));
        startButton.setElevation(dp(6));
        startButton.setOnClickListener(v -> onExecute());
        add(form, startButton, MATCH, 56, 0);
        add(root, form, MATCH, WRAP, 0);

        gap(root, 16);

        // ─ Status ─
        LinearLayout statusCard = card();
        add(statusCard, label("STATUS", 11, CYAN, true), MATCH, WRAP, 0);
        gap(statusCard, 10);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminateTintList(
                android.content.res.ColorStateList.valueOf(GREEN));
        add(statusCard, progressBar, MATCH, 6, 0);
        gap(statusCard, 8);

        statusText = label("Ready — tap EXECUTE to start.", 13, MUTED, false);
        statusText.setGravity(Gravity.START);
        add(statusCard, statusText, MATCH, WRAP, 0);
        add(root, statusCard, MATCH, WRAP, 0);

        gap(root, 16);

        // ─ Result ─
        LinearLayout resultCard = card();
        add(resultCard, label("RESULT", 11, GOLD, true), MATCH, WRAP, 0);
        gap(resultCard, 10);
        resultText = label("—", 15, WHITE, false);
        resultText.setGravity(Gravity.START);
        add(resultCard, resultText, MATCH, WRAP, 0);
        add(root, resultCard, MATCH, WRAP, 0);

        gap(root, 12);
        add(root, label("pikachu-tools.my.id  ·  MR RAFI  ©2025", 10, MUTED, false),
                MATCH, WRAP, 0);

        scroll.addView(root, new ScrollView.LayoutParams(MATCH, WRAP));
        return scroll;
    }

    // ── Button handler ────────────────────────────────────────────────────
    private void onExecute() {
        String number = numberInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();

        if (number.isEmpty()) {
            numberInput.setError("Number দাও");
            return;
        }
        if (amountStr.isEmpty()) {
            amountInput.setError("Amount দাও");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            amountInput.setError("সংখ্যা দাও");
            return;
        }

        hideKeyboard();
        setLoading(true);
        setStatus(CYAN, "⏳ Connecting...");
        resultText.setText("—");

        pool.execute(() -> runRequest(number, amount));
    }

    // ── Network logic (background thread) ────────────────────────────────
    private void runRequest(String number, int amount) {
        try {
            // Step 1: Login (if not already)
            if (!loggedIn) {
                uiStatus(MUTED, "🔑 Logging in...");
                boolean ok = doLogin();
                if (!ok) {
                    uiDone(RED, "❌ Login failed. Check credentials.", "Login failed");
                    return;
                }
                loggedIn = true;
                uiStatus(GREEN, "✅ Logged in");
            }

            // Step 2: Submit
            uiStatus(CYAN, "📡 Submitting...");
            String result = doSubmit(number, amount);
            uiDone(GREEN, "✅ Done", result);

        } catch (Exception e) {
            loggedIn = false; // reset on error
            uiDone(RED, "❌ Error: " + e.getMessage(), "Error: " + e.getMessage());
        }
    }

    // ── Login (retry up to 3x for CSRF 419) ──────────────────────────────
    private boolean doLogin() throws IOException {
        for (int i = 0; i < 3; i++) {
            String token = fetchToken(BASE + "/user/login");
            if (token == null) continue;

            FormBody body = new FormBody.Builder()
                    .add("_token",   token)
                    .add("email",    EMAIL)
                    .add("password", PASSWORD)
                    .build();

            Request req = new Request.Builder()
                    .url(BASE + "/user/login")
                    .post(body)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36")
                    .header("Accept",     "text/html,application/xhtml+xml,*/*;q=0.8")
                    .header("Referer",    BASE + "/user/login")
                    .header("Origin",     BASE)
                    .build();

            try (Response resp = http.newCall(req).execute()) {
                int code = resp.code();
                if (code == 419) continue;   // CSRF mismatch → retry with fresh token
                String url = resp.request().url().toString();
                if (!url.contains("/user/login")) return true;  // redirected away = success
            }
        }
        return false;
    }

    // ── Submit ────────────────────────────────────────────────────────────
    private String doSubmit(String number, int amount) throws IOException {
        // Retry once on 419
        for (int i = 0; i < 2; i++) {
            String token = fetchToken(BASE + "/user");
            if (token == null) throw new IOException("No CSRF token on submit page");

            FormBody body = new FormBody.Builder()
                    .add("_token", token)
                    .add("number", number)
                    .add("amount", String.valueOf(amount))
                    .build();

            Request req = new Request.Builder()
                    .url(BASE + "/user")
                    .post(body)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36")
                    .header("Accept",     "text/html,application/xhtml+xml,*/*;q=0.8")
                    .header("Referer",    BASE + "/user")
                    .header("Origin",     BASE)
                    .build();

            try (Response resp = http.newCall(req).execute()) {
                if (resp.code() == 419) continue;
                resp.body();
                String html = resp.body() != null ? resp.body().string() : "";
                return parseResponse(html);
            }
        }
        return "Submitted";
    }

    // ── Fetch CSRF token from a page ──────────────────────────────────────
    private String fetchToken(String url) throws IOException {
        Request req = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36")
                .header("Accept",     "text/html,application/xhtml+xml,*/*;q=0.8")
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (resp.body() == null) return null;
            String html = resp.body().string();

            // Try regex first (faster)
            Pattern p1 = Pattern.compile("name=[\"']_token[\"']\\s+value=[\"']([^\"']+)[\"']");
            Matcher m1 = p1.matcher(html);
            if (m1.find()) return m1.group(1);

            Pattern p2 = Pattern.compile("value=[\"']([^\"']+)[\"']\\s+name=[\"']_token[\"']");
            Matcher m2 = p2.matcher(html);
            if (m2.find()) return m2.group(1);

            // Fallback: Jsoup
            Document doc = Jsoup.parse(html);
            Element el = doc.selectFirst("input[name=_token]");
            if (el != null) return el.val();

            return null;
        }
    }

    // ── Parse server response ─────────────────────────────────────────────
    private String parseResponse(String html) {
        if (html == null || html.isEmpty()) return "Submitted ✓";

        // SweetAlert2
        Pattern swal = Pattern.compile(
                "Swal\\.fire\\s*\\(\\s*\\{(.*?)\\}\\s*\\)", Pattern.DOTALL);
        Matcher sm = swal.matcher(html);
        if (sm.find()) {
            String block = sm.group(1);
            String icon  = extract(block, "icon");
            String title = extract(block, "title");
            String text  = extract(block, "text");
            StringBuilder sb = new StringBuilder();
            if (icon  != null) sb.append("[").append(icon.toUpperCase()).append("] ");
            if (title != null) sb.append(title);
            if (text  != null && !text.equals(title)) sb.append("\n").append(text);
            if (sb.length() > 0) return sb.toString().trim();
        }

        // Bootstrap alerts
        Document doc = Jsoup.parse(html);
        for (String cls : new String[]{"alert-success","alert-danger","alert-warning","alert-info"}) {
            Element el = doc.selectFirst("." + cls);
            if (el != null) {
                String t = el.text().trim();
                if (!t.isEmpty()) return t;
            }
        }

        // Keyword scan
        for (Element el : doc.select("div,p,span")) {
            String t = el.text().trim();
            if (t.isEmpty()) continue;
            String low = t.toLowerCase();
            if (low.contains("success") || low.contains("complete") ||
                low.contains("done")    || low.contains("sent")     ||
                low.contains("error")   || low.contains("failed")   ||
                low.contains("invalid")) {
                return t;
            }
        }
        return "Submitted ✓";
    }

    private String extract(String block, String key) {
        Pattern p = Pattern.compile(key + "\\s*:\\s*['\"]([^'\"]+)['\"]");
        Matcher m = p.matcher(block);
        return m.find() ? m.group(1) : null;
    }

    // ── UI helpers (must run on main thread) ──────────────────────────────
    private void uiStatus(int color, String msg) {
        ui.post(() -> setStatus(color, msg));
    }

    private void uiDone(int color, String status, String result) {
        ui.post(() -> {
            setLoading(false);
            setStatus(color, status);
            resultText.setTextColor(color);
            resultText.setText(result);
        });
    }

    private void setLoading(boolean on) {
        progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        startButton.setEnabled(!on);
        startButton.setText(on ? "⏳  WORKING..." : "▶   EXECUTE");
    }

    private void setStatus(int color, String msg) {
        statusText.setTextColor(color);
        statusText.setText(msg);
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused == null) return;
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        focused.clearFocus();
    }

    // ── Widget builders ───────────────────────────────────────────────────
    private LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout card() {
        LinearLayout l = vbox();
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        l.setBackground(rounded(CARD, Color.rgb(25, 67, 44), 18));
        return l;
    }

    private EditText field(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setTextColor(WHITE);
        e.setHintTextColor(MUTED);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setTypeface(Typeface.MONOSPACE);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(rounded(FIELD, Color.rgb(30, 78, 52), 12));
        return e;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setLineSpacing(dp(2), 1f);
        if (bold) v.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        else      v.setTypeface(Typeface.MONOSPACE);
        return v;
    }

    private GradientDrawable gradient() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(9, 37, 24), Color.rgb(7, 20, 14)});
        d.setCornerRadius(dp(20));
        d.setStroke(dp(1), Color.rgb(0, 120, 70));
        return d;
    }

    private GradientDrawable rounded(int fill, int stroke, int r) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(r));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private void add(LinearLayout parent, View child, int w, int h, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                w == MATCH ? ViewGroup.LayoutParams.MATCH_PARENT : w,
                h == WRAP  ? ViewGroup.LayoutParams.WRAP_CONTENT : dp(h));
        if (topDp > 0) p.topMargin = dp(topDp);
        parent.addView(child, p);
    }

    private void gap(LinearLayout parent, int heightDp) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP  = ViewGroup.LayoutParams.WRAP_CONTENT;
}
