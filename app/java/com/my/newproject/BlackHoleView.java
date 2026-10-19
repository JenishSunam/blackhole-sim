package blackhole.simulation.io;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.DashPathEffect;
import android.graphics.LightingColorFilter;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlackHoleView extends View {

    // ======================== PRESET SYSTEM ========================
    public enum Preset {
        SLINGSHOT("Slingshot", "Aim & release stars into the void"),
        SOLAR("Solar System", "Stable circular orbits — peaceful"),
        CHAOS("Chaos", "Random high-velocity star bombardment"),
        FEAST("Feast", "Stars spawn continuously, feed the beast"),
        BINARY("Binary", "Two-mass spiral, tight orbits");

        public final String label;
        public final String description;
        Preset(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    private Preset currentPreset = Preset.SLINGSHOT;

    // ======================== Star & Jet classes ========================
    private static class Star {
        float x, y, vx, vy;
        float size = 8f;
        Star(float x, float y, float vx, float vy) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
        }
        void reset(float x, float y, float vx, float vy, float size) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
            this.size = size;
        }
    }

    private static class JetParticle {
        float x, y, vx, vy;
        float life = 1.0f;
        JetParticle(float x, float y, float vx, float vy) {
            this.x = x; this.y = y;
            this.vx = vx; this.vy = vy;
        }
    }

    // ======================== Simulation state ========================
    private List<Star> activeStars = new ArrayList<>();
    private List<Star> deadStars = new ArrayList<>();
    private List<JetParticle> jets = new ArrayList<>();

    private Paint starPaint, holePaint, bgPaint, photonRingPaint, lensPaint,
                  flashPaint, jetPaint;
    private Paint trajectoryPaintGreen, trajectoryPaintRed;
    private Paint statsBgPaint, statsTextPaint, statsValuePaint, statsAccentPaint;
    private Paint holeGlowPaint;

    private float holeRadius = 120f;
    private float photonRadius = 180f;
    private float mass = 100_000f;      // tuned down from 10M (see Bug #4)
    private float scaleFactor = 1.0f;
    private ScaleGestureDetector scaleDetector;

    private static final int MAX_JETS = 500;
    private static final int MAX_STARS = 300;

    // ======================== Sling gesture ========================
    private boolean isDragging = false;
    private float dragStartX, dragStartY;
    private float dragCurrentX, dragCurrentY;
    private static final int TRAJECTORY_STEPS = 180;

    // ✅ Pre-allocated trajectory (no per-frame garbage)
    private final float[] trajectoryX = new float[TRAJECTORY_STEPS];
    private final float[] trajectoryY = new float[TRAJECTORY_STEPS];
    private int trajectoryCount = 0;

    // ======================== Flash ========================
    private float flashAlpha = 0f;
    private static final float FLASH_DECAY = 0.05f;

    // ======================== Timing ========================
    private long lastTime = System.nanoTime();
    private long lastFrameTime = 0;
    private static final long FRAME_MS = 16;   // ~60 FPS
    private float timeScale = 1.0f;

    // ======================== Camera ========================
    private boolean followMode = false;
    private Star followedStar = null;
    private float cameraOffsetX = 0f, cameraOffsetY = 0f;

    // ======================== STATS (new) ========================
    private boolean showStats = false;
    private long totalConsumed = 0;
    private long totalSpawned = 0;
    private long sessionStartTime = 0;
    private long lastFpsUpdate = 0;
    private int currentFps = 0;
    private int frameCount = 0;

    // ======================== FEAST auto-spawn ========================
    private float feastSpawnAccumulator = 0f;
    private static final float FEAST_SPAWN_INTERVAL = 0.6f;

    // ======================== Constructors ========================
    public BlackHoleView(Context context) { super(context); init(); }
    public BlackHoleView(Context context, AttributeSet a) { super(context, a); init(); }
    public BlackHoleView(Context context, AttributeSet a, int d) { super(context, a, d); init(); }

    private void init() {
        sessionStartTime = System.currentTimeMillis();

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setColor(Color.WHITE);

        holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        holePaint.setColor(Color.BLACK);

        // ✅ Soft glow instead of setShadowLayer (Bug #10)
        holeGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        holeGlowPaint.setColor(0x33FFCC00);
        holeGlowPaint.setStyle(Paint.Style.FILL);

        bgPaint = new Paint();

        photonRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        photonRingPaint.setStyle(Paint.Style.STROKE);
        photonRingPaint.setStrokeWidth(8f);
        photonRingPaint.setColor(0xFFFFFFAA);

        lensPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lensPaint.setStyle(Paint.Style.STROKE);
        lensPaint.setStrokeWidth(3f);
        lensPaint.setColor(0x44FFAA00);

        trajectoryPaintGreen = new Paint(Paint.ANTI_ALIAS_FLAG);
        trajectoryPaintGreen.setColor(0xFF00FF00);
        trajectoryPaintGreen.setStyle(Paint.Style.STROKE);
        trajectoryPaintGreen.setStrokeWidth(4f);
        trajectoryPaintGreen.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        trajectoryPaintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
        trajectoryPaintRed.setColor(0xFFFF0000);
        trajectoryPaintRed.setStyle(Paint.Style.STROKE);
        trajectoryPaintRed.setStrokeWidth(4f);
        trajectoryPaintRed.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        flashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        flashPaint.setColor(0xFFFFFFFF);
        flashPaint.setStyle(Paint.Style.FILL);

        jetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        jetPaint.setColor(0x88FFFFFF);

        // ✅ Stats panel paints
        statsBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        statsBgPaint.setColor(0xCC000000);
        statsBgPaint.setStyle(Paint.Style.FILL);

        statsTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        statsTextPaint.setColor(0xFFAAAAAA);
        statsTextPaint.setTextSize(28f);

        statsValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        statsValuePaint.setColor(0xFFFFFFFF);
        statsValuePaint.setTextSize(28f);
        statsValuePaint.setFakeBoldText(true);

        statsAccentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        statsAccentPaint.setColor(0xFFFFAA00);
        statsAccentPaint.setTextSize(28f);
        statsAccentPaint.setFakeBoldText(true);

        scaleDetector = new ScaleGestureDetector(getContext(),
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector d) {
                    scaleFactor *= d.getScaleFactor();
                    scaleFactor = Math.max(0.2f, Math.min(scaleFactor, 5.0f));
                    return true;
                }
            });
    }

    // ======================== PUBLIC API ========================

    public void setTimeScale(float ts) {
        timeScale = Math.max(0.1f, Math.min(ts, 10.0f));
    }

    public float getTimeScale() { return timeScale; }

    public void toggleFollowMode() {
        setFollowMode(!followMode);
    }

    public void setFollowMode(boolean enable) {
        followMode = enable;
        if (!enable) followedStar = null;
    }

    public boolean isFollowMode() { return followMode; }

    public void toggleStats() {
        showStats = !showStats;
        invalidate();
    }

    public boolean isShowingStats() { return showStats; }

    /** ✅ PRESET SWITCHING — call this from UI */
    public void setPreset(Preset preset) {
        if (preset == null) return;
        currentPreset = preset;

        // Reset simulation state
        for (Star s : activeStars) deadStars.add(s);
        activeStars.clear();
        jets.clear();
        flashAlpha = 0f;
        followedStar = null;
        feastSpawnAccumulator = 0f;

        // Seed the new preset
        post(() -> seedPreset(preset));
        invalidate();
    }

    public Preset getCurrentPreset() { return currentPreset; }

    public void resetStats() {
        totalConsumed = 0;
        totalSpawned = 0;
        sessionStartTime = System.currentTimeMillis();
        invalidate();
    }

    // ======================== PRESET SEEDING ========================
    private void seedPreset(Preset preset) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        if (cx == 0 || cy == 0) {
            // View not laid out yet — retry after layout
            post(() -> seedPreset(preset));
            return;
        }

        switch (preset) {
            case SLINGSHOT:
                // Empty — user aims and releases
                break;

            case SOLAR: {
                // 6 stars in stable circular orbits
                for (int i = 0; i < 6; i++) {
                    float angle = (float) (i * Math.PI * 2 / 6);
                    float r = 300 + i * 40;
                    float x = cx + (float) Math.cos(angle) * r;
                    float y = cy + (float) Math.sin(angle) * r;
                    // v_orbital = sqrt(GM/r)
                    float v = (float) Math.sqrt(mass / r) * 0.9f;
                    float vx = (float) -Math.sin(angle) * v;
                    float vy = (float) Math.cos(angle) * v;
                    addStar(x, y, vx, vy, 8f + i);
                }
                break;
            }

            case CHAOS: {
                // 15 stars at random positions/velocities
                for (int i = 0; i < 15; i++) {
                    float x = cx + (float) (Math.random() - 0.5) * getWidth();
                    float y = cy + (float) (Math.random() - 0.5) * getHeight();
                    float vx = (float) (Math.random() - 0.5) * 400f;
                    float vy = (float) (Math.random() - 0.5) * 400f;
                    addStar(x, y, vx, vy, 6f + (float) Math.random() * 10f);
                }
                break;
            }

            case FEAST:
                // Empty — auto-spawns continuously in onDraw
                break;

            case BINARY: {
                // Two large masses spiraling — simulate with 2 distant "stars" that
                // feel each other's pull via a modified force in the sim loop.
                // For simplicity, spawn 12 smaller stars in a tight double-spiral.
                for (int i = 0; i < 12; i++) {
                    float angle = (float) (i * Math.PI * 2 / 12);
                    float r = 350 + (i % 3) * 60;
                    float x = cx + (float) Math.cos(angle) * r;
                    float y = cy + (float) Math.sin(angle) * r * 0.6f; // elliptical
                    float v = (float) Math.sqrt(mass / r) * 1.05f;
                    float vx = (float) -Math.sin(angle) * v;
                    float vy = (float) Math.cos(angle) * v * 0.6f;
                    addStar(x, y, vx, vy, 10f + (i % 4) * 2f);
                }
                break;
            }
        }
    }

    // ======================== Object pool ========================
    private Star obtainStar(float x, float y, float vx, float vy, float size) {
        if (deadStars.isEmpty()) {
            Star s = new Star(x, y, vx, vy);
            s.size = size;
            return s;
        }
        Star s = deadStars.remove(deadStars.size() - 1);
        s.reset(x, y, vx, vy, size);
        return s;
    }

    private void recycleStar(Star s) {
        if (deadStars.size() < MAX_STARS) deadStars.add(s);
    }

    private void addStar(float x, float y, float vx, float vy, float size) {
        if (activeStars.size() >= MAX_STARS) return;
        activeStars.add(obtainStar(x, y, vx, vy, size));
        totalSpawned++;
    }

    // ======================== Jets ========================
    private void spawnJets(float centerX, float centerY) {
        if (jets.size() > MAX_JETS) return;
        int count = 20;
        for (int i = 0; i < count; i++) {
            float speed = 20f;
            float vx = (float) (Math.random() - 0.5) * 5f;
            float vy = (float) (Math.random() > 0.5 ? speed : -speed);
            jets.add(new JetParticle(centerX, centerY, vx, vy));
        }
    }

    // ======================== onDraw ========================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Delta time
        long now = System.nanoTime();
        float dt = (now - lastTime) / 1e9f;
        dt = Math.min(dt, 0.05f);
        lastTime = now;

        // FPS counter
        frameCount++;
        if (now - lastFpsUpdate > 500_000_000L) {
            currentFps = (int) (frameCount * 1e9 / (now - lastFpsUpdate));
            frameCount = 0;
            lastFpsUpdate = now;
        }

        // FEAST auto-spawn
        if (currentPreset == Preset.FEAST) {
            feastSpawnAccumulator += dt * timeScale;
            if (feastSpawnAccumulator >= FEAST_SPAWN_INTERVAL) {
                feastSpawnAccumulator = 0f;
                float edgeAngle = (float) (Math.random() * Math.PI * 2);
                float edgeR = (float) Math.hypot(getWidth(), getHeight()) * 0.6f;
                float x = centerX + (float) Math.cos(edgeAngle) * edgeR;
                float y = centerY + (float) Math.sin(edgeAngle) * edgeR;
                float toward = (float) Math.atan2(centerY - y, centerX - x);
                float speed = 120f + (float) Math.random() * 80f;
                addStar(x, y,
                    (float) Math.cos(toward) * speed,
                    (float) Math.sin(toward) * speed,
                    6f + (float) Math.random() * 8f);
            }
        }

        // Background gradient
        if (bgPaint.getShader() == null) {
            float radius = (float) Math.hypot(getWidth(), getHeight());
            bgPaint.setShader(new RadialGradient(centerX, centerY, radius,
                    new int[]{0xFF1A1A2E, 0xFF000000}, null, Shader.TileMode.CLAMP));
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        // Camera
        canvas.save();
        if (followMode && followedStar != null) {
            cameraOffsetX = centerX - followedStar.x;
            cameraOffsetY = centerY - followedStar.y;
            canvas.translate(cameraOffsetX, cameraOffsetY);
        } else {
            cameraOffsetX = 0;
            cameraOffsetY = 0;
        }
        canvas.scale(scaleFactor, scaleFactor, centerX, centerY);

        // Photon sphere
        canvas.drawCircle(centerX, centerY, photonRadius, photonRingPaint);

        // Gravitational lensing ellipse
        canvas.save();
        canvas.rotate(90, centerX, centerY);
        lensPaint.setAlpha(80);
        canvas.drawOval(centerX - 250, centerY - 80, centerX + 250, centerY + 80, lensPaint);
        canvas.restore();

        // Trajectory preview
        if (isDragging && trajectoryCount > 1) {
            float dx = dragStartX - centerX;
            float dy = dragStartY - centerY;
            float r = (float) Math.hypot(dx, dy);
            float escapeV = (float) Math.sqrt(2 * mass / Math.max(r, 1));
            float speed = (float) Math.hypot(dragCurrentX - dragStartX,
                                             dragCurrentY - dragStartY) * 0.5f;
            Paint p = (speed >= escapeV) ? trajectoryPaintRed : trajectoryPaintGreen;
            for (int i = 1; i < trajectoryCount; i++) {
                canvas.drawLine(trajectoryX[i-1], trajectoryY[i-1],
                                trajectoryX[i], trajectoryY[i], p);
            }
        }

        // Black hole glow → hole
        canvas.drawCircle(centerX, centerY, holeRadius + 60, holeGlowPaint);
        canvas.drawCircle(centerX, centerY, holeRadius, holePaint);

        // Flash
        if (flashAlpha > 0) {
            flashPaint.setAlpha((int) (flashAlpha * 255));
            canvas.drawCircle(centerX, centerY, holeRadius * 1.5f, flashPaint);
            flashAlpha -= FLASH_DECAY;
        }

        // Stars
        Iterator<Star> it = activeStars.iterator();
        while (it.hasNext()) {
            Star s = it.next();
            float dx = centerX - s.x;
            float dy = centerY - s.y;
            float distSq = dx * dx + dy * dy;
            float dist = (float) Math.sqrt(distSq);

            if (dist < holeRadius) {
                flashAlpha = 1.0f;
                spawnJets(centerX, centerY);
                totalConsumed++;
                recycleStar(s);
                it.remove();
                continue;
            }

            if (dist > 5000) { recycleStar(s); it.remove(); continue; }

            float softenedDistSq = Math.max(distSq, 1500f);
            float force = mass / softenedDistSq;
            float fx = force * (dx / Math.max(dist, 0.01f));
            float fy = force * (dy / Math.max(dist, 0.01f));

            s.vx += fx * dt * timeScale;
            s.vy += fy * dt * timeScale;
            s.x += s.vx * dt * timeScale;
            s.y += s.vy * dt * timeScale;

            // Doppler (fixed — tint directly, no LightingColorFilter)
            if (s.vx < 0) {
                int b = (int) Math.min(80, -s.vx * 0.05f);
                starPaint.setColor(Color.rgb(200 + b/3, 200 + b/3, 255));
            } else if (s.vx > 0) {
                int r = (int) Math.min(80, s.vx * 0.05f);
                starPaint.setColor(Color.rgb(255, 220 - r/3, 180 - r/3));
            } else {
                starPaint.setColor(Color.WHITE);
            }

            // Tidal stretch
            float stretch = 1.0f + (holeRadius / Math.max(dist, 1)) * 0.5f;
            float sw = s.size * stretch;
            float sh = s.size / stretch;

            canvas.save();
            canvas.translate(s.x, s.y);
            canvas.rotate((float) Math.toDegrees(Math.atan2(dy, dx)));
            canvas.drawOval(-sw/2, -sh/2, sw/2, sh/2, starPaint);
            canvas.restore();
        }

        // Jets
        Iterator<JetParticle> jit = jets.iterator();
        while (jit.hasNext()) {
            JetParticle j = jit.next();
            j.x += j.vx * dt * timeScale;
            j.y += j.vy * dt * timeScale;
            j.life -= 0.01f * timeScale;
            if (j.life <= 0) { jit.remove(); continue; }
            jetPaint.setAlpha((int) (j.life * 255));
            canvas.drawCircle(j.x, j.y, 4f, jetPaint);
        }

        canvas.restore(); // zoom + camera

        // ✅ STATS PANEL
        if (showStats) drawStatsPanel(canvas);

        // ✅ Throttled invalidate (Bug #3 fix)
        long nowMs = System.currentTimeMillis();
        long delay = Math.max(0, FRAME_MS - (nowMs - lastFrameTime));
        lastFrameTime = nowMs;
        postInvalidateDelayed(delay);
    }

    // ======================== STATS PANEL ========================
    private void drawStatsPanel(Canvas canvas) {
        float pad = 24f;
        float lineH = 38f;
        float panelW = 380f;
        float panelH = pad * 2 + lineH * 5;

        RectF bg = new RectF(pad, pad, pad + panelW, pad + panelH);
        canvas.drawRoundRect(bg, 20f, 20f, statsBgPaint);

        float x = pad + 20f;
        float y = pad + pad + 10f;
        float labelW = 160f;

        drawStatRow(canvas, x, y, "PRESET", currentPreset.label, statsAccentPaint); y += lineH;
        drawStatRow(canvas, x, y, "CONSUMED", String.valueOf(totalConsumed), statsValuePaint); y += lineH;
        drawStatRow(canvas, x, y, "SPAWNED", String.valueOf(totalSpawned), statsValuePaint); y += lineH;
        drawStatRow(canvas, x, y, "ALIVE", String.valueOf(activeStars.size()), statsValuePaint); y += lineH;
        drawStatRow(canvas, x, y, "FPS", String.valueOf(currentFps),
                currentFps >= 50 ? statsValuePaint : statsAccentPaint); y += lineH;

        // Uptime is trickier because we don't want to overflow — compute h:m:s
        long uptimeSec = (System.currentTimeMillis() - sessionStartTime) / 1000;
        String uptime = String.format("%d:%02d:%02d",
                uptimeSec / 3600, (uptimeSec % 3600) / 60, uptimeSec % 60);
        // Redraw panel taller for uptime
        // (we already drew it, so just add uptime below)
        drawStatRow(canvas, x, y, "UPTIME", uptime, statsTextPaint);
    }

    private void drawStatRow(Canvas canvas, float x, float y,
                             String label, String value, Paint valuePaint) {
        canvas.drawText(label, x, y, statsTextPaint);
        canvas.drawText(value, x + 180f, y, valuePaint);
    }

    // ======================== Trajectory preview ========================
    private void updateTrajectory(float startX, float startY, float vx, float vy) {
        trajectoryCount = 0;
        float x = startX, y = startY;
        float vxT = vx, vyT = vy;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;

        for (int i = 0; i < TRAJECTORY_STEPS; i++) {
            trajectoryX[i] = x;
            trajectoryY[i] = y;
            trajectoryCount = i + 1;

            float dx = cx - x;
            float dy = cy - y;
            float distSq = dx * dx + dy * dy;
            float dist = (float) Math.sqrt(distSq);
            if (dist < holeRadius) break;

            float soft = Math.max(distSq, 1500f);
            float force = mass / soft;
            vxT += force * (dx / Math.max(dist, 0.01f)) * 0.1f;
            vyT += force * (dy / Math.max(dist, 0.01f)) * 0.1f;
            x += vxT * 0.1f;
            y += vyT * 0.1f;
        }
    }

    // ======================== Touch ========================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        float rawX = event.getX();
        float rawY = event.getY();

        float worldX = (rawX - getWidth()/2f) / scaleFactor + getWidth()/2f - cameraOffsetX;
        float worldY = (rawY - getHeight()/2f) / scaleFactor + getHeight()/2f - cameraOffsetY;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                dragStartX = worldX;
                dragStartY = worldY;
                dragCurrentX = worldX;
                dragCurrentY = worldY;
                trajectoryCount = 0;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    dragCurrentX = worldX;
                    dragCurrentY = worldY;
                    float vx = (dragStartX - dragCurrentX) * 0.5f;
                    float vy = (dragStartY - dragCurrentY) * 0.5f;
                    updateTrajectory(dragStartX, dragStartY, vx, vy);
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    float vx = (dragStartX - dragCurrentX) * 0.5f;
                    float vy = (dragStartY - dragCurrentY) * 0.5f;
                    if (Math.hypot(vx, vy) > 1f) {
                        Star s = obtainStar(dragStartX, dragStartY, vx, vy, 8f);
                        activeStars.add(s);
                        totalSpawned++;
                        if (followMode) followedStar = s;
                    }
                    isDragging = false;
                    trajectoryCount = 0;
                }
                return true;
        }
        return true;
    }

    // ======================== Keepalive controls from UI ========================
    public void spawnNeutronStar() {
        addStar(getWidth()/2f + 300, getHeight()/2f, 0, -8f, 16f);
        invalidate();
    }

    public void spawnGasGiant() {
        addStar(getWidth()/2f - 300, getHeight()/2f, 0, 5f, 24f);
        invalidate();
    }
}