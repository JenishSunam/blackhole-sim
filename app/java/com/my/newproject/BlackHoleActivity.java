package blackhole.simulation.io;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

public class BlackHoleActivity extends Activity {

    private BlackHoleView blackHoleView;
    private TextView tvTimeScale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        buildUI();
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF000000);

        // ══════════════ BLACK HOLE VIEW ══════════════
        blackHoleView = new BlackHoleView(this);
        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        blackHoleView.setLayoutParams(viewParams);
        root.addView(blackHoleView);

        // ══════════════ PRESET BUTTONS ROW ══════════════
        HorizontalScrollView hScroll = new HorizontalScrollView(this);
        hScroll.setBackgroundColor(0xFF111111);
        hScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        presetRow.setPadding(12, 12, 12, 12);

        for (final BlackHoleView.Preset p : BlackHoleView.Preset.values()) {
            Button btn = makeButton(p.label, 0xFF2196F3);
            btn.setTextSize(11);
            btn.setOnClickListener(v -> {
                blackHoleView.setPreset(p);
            });
            presetRow.addView(btn);
        }

        hScroll.addView(presetRow);
        root.addView(hScroll);

        // ══════════════ CONTROL ROW ══════════════
        LinearLayout controlRow = new LinearLayout(this);
        controlRow.setOrientation(LinearLayout.HORIZONTAL);
        controlRow.setPadding(12, 12, 12, 12);
        controlRow.setBackgroundColor(0xFF0A0A0A);
        controlRow.setGravity(Gravity.CENTER);

        // Stats toggle
        Button btnStats = makeButton("📊 STATS", 0xFF4CAF50);
        btnStats.setOnClickListener(v -> blackHoleView.toggleStats());
        controlRow.addView(btnStats);

        // Follow toggle
        Button btnFollow = makeButton("👁 FOLLOW", 0xFF9C27B0);
        btnFollow.setOnClickListener(v -> blackHoleView.toggleFollowMode());
        controlRow.addView(btnFollow);

        // Reset
        Button btnReset = makeButton("🔄 RESET", 0xFFF44336);
        btnReset.setOnClickListener(v -> {
            blackHoleView.setPreset(blackHoleView.getCurrentPreset());
            blackHoleView.resetStats();
        });
        controlRow.addView(btnReset);

        root.addView(controlRow);

        // ══════════════ TIME SCALE ROW ══════════════
        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setPadding(20, 8, 20, 20);
        timeRow.setBackgroundColor(0xFF0A0A0A);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText("⏱ TIME: ");
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(14);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        timeRow.addView(label);

        tvTimeScale = new TextView(this);
        tvTimeScale.setText("1.0x");
        tvTimeScale.setTextColor(0xFFFFAA00);
        tvTimeScale.setTextSize(14);
        tvTimeScale.setTypeface(Typeface.DEFAULT_BOLD);
        tvTimeScale.setPadding(0, 0, 20, 0);
        timeRow.addView(tvTimeScale);

        SeekBar timeSlider = new SeekBar(this);
        timeSlider.setMax(100);  // 0.1x - 10x = 100 steps
        timeSlider.setProgress(10);  // 1.0x
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        timeSlider.setLayoutParams(sliderParams);
        timeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int v, boolean b) {
                float ts = 0.1f + (v / 100f) * 9.9f;
                blackHoleView.setTimeScale(ts);
                tvTimeScale.setText(String.format("%.1fx", ts));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        timeRow.addView(timeSlider);

        root.addView(timeRow);

        setContentView(root);
    }

    private Button makeButton(String text, int color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(12);
        btn.setAllCaps(false);
        btn.setPadding(20, 10, 20, 10);
        btn.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(12);
        btn.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(6, 0, 6, 0);
        btn.setLayoutParams(params);

        return btn;
    }
}