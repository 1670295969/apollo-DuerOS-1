package com.baidu.carlifevehicle.access;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AccessibilityUtils {


    public static void setAccessibilityService(Context context,ComponentName componentName) {
        try{
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),0);
            if (isSystemApp(info) || isSystemUpdateApp(info)){
                setAccessibilityServiceState(context,componentName);
            }else{
//                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
//                context.startActivity(intent);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void jump2AccessibilitySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        context.startActivity(intent);
    }

    /**
     * 是否是系统软件或者是系统软件的更新软件
     * @return
     */
    public static boolean isSystemApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) || (pInfo.applicationInfo.uid == 1000);
    }

    public static boolean isSystemUpdateApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
    }

    public static final char SERVICES_SEPARATOR = ':';
    public static Set<ComponentName> getEnabledServicesFromSettings(Context context) {
        final String enabledServicesSetting = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabledServicesSetting)) {
            return Collections.emptySet();
        }

        final Set<ComponentName> enabledServices = new HashSet<>();
        final TextUtils.StringSplitter colonSplitter =
                new TextUtils.SimpleStringSplitter(SERVICES_SEPARATOR);
        colonSplitter.setString(enabledServicesSetting);

        for (String componentNameString : colonSplitter) {
            final ComponentName enabledService = ComponentName.unflattenFromString(
                    componentNameString);
            if (enabledService != null) {
                enabledServices.add(enabledService);
            }
        }

        return enabledServices;
    }

    public static void setAccessibilityServiceState(Context context, ComponentName componentName) {
        Set<ComponentName> enabledServices = getEnabledServicesFromSettings(context);

        if (enabledServices.isEmpty()) {
            enabledServices = new HashSet<>(/* capacity= */ 1);
        }

        enabledServices.add(componentName);


        final StringBuilder enabledServicesBuilder = new StringBuilder();
        for (ComponentName enabledService : enabledServices) {
            enabledServicesBuilder.append(enabledService.flattenToString());
            enabledServicesBuilder.append(SERVICES_SEPARATOR);
        }

        final int enabledServicesBuilderLength = enabledServicesBuilder.length();
        if (enabledServicesBuilderLength > 0) {
            enabledServicesBuilder.deleteCharAt(enabledServicesBuilderLength - 1);
        }

        Settings.Secure.putString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                enabledServicesBuilder.toString());
    }

}
