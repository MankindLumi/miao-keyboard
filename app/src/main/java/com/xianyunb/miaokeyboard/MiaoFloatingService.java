package com.xianyunb.miaokeyboard;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 猫猫悬浮窗服务：屏幕上一个可拖拽的悬浮按钮，点击后手动触发喵化。
 * 支持自定义透明度 / 图标 / 颜色 / 尺寸。
 */
public class MiaoFloatingService extends Service {
    private static MiaoFloatingService instance;
    private WindowManager windowManager;
    private View floatView;
    private TextView floatBtn;
    private WindowManager.LayoutParams params;
    private boolean added = false;

    public static MiaoFloatingService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.floating_button, null);
        floatBtn = floatView.findViewById(R.id.float_btn);
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 300;
        applyStyle();
        windowManager.addView(floatView, params);
        added = true;
        floatView.setOnTouchListener(new FloatingTouchListener());
    }

    /** 应用透明度 / 图标 / 颜色 / 尺寸（首次或刷新时调用）。 */
    public void applyStyle() {
        if (floatView == null || floatBtn == null) return;
        MiaoPrefs prefs = new MiaoPrefs(this);
        float alpha = prefs.getFloatAlpha();
        String text = prefs.getFloatText();
        int color = prefs.getFloatColor();
        int size = prefs.getFloatSize();

        float density = getResources().getDisplayMetrics().density;
        int px = Math.round(size * density);
        if (params != null) {
            params.alpha = alpha;
            // 悬浮窗整体尺寸由 WindowManager.LayoutParams 控制
            params.width = px;
            params.height = px;
        }
        // 图标
        if (text == null || text.trim().isEmpty()) text = "🐾";
        floatBtn.setText(text);
        floatBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, size * 0.46f);
        // 背景（动态渐变）
        View container = floatView.findViewById(R.id.float_container);
        if (container != null) {
            container.setBackground(buildBackground(color));
        }
        if (added && windowManager != null) {
            windowManager.updateViewLayout(floatView, params);
        }
    }

    /** 根据主色生成「渐变圆 + 白描边 + 顶部高光」的玻璃质感背景。 */
    private Drawable buildBackground(int color) {
        float d = getResources().getDisplayMetrics().density;
        GradientDrawable main = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{lighten(color, 0.20f), color, darken(color, 0.10f)});
        main.setShape(GradientDrawable.OVAL);
        main.setStroke(Math.round(2 * d), Color.WHITE);

        GradientDrawable highlight = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x99FFFFFF, 0x00FFFFFF});
        highlight.setShape(GradientDrawable.OVAL);

        LayerDrawable layer = new LayerDrawable(new Drawable[]{main, highlight});
        layer.setLayerInset(1, Math.round(10 * d), Math.round(7 * d),
                Math.round(10 * d), Math.round(20 * d));
        return layer;
    }

    private int lighten(int color, float f) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        r = (int) (r + (255 - r) * f);
        g = (int) (g + (255 - g) * f);
        b = (int) (b + (255 - b) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int darken(int color, float f) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        r = (int) (r * (1 - f));
        g = (int) (g * (1 - f));
        b = (int) (b * (1 - f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onFloatClick() {
        MiaoPrefs prefs = new MiaoPrefs(this);
        if (!prefs.isMasterOn()) {
            Toast.makeText(this, "总开关已关闭喵~", Toast.LENGTH_SHORT).show();
            return;
        }
        MiaoAccessibilityService svc = MiaoAccessibilityService.getInstance();
        if (svc == null) {
            Toast.makeText(this, "请先开启无障碍服务喵~", Toast.LENGTH_SHORT).show();
            return;
        }
        svc.triggerManualMiao();
    }

    /** 区分点击与拖拽：移动距离小视为点击，否则拖动悬浮窗。 */
    private class FloatingTouchListener implements View.OnTouchListener {
        private int startX, startY;
        private float downX, downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = params.x;
                    startY = params.y;
                    downX = event.getRawX();
                    downY = event.getRawY();
                    floatView.animate().scaleX(0.88f).scaleY(0.88f).setDuration(120).start();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = startX + (int) (event.getRawX() - downX);
                    params.y = startY + (int) (event.getRawY() - downY);
                    windowManager.updateViewLayout(floatView, params);
                    return true;
                case MotionEvent.ACTION_UP:
                    floatView.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    if (Math.abs(dx) < 10 && Math.abs(dy) < 10) {
                        onFloatClick();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    floatView.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    return true;
            }
            return false;
        }
    }

    @Override
    public void onDestroy() {
        instance = null;
        added = false;
        if (floatView != null && windowManager != null) {
            try {
                windowManager.removeView(floatView);
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }
}