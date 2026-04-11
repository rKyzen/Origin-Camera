package com.betterglyph;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.service.quicksettings.TileService;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private MaterialButton btnToggle;
    private MaterialButton btnGrantPermissions;
    private TextView tvStatus, tvDeviceLabel, tvLatencyValue, tvGammaValue;
    private TextView tvActivePresetName, tvActivePresetDescription;
    private View statusDot;
    private Spinner spinnerDevice;
    private SeekBar seekLatency, seekGamma;
    private HorizontalScrollView presetScroll;
    private LinearLayout presetCarousel;

    // Tab views
    private TextView tabPlay, tabTune, tabInfo;
    private LinearLayout panelPlay, panelTune, panelInfo;
    private int mActiveTab = 0; // 0=play, 1=tune, 2=info

    // ── State ──────────────────────────────────────────────────────────────
    private boolean mIsVisualizing   = false;
    private String  mSelectedPreset  = "";
    private int     mSelectedDevice  = DeviceProfile.DEVICE_UNKNOWN;
    private int     mLatencyMs       = 0;
    private float   mGamma           = AudioCaptureService.DEFAULT_GAMMA;
    private boolean mApplyingLatency = false;
    private boolean mApplyingGamma   = false;

    private final List<AudioCaptureService.PresetInfo> mPresetInfos  = new ArrayList<>();
    private final List<MaterialButton>                 mPresetButtons = new ArrayList<>();

    // ── Service binding ────────────────────────────────────────────────────
    private AudioCaptureService mService = null;
    private boolean mBound    = false;
    private int     mPending  = 0;
    private Intent  mPendData = null;
    private boolean mHasTok   = false;

    private final ServiceConnection mConn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            mService = ((AudioCaptureService.LocalBinder) b).getService();
            mBound = true;
            mService.setDevice(mSelectedDevice);
            if (!mSelectedPreset.isEmpty()) mService.setPreset(mSelectedPreset);
            mService.setLatencyCompensationMs(mLatencyMs);
            mService.setGamma(mGamma);
            if (mHasTok && mPendData != null) {
                mService.startCapture(mPending, mPendData);
                mPending = 0; mPendData = null; mHasTok = false;
            }
        }
        @Override public void onServiceDisconnected(ComponentName n) { mService=null; mBound=false; }
    };

    private MediaProjectionManager mProjMgr;

    private final ActivityResultLauncher<Intent> mProjLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    deliverToken(result.getResultCode(), result.getData());
                } else {
                    Toast.makeText(this, "Screen capture denied", Toast.LENGTH_SHORT).show();
                    updateUI(false); stopEverything();
                }
            });

    private final ActivityResultLauncher<String> mNotifLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), g -> requestProj());

    // ── Lifecycle ──────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hero area
        tvActivePresetName        = findViewById(R.id.tvActivePresetName);
        tvActivePresetDescription = findViewById(R.id.tvActivePresetDescription);
        statusDot                 = findViewById(R.id.statusDot);
        tvStatus                  = findViewById(R.id.tvStatus);

        // Tab bar
        tabPlay  = findViewById(R.id.tabPlay);
        tabTune  = findViewById(R.id.tabTune);
        tabInfo  = findViewById(R.id.tabInfo);
        panelPlay = findViewById(R.id.panelPlay);
        panelTune = findViewById(R.id.panelTune);
        panelInfo = findViewById(R.id.panelInfo);

        // Play tab
        btnToggle    = findViewById(R.id.btnToggle);
        presetScroll = findViewById(R.id.presetScroll);
        presetCarousel = findViewById(R.id.presetCarousel);

        // Tune tab
        tvLatencyValue = findViewById(R.id.tvLatencyValue);
        tvGammaValue   = findViewById(R.id.tvGammaValue);
        seekLatency    = findViewById(R.id.seekLatency);
        seekGamma      = findViewById(R.id.seekGamma);

        // Info tab
        tvDeviceLabel      = findViewById(R.id.tvDeviceLabel);
        spinnerDevice      = findViewById(R.id.spinnerDevice);
        btnGrantPermissions= findViewById(R.id.btnGrantPermissions);

        mProjMgr = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // ── Device detection & spinner ─────────────────────────────────────
        int detected = DeviceProfile.detectDevice();
        mSelectedDevice = detected != DeviceProfile.DEVICE_UNKNOWN
                ? detected : DeviceProfile.DEVICE_NP2;

        String[] deviceNames = {"Phone (1)","Phone (2)","Phone (2a) / 2a+","Phone (3a) / 3a Pro"};
        int[]    deviceIds   = {DeviceProfile.DEVICE_NP1, DeviceProfile.DEVICE_NP2,
                                DeviceProfile.DEVICE_NP2A, DeviceProfile.DEVICE_NP3A};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, deviceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDevice.setAdapter(adapter);
        for (int i = 0; i < deviceIds.length; i++)
            if (deviceIds[i] == mSelectedDevice) { spinnerDevice.setSelection(i); break; }

        tvDeviceLabel.setText(detected != DeviceProfile.DEVICE_UNKNOWN
                ? "Auto-detected: " + DeviceProfile.deviceName(detected)
                : "Device not detected — select manually");

        spinnerDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                int dev = deviceIds[pos];
                boolean changed = dev != mSelectedDevice;
                mSelectedDevice = dev;
                if (changed && mIsVisualizing) {
                    stopEverything(); updateUI(false);
                    Toast.makeText(MainActivity.this,"Device changed — tap Start",Toast.LENGTH_SHORT).show();
                }
                loadLatencyForDevice();
                reloadPresets();
                if (mBound && mService != null) {
                    mService.setDevice(mSelectedDevice);
                    if (!mSelectedPreset.isEmpty()) mService.setPreset(mSelectedPreset);
                    mService.setLatencyCompensationMs(mLatencyMs);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // ── Sliders ────────────────────────────────────────────────────────
        mGamma = AudioCaptureService.loadGamma(this);
        syncGammaUi(mGamma);
        seekGamma.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                float g = AudioCaptureService.gammaFromSliderProgress(p);
                syncGammaUi(g);
                if (!mApplyingGamma && user) applyGamma(g);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        seekLatency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int ms = AudioCaptureService.clampLatencyCompensationMs(p);
                syncLatencyUi(ms);
                if (!mApplyingLatency && user) applyLatency(ms);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // ── Permissions button ─────────────────────────────────────────────
        btnGrantPermissions.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    mNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    Toast.makeText(this, "Notification permission already granted ✓", Toast.LENGTH_SHORT).show();
                    btnGrantPermissions.setText("Notification Permission ✓");
                }
            } else {
                Toast.makeText(this, "No additional permissions needed on this OS version ✓", Toast.LENGTH_SHORT).show();
            }
        });

        // ── Tab switching ──────────────────────────────────────────────────
        tabPlay.setOnClickListener(v -> switchTab(0));
        tabTune.setOnClickListener(v -> switchTab(1));
        tabInfo.setOnClickListener(v -> switchTab(2));

        // ── Play ───────────────────────────────────────────────────────────
        btnToggle.setOnClickListener(v -> {
            if (mIsVisualizing) { stopEverything(); updateUI(false); }
            else requestStart();
        });

        loadLatencyForDevice();
        reloadPresets();
        updateUI(false);
        switchTab(0);
    }

    @Override protected void onDestroy() {
        if (mBound) { unbindService(mConn); mBound = false; }
        super.onDestroy();
    }

    // ── Tab switching ──────────────────────────────────────────────────────
    private void switchTab(int tab) {
        mActiveTab = tab;

        // Reset all tabs
        styleTab(tabPlay, false); styleTab(tabTune, false); styleTab(tabInfo, false);
        panelPlay.setVisibility(View.GONE);
        panelTune.setVisibility(View.GONE);
        panelInfo.setVisibility(View.GONE);

        switch (tab) {
            case 0: styleTab(tabPlay, true); panelPlay.setVisibility(View.VISIBLE); break;
            case 1: styleTab(tabTune, true); panelTune.setVisibility(View.VISIBLE); break;
            case 2: styleTab(tabInfo, true); panelInfo.setVisibility(View.VISIBLE); break;
        }
    }

    private void styleTab(TextView tab, boolean active) {
        tab.setTextColor(ContextCompat.getColor(this,
                active ? R.color.accent_white : R.color.text_secondary));
        tab.setBackgroundResource(active ? R.drawable.tab_selected_bg : android.R.color.transparent);
    }

    // ── Preset management ──────────────────────────────────────────────────
    private void reloadPresets() {
        List<AudioCaptureService.PresetInfo> list =
                AudioCaptureService.loadPresetInfos(this, mSelectedDevice);
        mPresetInfos.clear(); mPresetInfos.addAll(list);
        if (findPreset(mSelectedPreset) == null && !mPresetInfos.isEmpty())
            mSelectedPreset = mPresetInfos.get(0).key;
        buildPresetButtons();
        updatePresetCard();
        updateUI(mIsVisualizing);
    }

    private void buildPresetButtons() {
        presetCarousel.removeAllViews(); mPresetButtons.clear();
        int h = dp(42), gap = dp(8);
        for (AudioCaptureService.PresetInfo info : mPresetInfos) {
            MaterialButton btn = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, h);
            lp.setMarginEnd(gap);
            btn.setLayoutParams(lp);
            btn.setText(info.key); btn.setAllCaps(false); btn.setSingleLine(true);
            btn.setMinWidth(0); btn.setMinimumWidth(0);
            btn.setInsetTop(0); btn.setInsetBottom(0);
            btn.setCornerRadius(dp(21));
            btn.setPaddingRelative(dp(16),0,dp(16),0);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            btn.setTag(info.key);
            btn.setOnClickListener(v -> selectPreset(info.key));
            presetCarousel.addView(btn); mPresetButtons.add(btn);
        }
        updateButtonStates();
    }

    private void selectPreset(String key) {
        if (key == null || key.isEmpty()) return;
        boolean changed = !key.equals(mSelectedPreset);
        mSelectedPreset = key;
        updateButtonStates(); updatePresetCard(); updateUI(mIsVisualizing);
        if (changed && mBound && mService != null) mService.setPreset(key);
    }

    private void updateButtonStates() {
        MaterialButton sel = null;
        for (MaterialButton b : mPresetButtons) {
            boolean on = mSelectedPreset.equals(b.getTag());
            styleBtn(b, on);
            if (on) sel = b;
        }
        if (sel != null) {
            MaterialButton toCenter = sel;
            presetScroll.post(() -> {
                int target = Math.max(0, toCenter.getLeft()
                        - (presetScroll.getWidth() - toCenter.getWidth()) / 2);
                presetScroll.smoothScrollTo(target, 0);
            });
        }
    }

    private void styleBtn(MaterialButton b, boolean on) {
        b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                on ? R.color.accent_white : R.color.bg_dark)));
        b.setTextColor(ContextCompat.getColor(this,
                on ? R.color.bg_dark : R.color.accent_white));
        b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,
                on ? R.color.accent_white : R.color.text_muted)));
        b.setStrokeWidth(on ? 0 : dp(1));
    }

    private void updatePresetCard() {
        AudioCaptureService.PresetInfo info = findPreset(mSelectedPreset);
        if (info == null) {
            tvActivePresetName.setText("—");
            tvActivePresetDescription.setText("Select a preset to begin");
        } else {
            tvActivePresetName.setText(info.key);
            tvActivePresetDescription.setText(info.description);
        }
    }

    private AudioCaptureService.PresetInfo findPreset(String key) {
        for (AudioCaptureService.PresetInfo i : mPresetInfos)
            if (i.key.equals(key)) return i;
        return null;
    }

    // ── Capture flow ───────────────────────────────────────────────────────
    private void requestStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            mNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS); return;
        }
        requestProj();
    }

    private void requestProj() {
        mProjLauncher.launch(mProjMgr.createScreenCaptureIntent());
    }

    private void deliverToken(int code, Intent data) {
        Intent svc = new Intent(this, AudioCaptureService.class);
        svc.putExtra(AudioCaptureService.EXTRA_PRESET_KEY, mSelectedPreset);
        ContextCompat.startForegroundService(this, svc);
        if (mBound && mService != null) {
            mService.setDevice(mSelectedDevice);
            if (!mSelectedPreset.isEmpty()) mService.setPreset(mSelectedPreset);
            mService.setLatencyCompensationMs(mLatencyMs);
            mService.setGamma(mGamma);
            mService.startCapture(code, data);
        } else {
            mPending = code; mPendData = data; mHasTok = true;
            bindService(svc, mConn, Context.BIND_AUTO_CREATE);
        }
        updateUI(true);
    }

    private void stopEverything() {
        if (mBound && mService != null) mService.stopCapture();
        if (mBound) { unbindService(mConn); mBound=false; mService=null; }
        mPending=0; mPendData=null; mHasTok=false;
        stopService(new Intent(this, AudioCaptureService.class));
    }

    // ── Sliders ────────────────────────────────────────────────────────────
    private void loadLatencyForDevice() {
        int ms = AudioCaptureService.loadLatencyCompensationMs(this, mSelectedDevice);
        syncLatencyUi(ms);
    }

    private void applyLatency(int ms) {
        mLatencyMs = AudioCaptureService.clampLatencyCompensationMs(ms);
        AudioCaptureService.saveLatencyCompensationMs(this, mSelectedDevice, mLatencyMs);
        if (mBound && mService != null) mService.setLatencyCompensationMs(mLatencyMs);
    }

    private void applyGamma(float g) {
        mGamma = AudioCaptureService.clampGamma(g);
        AudioCaptureService.saveGamma(this, mGamma);
        if (mBound && mService != null) mService.setGamma(mGamma);
    }

    private void syncLatencyUi(int ms) {
        mLatencyMs = AudioCaptureService.clampLatencyCompensationMs(ms);
        tvLatencyValue.setText(String.format(Locale.US, "%d ms", mLatencyMs));
        if (seekLatency.getProgress() != mLatencyMs) {
            mApplyingLatency = true; seekLatency.setProgress(mLatencyMs); mApplyingLatency = false;
        }
    }

    private void syncGammaUi(float g) {
        mGamma = AudioCaptureService.clampGamma(g);
        tvGammaValue.setText(String.format(Locale.US, "%.2f", mGamma));
        int p = AudioCaptureService.gammaToSliderProgress(mGamma);
        if (seekGamma.getProgress() != p) {
            mApplyingGamma = true; seekGamma.setProgress(p); mApplyingGamma = false;
        }
    }

    // ── UI update ──────────────────────────────────────────────────────────
    private void updateUI(boolean active) {
        mIsVisualizing = active;
        TileService.requestListeningState(this,
                new ComponentName(this, VisualizerTileService.class));
        runOnUiThread(() -> {
            btnToggle.setText(active ? "STOP" : "START");
            btnToggle.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this,
                            active ? R.color.dot_active_color : R.color.accent_white)));
            btnToggle.setTextColor(ContextCompat.getColor(this, R.color.bg_dark));
            statusDot.setBackgroundResource(
                    active ? R.drawable.dot_active : R.drawable.dot_inactive);
            String label = mSelectedPreset.isEmpty() ? "No preset" : mSelectedPreset;
            tvStatus.setText(active ? label + " — listening…" : "Pick a preset and tap Start");
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
