package dev.vbonnet.flutterwebbrowser;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class MethodCallHandlerImpl implements MethodCallHandler {

    private Activity activity;

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    private static final String SERVICE_ACTION = "android.support.customtabs.action.CustomTabsService";
    private static final String CHROME_PACKAGE = "com.android.chrome";

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "openWebPage":
                openUrl(call, result);
                break;
            case "warmup":
                warmup(result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    /**
     * @param call   Method call
     * @param result Returns result wrapper, wrapping status codes -1, 0 or 1.
     *               -1   -> No browser on device, do nothing
     *               0   -> No implementation of custom tabs available, show picker
     *               1   -> Custom Tab impl. found, call launchUrl as before
     */
    private void openUrl(MethodCall call, Result result) {
        if (activity == null) {
            result.error("no_activity", "Plugin is only available within a activity context", null);
            return;
        }
        String url = call.argument("url");
        HashMap<String, Object> options = call.<HashMap<String, Object>>argument("android_options");

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

        builder.setColorScheme((Integer) options.get("colorScheme"));

        String navigationBarColor = (String) options.get("navigationBarColor");
        if (navigationBarColor != null) {
            builder.setNavigationBarColor(Color.parseColor(navigationBarColor));
        }

        String toolbarColor = (String) options.get("toolbarColor");
        if (toolbarColor != null) {
            builder.setToolbarColor(Color.parseColor(toolbarColor));
        }

        String secondaryToolbarColor = (String) options.get("secondaryToolbarColor");
        if (secondaryToolbarColor != null) {
            builder.setSecondaryToolbarColor(Color.parseColor(secondaryToolbarColor));
        }

        builder.setInstantAppsEnabled((Boolean) options.get("instantAppsEnabled"));

        if ((Boolean) options.get("addDefaultShareMenuItem")) {
            builder.addDefaultShareMenuItem();
        }

        builder.setShowTitle((Boolean) options.get("showTitle"));

        if ((Boolean) options.get("urlBarHidingEnabled")) {
            builder.enableUrlBarHiding();
        }

        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.setPackage(getPackageName());
        if (url != null) {
            Uri uri = Uri.parse(url);
            if (isChromeCustomTabsSupported()) {
                customTabsIntent.launchUrl(activity, uri);
                result.success(1);
            } else if (isAnyBrowserSupported()) {
                useDefaultBrowser(uri);
                result.success(0);
            }
        }

        // do nothing, return error code -1
        result.success(-1);
    }

    private void warmup(Result result) {
        boolean success = isChromeCustomTabsSupported() &&
                CustomTabsClient.connectAndInitialize(activity, getPackageName());
        result.success(success);
    }

    private String getPackageName() {
        return CustomTabsClient.getPackageName(activity, Collections.singletonList(CHROME_PACKAGE));
    }

    private boolean isChromeCustomTabsSupported() {
        Intent serviceIntent = new Intent(SERVICE_ACTION);
        List<ResolveInfo> resolveInfo = activity.getApplicationContext().getPackageManager().queryIntentServices(serviceIntent, 0);

        return resolveInfo != null && !resolveInfo.isEmpty();
    }

    private boolean isAnyBrowserSupported() {
        // Requires uri to determine correct activity that can handle http/s requests. Don't
        // use passed url since that may match the app itself (deeplink).
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://google.de"));
        List<ResolveInfo> resolveInfo = activity.getApplicationContext().getPackageManager().queryIntentActivities(browserIntent, PackageManager.MATCH_ALL);

        return resolveInfo != null && !resolveInfo.isEmpty();
    }

    // Use default browser instead of triggering chooser so that we don't get into
    // a loop where the current app will forever try to open the URL because it
    // was once set as the preferred app for URLs
    private void useDefaultBrowser(Uri uri) {
        Intent defaultBrowser = Intent.makeMainSelectorActivity(
                Intent.ACTION_MAIN,
                Intent.CATEGORY_APP_BROWSER
        );

        defaultBrowser.setData(uri);
        activity.startActivity(defaultBrowser);
    }
}

