package git.mojo.sdl;

import static android.content.Context.UI_MODE_SERVICE;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SDLActivity {

    private static GrabListener grabListener;
    private static Map<Integer, SDLCursor> customCursors = new HashMap<>();
    private static SDLCursor.CursorChangeCallback cursorCallback;
    private static int lastCursorId = 0;
    private static Runnable initCallback;

    protected static Surface mSurface;
    protected static Activity mContext;

    private static SDLClipboard mClipboard;
    private static boolean dynamicOrientationEnabled = false;

    public static void initialize() {
        mSurface = null;
    }

    public static void setClipboard(SDLClipboard clipboard){
        mClipboard = clipboard;
    }
    public static void setCursorCallback(SDLCursor.CursorChangeCallback callback){
        cursorCallback = callback;
    }
    public static void setGrabListener(GrabListener grabListener){
        SDLActivity.grabListener = grabListener;
    }

    public static void setNativeSurface(Surface surface){
        SDLActivity.mSurface = surface;
    }
    public static void setInitCallback(Runnable callback){
        initCallback = callback;
    }

    // Native method declarations
    public static native String nativeGetVersion();
    public static native void nativeSetupJNI();
    public static native int nativeGetCompiledSubsystems();
    public static native boolean nativeIsHIDAPIEnabled();
    public static native void nativeInitMainThread();
    public static native void nativeCleanupMainThread();
    public static native int nativeRunMain(String library, String function, Object arguments);
    public static native void nativeLowMemory();
    public static native void nativeSendQuit();
    public static native void nativeQuit();
    public static native void nativePause();
    public static native void nativeResume();
    public static native void nativeFocusChanged(boolean hasFocus);
    public static native void nativeVisibilityChanged(boolean visibility);
    public static native void onNativeDropFile(String filename);
    public static native void nativeSetScreenResolution(int surfaceWidth, int surfaceHeight, int deviceWidth, int deviceHeight, float density, float rate);
    public static native void onNativeResize();
    public static native boolean onNativeKeyDown(int keycode);
    public static native boolean onNativeKeyUp(int keycode);
    public static native boolean onNativeSoftReturnKey();
    public static native void onNativeKeyboardFocusLost();
    public static native void onNativeMouse(int button, int action, float x, float y, boolean relative);
    public static native void onNativeMouseButton(int button, int action, float x, float y, boolean relative);
    public static native void onNativeTouch(int touchDevId, int pointerFingerId,
                                            int action, float x,
                                            float y, float p);
    public static native void onNativePen(int penId, int device_type, int button, int action, float x, float y, float p);
    public static native void onNativeClipboardChanged();
    public static native void onNativeSurfaceCreated();
    public static native void onNativeSurfaceChanged();
    public static native void onNativeSurfaceDestroyed();
    public static native void onNativeScreenKeyboardShown();
    public static native void onNativeScreenKeyboardHidden();
    public static native String nativeGetHint(String name);
    public static native boolean nativeGetHintBoolean(String name, boolean default_value);
    public static native void nativeSetenv(String name, String value);
    public static native void nativeSetNaturalOrientation(int orientation);
    public static native void onNativeRotationChanged(int rotation);
    public static native void onNativeInsetsChanged(int left, int right, int top, int bottom);
    public static native void nativeAddTouch(int touchId, String name);
    public static native void nativePermissionResult(int requestCode, boolean result);
    public static native void onNativeLocaleChanged();
    public static native void onNativeDarkModeChanged(boolean enabled);
    public static native boolean nativeAllowRecreateActivity();
    public static native int nativeCheckSDLThreadCounter();
    public static native void onNativeFileDialog(int requestCode, String[] filelist, int filter);
    public static native void onNativePinchStart(float span_x, float span_y, float focus_x, float focus_y);
    public static native void onNativePinchUpdate(float scale, float span_x, float span_y, float focus_x, float focus_y);
    public static native void onNativePinchEnd();
    public static void onSDLInit(){
        Log.i("SDLInit", "SDL init called!");
        if(initCallback != null) initCallback.run();
    }

    public static Surface getNativeSurface() {
        return mSurface;
    }

    public static Activity getContext() {
        return SDL.getContext();
    }

    public static void manualBackButton() {
        if (getContext() != null) {
            getContext().onBackPressed();
        }
    }

    public static void setDynamicOrientationEnabled(boolean enabled) {
        dynamicOrientationEnabled = enabled;
    }

    public static void setOrientation(int w, int h, boolean resizable, String hint) {
        if (getContext() == null || !dynamicOrientationEnabled) {
            return;
        }

        int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if (w > h) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        } else if (h > w) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        }

        final int finalOrientation = orientation;
        getContext().runOnUiThread(() -> getContext().setRequestedOrientation(finalOrientation));
    }

    public static boolean shouldMinimizeOnFocusLoss() {
        return false;
    }

    public static boolean supportsRelativeMouse() {
        return true;
    }

    public static boolean setRelativeMouseEnabled(boolean enabled) {
        if (grabListener != null) grabListener.onGrabState(enabled);
        return true;
    }

    public static void initTouch() {
        Log.i("SDL", "initTouch called");
    }

    public static boolean clipboardHasText() {
        return mClipboard != null && mClipboard.getClipboardString() != null;
    }

    public static String clipboardGetText() {
        return mClipboard != null ? mClipboard.getClipboardString() : "";
    }

    public static void clipboardSetText(String string) {
        if(mClipboard != null) mClipboard.setClipboardString(string);
    }

    public static int createCustomCursor(int[] colors, int width, int height, int hotSpotX, int hotSpotY) {
        Bitmap bitmap = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
        SDLCursor cursor = new SDLCursor(width, height, hotSpotX, hotSpotY, bitmap);
        lastCursorId++;
        customCursors.put(lastCursorId, cursor);
        return lastCursorId;
    }

    public static void destroyCustomCursor(int cursorID) {
        customCursors.remove(cursorID);
    }

    public static boolean setCustomCursor(int cursorID) {
        if(!customCursors.containsKey(cursorID)) return false;
        if (cursorCallback != null) cursorCallback.onCursorChange(customCursors.get(cursorID));
        return true;
    }

    public static boolean setSystemCursor(int cursorID) {
        if (cursorCallback != null) cursorCallback.onCursorChange(null);
        return true;
    }

    public static void requestPermission(String permission, int requestCode) {
        if (getContext() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getContext().requestPermissions(new String[]{permission}, requestCode);
        }
    }

    public static boolean openURL(String url)
    {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));

            int flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                | Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
            i.addFlags(flags);

            if (mContext != null) mContext.startActivity(i);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    static String getDeviceFormFactor() {
        if (getContext() == null) return "phone";
        Configuration config = getContext().getResources().getConfiguration();
        if ((config.uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_WATCH) {
            return "wearable";
        }
        if (isAndroidTV()) {
            return "tv";
        } else if (isVRHeadset()) {
            return "headset";
        } else if (isTablet()) {
            return "tablet";
        } else {
            return "phone";
        }
    }

    public static boolean getManifestEnvironmentVariables() {
        try {
            if (getContext() == null) {
                return false;
            }

            ApplicationInfo applicationInfo = getContext().getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            if (bundle == null) {
                return false;
            }
            String prefix = "SDL_ENV.";
            final int trimLength = prefix.length();
            for (String key : bundle.keySet()) {
                if (key.startsWith(prefix)) {
                    String name = key.substring(trimLength);
                    String value = bundle.get(key).toString();
                    nativeSetenv(name, value);
                }
            }
            /* environment variables set! */
            return true;
        } catch (Exception e) {
            Log.v("SDL", "Manifest env exception " + e.toString());
        }
        return false;
    }
    public static boolean isAndroidTV() {
        if (getContext() == null) return false;
        UiModeManager uiModeManager = (UiModeManager) getContext().getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }
        if (Build.MANUFACTURER.equals("MINIX") && Build.MODEL.equals("NEO-U1")) {
            return true;
        }
        if (Build.MANUFACTURER.equals("Amlogic") &&
            (Build.MODEL.startsWith("TV") ||
                Build.MODEL.equals("X96-W") ||
                Build.MODEL.equals("A95X-R1"))) {
            return true;
        }
        return false;
    }

    public static boolean isVRHeadset() {
        if (Build.MANUFACTURER.equals("Oculus") && Build.MODEL.startsWith("Quest")) {
            return true;
        }
        if (Build.MANUFACTURER.equals("Pico")) {
            return true;
        }
        return false;
    }

    public static boolean isChromebook() {
        if (getContext() != null) {
            if (getContext().getPackageManager().hasSystemFeature("org.chromium.arc")
                || getContext().getPackageManager().hasSystemFeature("org.chromium.arc.device_management")) {
                return true;
            }
        }

        return (Build.MODEL != null && Build.MODEL.startsWith("sdk_gpc_"));
    }
    public static boolean isDeXMode() {
        if (getContext() == null || Build.VERSION.SDK_INT < 24) {
            return false;
        }
        try {
            final Configuration config = getContext().getResources().getConfiguration();
            final Class<?> configClass = config.getClass();
            return configClass.getField("SEM_DESKTOP_MODE_ENABLED").getInt(configClass)
                == configClass.getField("semDesktopModeEnabled").getInt(config);
        } catch(Exception ignored) {
            return false;
        }
    }
    public static double getDiagonal()
    {
        DisplayMetrics metrics = new DisplayMetrics();
        Activity activity = getContext();
        if (activity == null) {
            return 0.0;
        }
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        double dWidthInches = metrics.widthPixels / (double)metrics.xdpi;
        double dHeightInches = metrics.heightPixels / (double)metrics.ydpi;

        return Math.sqrt((dWidthInches * dWidthInches) + (dHeightInches * dHeightInches));
    }

    public static boolean isTablet() {
        return (getDiagonal() >= 7.0);
    }

    public static boolean sendMessage(int what, int arg) {
        return false;
    }

    public static void minimizeWindow() {
        if (mContext != null) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(startMain);
        }
    }

    public static boolean setActivityTitle(String title) {
        return true;
    }

    public static void setWindowStyle(boolean fullscreen) {
        if (getContext() != null) {
            getContext().runOnUiThread(() -> {
                if (fullscreen) {
                    getContext().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                } else {
                    getContext().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            });
        }
    }

    public static boolean showTextInput(int input_type, int x, int y, int w, int h) {
        try {
            Activity context = getContext();
            if (context == null) return false;
            Class<?> gameActivityClass = Class.forName("net.kdt.pojavlaunch.game.GameActivity");
            Method switchKeyboardStateMethod = gameActivityClass.getMethod("switchKeyboardState", boolean.class);
            context.runOnUiThread(() -> {
                try {
                    switchKeyboardStateMethod.invoke(null, true);
                } catch (Exception e) {
                    Log.e("SDL", "Failed to invoke switchKeyboardState", e);
                }
            });
            return true;
        } catch (Exception e) {
            Log.e("SDL", "GameActivity not found or method missing", e);
        }
        return false;
    }

    public static boolean showToast(String message, int duration, int gravity, int xOffset, int yOffset)
    {
        if (mContext == null) return false;
        try
        {
            mContext.runOnUiThread(() -> {
                try {
                    Toast toast = Toast.makeText(mContext, message, duration);
                    if (gravity >= 0) {
                        toast.setGravity(gravity, xOffset, yOffset);
                    }
                    toast.show();
                } catch(Exception ex) {
                    Log.e("SDL", "Failed to spawn toast: " + ex.getMessage());
                }
            });
        } catch(Exception ex) {
            return false;
        }
        return true;
    }

    public static int openFileDescriptor(String uri, String mode) {
        if (mContext == null) return -1;
        try(ParcelFileDescriptor fileDescriptor = mContext.getContentResolver().openFileDescriptor(Uri.parse(uri), mode);) {
            if(fileDescriptor == null) return -1;
            return fileDescriptor.detachFd();
        } catch (IOException e) {
            Log.e("SDL", "Unable to open FD at " + uri);
            return -1;
        }
    }

    public static boolean showFileDialog(String[] filters, boolean allowMultiple, int type, String initialPath, int requestCode) {
        return false;
    }

    public static String getPreferredLocales() {
        StringBuilder result = new StringBuilder();
        if (Build.VERSION.SDK_INT >= 24) {
            LocaleList locales = LocaleList.getAdjustedDefault();
            for (int i = 0; i < locales.size(); i++) {
                if (i != 0) result.append(",");
                result.append(formatLocale(locales.get(i)));
            }
        }
        return result.toString();
    }

    public static String formatLocale(Locale locale) {
        String lang = locale.getLanguage();
        if (lang.equals("in")) lang = "id";
        else if (lang.isEmpty()) lang = "und";

        String country = locale.getCountry();
        return country.isEmpty() ? lang : lang + "_" + country;
    }
}
