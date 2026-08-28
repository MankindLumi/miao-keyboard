package com.xianyunb.miaokeyboard;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
/**
 * 猫猫悬浮窗服务：屏幕上一个可拖拽的悬浮按钮，点击后手动触发喵化。
 */
public class MiaoFloatingService extends Service {

    private WindowManager windowManager;
    private View floatView;
    private WindowManager.LayoutParams params;
    @Override
    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.floating_button, null);
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
        windowManager.addView(floatView, params);
        floatView.setOnTouchListener(new FloatingTouchListener());
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
        if (floatView != null && windowManager != null) {
            try {
                windowManager.removeView(floatView);
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }
}