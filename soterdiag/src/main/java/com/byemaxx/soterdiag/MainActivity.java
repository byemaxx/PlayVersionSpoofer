package com.byemaxx.soterdiag;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.soter.soterserver.ISoterService;
import com.tencent.soter.soterserver.SoterDeviceResult;
import com.tencent.soter.soterserver.SoterExportResult;
import com.tencent.soter.soterserver.SoterSessionResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String SOTER_PACKAGE = "com.tencent.soter.soterserver";
    private static final String SOTER_ACTION = "com.tencent.soter.soterserver.ISoterService";
    private static final String TEST_AUTH_KEY = "soter_diag_auth_v1";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final StringBuilder report = new StringBuilder();
    private TextView output;
    private volatile ISoterService soterService;
    private boolean bound;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            soterService = ISoterService.Stub.asInterface(service);
            append("[PASS] Binder connected: " + name.flattenToShortString());
            runReadOnlyServiceTests();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            soterService = null;
            append("[WARN] Binder disconnected: " + name.flattenToShortString());
        }

        @Override
        public void onBindingDied(ComponentName name) {
            soterService = null;
            append("[FAIL] Binder died: " + name.flattenToShortString());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        append("Soter Diagnostics v1.0");
        append("Local-only diagnostic; no network access.");
        append("Test UID: " + Process.myUid());
        append("");
        runPlatformChecks();
        bindSoterService();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Soter Diagnostics");
        title.setTextSize(24f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Direct Binder tests for com.tencent.soter.soterserver");
        subtitle.setTextSize(13f);
        subtitle.setPadding(0, dp(4), 0, dp(12));
        root.addView(subtitle);

        root.addView(button("Reconnect / Read-only tests", v -> bindSoterService()));
        root.addView(button("Test ASK (create if missing)", v -> testAsk()));
        root.addView(button("Test AuthKey (create if missing)", v -> testAuthKey()));
        root.addView(button("Init Soter sign session", v -> testSignSession()));
        root.addView(button("Copy report", v -> copyReport()));

        output = new TextView(this);
        output.setTextSize(12f);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextIsSelectable(true);
        output.setMovementMethod(new ScrollingMovementMethod());
        output.setPadding(0, dp(12), 0, dp(24));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(output, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private Button button(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        b.setLayoutParams(lp);
        return b;
    }

    private void runPlatformChecks() {
        append("=== Platform ===");
        append("Manufacturer: " + Build.MANUFACTURER);
        append("Model: " + Build.MODEL);
        append("Device: " + Build.DEVICE);
        append("Android: " + Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT);
        append("Build fingerprint: " + Build.FINGERPRINT);

        try {
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            append("Device secure: " + (km != null && km.isDeviceSecure()));
        } catch (Throwable t) {
            append("[WARN] Keyguard check: " + error(t));
        }

        try {
            FingerprintManager fm = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
            if (fm == null) {
                append("FingerprintManager: unavailable");
            } else {
                append("Fingerprint hardware: " + fm.isHardwareDetected());
                append("Fingerprint enrolled: " + fm.hasEnrolledFingerprints());
            }
        } catch (Throwable t) {
            append("[WARN] Fingerprint check: " + error(t));
        }

        append("");
        append("=== Soter package ===");
        try {
            PackageInfo pi;
            if (Build.VERSION.SDK_INT >= 33) {
                pi = getPackageManager().getPackageInfo(SOTER_PACKAGE, PackageManager.PackageInfoFlags.of(0));
            } else {
                pi = getPackageManager().getPackageInfo(SOTER_PACKAGE, 0);
            }
            ApplicationInfo ai = pi.applicationInfo;
            boolean system = ai != null && (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean updatedSystem = ai != null && (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            append("[PASS] Package present");
            append("Version: " + pi.versionName + " (" + pi.getLongVersionCode() + ")");
            append("Source: " + (ai == null ? "unknown" : ai.sourceDir));
            append("FLAG_SYSTEM: " + system);
            append("FLAG_UPDATED_SYSTEM_APP: " + updatedSystem);
        } catch (PackageManager.NameNotFoundException e) {
            append("[FAIL] Soter package not installed");
        }

        try {
            Intent intent = new Intent(SOTER_ACTION).setPackage(SOTER_PACKAGE);
            List<ResolveInfo> services;
            if (Build.VERSION.SDK_INT >= 33) {
                services = getPackageManager().queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(0));
            } else {
                services = getPackageManager().queryIntentServices(intent, 0);
            }
            append("Resolvable Soter services: " + services.size());
            for (ResolveInfo ri : services) {
                if (ri.serviceInfo != null) {
                    append("  - " + ri.serviceInfo.packageName + "/" + ri.serviceInfo.name
                            + " exported=" + ri.serviceInfo.exported);
                }
            }
        } catch (Throwable t) {
            append("[WARN] Service resolution: " + error(t));
        }
        append("");
    }

    private void bindSoterService() {
        if (bound && soterService != null) {
            append("[PASS] Soter Binder already connected");
            runReadOnlyServiceTests();
            return;
        }

        try {
            Intent intent = new Intent(SOTER_ACTION).setPackage(SOTER_PACKAGE);
            boolean result = bindService(intent, connection, Context.BIND_AUTO_CREATE);
            bound = result;
            append(result ? "[INFO] bindService accepted; waiting for callback"
                    : "[FAIL] bindService returned false");
        } catch (Throwable t) {
            append("[FAIL] bindService exception: " + error(t));
        }
    }

    private void runReadOnlyServiceTests() {
        ISoterService svc = soterService;
        if (svc == null) {
            append("[FAIL] Service is not connected");
            return;
        }

        worker.execute(() -> {
            append("=== Soter service read-only ===");
            try {
                append("[PASS] Service version: " + svc.getVersion());
            } catch (Throwable t) {
                append("[FAIL] getVersion: " + error(t));
            }

            try {
                SoterDeviceResult d = svc.getDeviceId();
                if (d == null) {
                    append("[FAIL] getDeviceId returned null");
                } else {
                    append("DeviceId resultCode: " + d.resultCode);
                    append("DeviceId length: " + dataLength(d.exportData, d.exportDataLength));
                    append("DeviceId SHA-256: " + digest(d.exportData));
                }
            } catch (Throwable t) {
                append("[FAIL] getDeviceId: " + error(t));
            }
            append("");
        });
    }

    private void testAsk() {
        ISoterService svc = requireService();
        if (svc == null) return;
        worker.execute(() -> testAskInternal(svc));
    }

    private boolean testAskInternal(ISoterService svc) {
        int uid = Process.myUid();
        append("=== ASK test (UID " + uid + ") ===");
        try {
            boolean before = svc.hasAskAlready(uid);
            append("ASK exists before: " + before);
            if (!before) {
                int rc = svc.generateAppSecureKey(uid);
                append((rc == 0 ? "[PASS]" : "[FAIL]") + " generateAppSecureKey rc=" + rc);
            }
            boolean after = svc.hasAskAlready(uid);
            append("ASK exists after: " + after);
            if (!after) {
                append("[FAIL] ASK is still absent");
                append("");
                return false;
            }

            SoterExportResult r = svc.getAppSecureKey(uid);
            if (r == null) {
                append("[FAIL] getAppSecureKey returned null");
                append("");
                return false;
            }
            append("ASK export resultCode: " + r.resultCode);
            append("ASK export length: " + dataLength(r.exportData, r.exportDataLength));
            append("ASK export SHA-256: " + digest(r.exportData));
            boolean ok = r.resultCode == 0 && r.exportData != null && r.exportData.length > 0;
            append(ok ? "[PASS] ASK generation/export works" : "[FAIL] ASK export is invalid");
            append("");
            return ok;
        } catch (Throwable t) {
            append("[FAIL] ASK test: " + error(t));
            append("");
            return false;
        }
    }

    private void testAuthKey() {
        ISoterService svc = requireService();
        if (svc == null) return;
        worker.execute(() -> testAuthKeyInternal(svc));
    }

    private boolean testAuthKeyInternal(ISoterService svc) {
        int uid = Process.myUid();
        append("=== AuthKey test ===");
        try {
            if (!svc.hasAskAlready(uid) && !testAskInternal(svc)) {
                append("[FAIL] Cannot test AuthKey without ASK");
                append("");
                return false;
            }

            boolean before = svc.hasAuthKey(uid, TEST_AUTH_KEY);
            append("AuthKey exists before: " + before);
            if (!before) {
                int rc = svc.generateAuthKey(uid, TEST_AUTH_KEY);
                append((rc == 0 ? "[PASS]" : "[FAIL]") + " generateAuthKey rc=" + rc);
            }
            boolean after = svc.hasAuthKey(uid, TEST_AUTH_KEY);
            append("AuthKey exists after: " + after);
            if (!after) {
                append("[FAIL] AuthKey is still absent");
                append("");
                return false;
            }

            SoterExportResult r = svc.getAuthKey(uid, TEST_AUTH_KEY);
            if (r == null) {
                append("[FAIL] getAuthKey returned null");
                append("");
                return false;
            }
            append("AuthKey export resultCode: " + r.resultCode);
            append("AuthKey export length: " + dataLength(r.exportData, r.exportDataLength));
            append("AuthKey export SHA-256: " + digest(r.exportData));
            boolean ok = r.resultCode == 0 && r.exportData != null && r.exportData.length > 0;
            append(ok ? "[PASS] AuthKey generation/export works" : "[FAIL] AuthKey export is invalid");
            append("");
            return ok;
        } catch (Throwable t) {
            append("[FAIL] AuthKey test: " + error(t));
            append("");
            return false;
        }
    }

    private void testSignSession() {
        ISoterService svc = requireService();
        if (svc == null) return;
        worker.execute(() -> {
            append("=== Soter sign-session init ===");
            try {
                if (!svc.hasAuthKey(Process.myUid(), TEST_AUTH_KEY) && !testAuthKeyInternal(svc)) {
                    append("[FAIL] Cannot initialize signing without AuthKey");
                    append("");
                    return;
                }
                String challenge = "soter-diag-" + System.currentTimeMillis();
                SoterSessionResult r = svc.initSigh(Process.myUid(), TEST_AUTH_KEY, challenge);
                if (r == null) {
                    append("[FAIL] initSigh returned null");
                } else {
                    append("initSigh resultCode: " + r.resultCode);
                    append("Session allocated: " + (r.session != 0));
                    append(r.resultCode == 0 && r.session != 0
                            ? "[PASS] Soter signing session can be initialized"
                            : "[FAIL] Soter signing-session initialization failed");
                }
                append("Note: this does not call finishSign; biometric authorization is a separate step.");
            } catch (Throwable t) {
                append("[FAIL] initSigh: " + error(t));
            }
            append("");
        });
    }

    private ISoterService requireService() {
        ISoterService svc = soterService;
        if (svc == null) {
            append("[FAIL] Soter service not connected. Tap Reconnect first.");
            bindSoterService();
            return null;
        }
        return svc;
    }

    private void copyReport() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Soter diagnostic report", report.toString()));
            Toast.makeText(this, "Report copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void append(String line) {
        synchronized (report) {
            report.append(line).append('\n');
        }
        runOnUiThread(() -> {
            if (output != null) {
                output.append(line + "\n");
            }
        });
    }

    private static int dataLength(byte[] data, int declared) {
        return data != null ? data.length : declared;
    }

    private static String digest(byte[] data) {
        if (data == null || data.length == 0) return "<none>";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(hash.length, 12); i++) {
                sb.append(String.format(Locale.US, "%02x", hash[i] & 0xff));
            }
            return sb + "…";
        } catch (Throwable t) {
            return "<digest error>";
        }
    }

    private static String error(Throwable t) {
        String message = t.getMessage();
        return t.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (bound) {
            try {
                unbindService(connection);
            } catch (Throwable ignored) {
            }
        }
        worker.shutdownNow();
        super.onDestroy();
    }
}
