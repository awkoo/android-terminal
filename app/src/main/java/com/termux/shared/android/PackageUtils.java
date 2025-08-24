package com.termux.shared.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.R;
import com.termux.shared.interact.MessageDialogUtils;
import com.termux.shared.reflection.ReflectionUtils;
import com.termux.utils.UI;

import java.lang.reflect.Field;

public class PackageUtils {

    private static final String LOG_TAG = "PackageUtils";

    /**
     * Get the {@link Context} for the package name with {@link Context#CONTEXT_RESTRICTED} flags.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the {@code packageName}.
     * @param packageName The package name whose {@link Context} to get.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static Context getContextForPackage(@NonNull final Context context, String packageName) {
       return getContextForPackage(context, packageName, Context.CONTEXT_RESTRICTED);
    }

    /**
     * Get the {@link Context} for the package name.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the {@code packageName}.
     * @param packageName The package name whose {@link Context} to get.
     * @param flags The flags for {@link Context} type.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static Context getContextForPackage(@NonNull final Context context, String packageName, int flags) {
        try {
            return context.createPackageContext(packageName, flags);
        } catch (Exception e) {
            e.getMessage();
//        logMessage(Log.VERBOSE, tag, message);
            return null;
        }
    }

    /**
     * Get the {@link Context} for a package name.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the {@code packageName}.
     * @param packageName The package name whose {@link Context} to get.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static Context getContextForPackageOrExitApp(@NonNull Context context, String packageName,
                                                        final boolean exitAppOnError) {
        Context packageContext = getContextForPackage(context, packageName);

        if (packageContext == null && exitAppOnError) {
            String errorMessage = context.getString(R.string.error_get_package_context_failed_message,
                packageName);
            MessageDialogUtils.exitAppWithErrorMessage(context,
                context.getString(R.string.error_get_package_context_failed_title),
                errorMessage);
        }

        return packageContext;
    }


//    /**
//     * Get the {@link PackageInfo} for the package associated with the {@code context}.
//     *
//     * @param context The {@link Context} for the package.
//     * @param flags The flags to pass to {@link PackageManager#getPackageInfo(String, int)}.
//     * @return Returns the {@link PackageInfo}. This will be {@code null} if an exception is raised.
//     */
//    @Nullable
//    public static PackageInfo getPackageInfoForPackage(@NonNull final Context context, final int flags) {
//        return getPackageInfoForPackage(context, context.getPackageName(), flags);
//    }

//    /**
//     * Get the {@link PackageInfo} for the package associated with the {@code packageName}.
//     *
//     * Also check {@link #isAppInstalled(Context, String, String) if targetting targeting sdk
//     * `30` (android `11`) since {@link PackageManager.NameNotFoundException} may be thrown.
//     *
//     * @param context The {@link Context} for operations.
//     * @param packageName The package name of the package.
//     * @param flags The flags to pass to {@link PackageManager#getPackageInfo(String, int)}.
//     * @return Returns the {@link PackageInfo}. This will be {@code null} if an exception is raised.
//     */
//    @Nullable
//    public static PackageInfo getPackageInfoForPackage(@NonNull final Context context, @NonNull final String packageName, final int flags) {
//        try {
//            return context.getPackageManager().getPackageInfo(packageName, flags);
//        } catch (final Exception e) {
//            return null;
//        }
//    }



//    /**
//     * Get the {@link ApplicationInfo} for the {@code packageName}.
//     *
//     * @param context The {@link Context} for operations.
//     * @param packageName The package name of the package.
//     * @return Returns the {@link ApplicationInfo}. This will be {@code null} if an exception is raised.
//     */
//    @Nullable
//    public static ApplicationInfo getApplicationInfoForPackage(@NonNull final Context context, @NonNull final String packageName) {
//        return getApplicationInfoForPackage(context, packageName, 0);
//    }

//    /**
//     * Get the {@link ApplicationInfo} for the {@code packageName}.
//     *
//     * Also check {@link #isAppInstalled(Context, String, String) if targetting targeting sdk
//     * `30` (android `11`) since {@link PackageManager.NameNotFoundException} may be thrown.
//     *
//     * @param context The {@link Context} for operations.
//     * @param packageName The package name of the package.
//     * @param flags The flags to pass to {@link PackageManager#getApplicationInfo(String, int)}.
//     * @return Returns the {@link ApplicationInfo}. This will be {@code null} if an exception is raised.
//     */
//    @Nullable
//    public static ApplicationInfo getApplicationInfoForPackage(@NonNull final Context context, @NonNull final String packageName, final int flags) {
//        try {
//            return context.getPackageManager().getApplicationInfo(packageName, flags);
//        } catch (final Exception e) {
//            return null;
//        }
//    }

    /**
     * Get the {@code privateFlags} {@link Field} of the {@link ApplicationInfo} class.
     *
     * @param applicationInfo The {@link ApplicationInfo} for the package.
     * @return Returns the private flags or {@code null} if an exception was raised.
     */
    @Nullable
    public static Integer getApplicationInfoPrivateFlagsForPackage(@NonNull final ApplicationInfo applicationInfo) {
        ReflectionUtils.bypassHiddenAPIReflectionRestrictions();
        try {
            return (Integer) ReflectionUtils.invokeField(ApplicationInfo.class, "privateFlags", applicationInfo).value;
        } catch (Exception e) {
            // ClassCastException may be thrown
            //        Logger.logErrorExtended(tag, getMessageAndStackTraceString(message, throwable));
            return null;
        }
    }

    /**
     * Get the {@code privateFlags} {@link Field} of the {@link ApplicationInfo} class.
     *
     * @param fieldName The name of the field to get.
     * @return Returns the field value or {@code null} if an exception was raised.
     */
    @Nullable
    public static Integer getApplicationInfoStaticIntFieldValue(@NonNull String fieldName) {
        ReflectionUtils.bypassHiddenAPIReflectionRestrictions();
        try {
            return (Integer) ReflectionUtils.invokeField(ApplicationInfo.class, fieldName, null).value;
        } catch (Exception e) {
            // ClassCastException may be thrown
            //        Logger.logErrorExtended(tag, getMessageAndStackTraceString(message, throwable));
            return null;
        }
    }

    /**
     * Check if the app associated with the {@code applicationInfo} has a specific flag set.
     *
     * @param flagToCheckName The name of the field for the flag to check.
     * @param applicationInfo The {@link ApplicationInfo} for the package.
     * @return Returns {@code true} if app has flag is set, otherwise {@code false}. This will be
     * {@code null} if an exception is raised.
     */
    @Nullable
    public static Boolean isApplicationInfoPrivateFlagSetForPackage(@NonNull String flagToCheckName, @NonNull final ApplicationInfo applicationInfo) {
        Integer privateFlags = getApplicationInfoPrivateFlagsForPackage(applicationInfo);
        if (privateFlags == null) return null;

        Integer flagToCheck = getApplicationInfoStaticIntFieldValue(flagToCheckName);
        if (flagToCheck == null) return null;

        return ( 0 != ( privateFlags & flagToCheck ) );
    }


//    /**
//     * Get the {@code targetSdkVersion} for the package associated with the {@code context}.
//     *
//     * @param context The {@link Context} for the package.
//     * @return Returns the {@code targetSdkVersion}.
//     */
//    public static int getTargetSDKForPackage(@NonNull final Context context) {
//        return getTargetSDKForPackage(context.getApplicationInfo());
//    }

//    /**
//     * Get the {@code targetSdkVersion} for the package associated with the {@code applicationInfo}.
//     *
//     * @param applicationInfo The {@link ApplicationInfo} for the package.
//     * @return Returns the {@code targetSdkVersion}.
//     */
//    public static int getTargetSDKForPackage(@NonNull final ApplicationInfo applicationInfo) {
//        return applicationInfo.targetSdkVersion;
//    }


//    /**
//     * Check if the app associated with the {@code context} has
//     * ApplicationInfo.PRIVATE_FLAG_REQUEST_LEGACY_EXTERNAL_STORAGE (requestLegacyExternalStorage)
//     * set to {@code true} in app manifest.
//     *
//     * @param context The {@link Context} for the package.
//     * @return Returns {@code true} if app has requested legacy external storage, otherwise
//     * {@code false}. This will be {@code null} if an exception is raised.
//     */
//    @Nullable
//    public static Boolean hasRequestedLegacyExternalStorage(@NonNull final Context context) {
//        return hasRequestedLegacyExternalStorage(context.getApplicationInfo());
//    }
//
//    /**
//     * Check if the app associated with the {@code applicationInfo} has
//     * ApplicationInfo.PRIVATE_FLAG_REQUEST_LEGACY_EXTERNAL_STORAGE (requestLegacyExternalStorage)
//     * set to {@code true} in app manifest.
//     *
//     * @param applicationInfo The {@link ApplicationInfo} for the package.
//     * @return Returns {@code true} if app has requested legacy external storage, otherwise
//     * {@code false}. This will be {@code null} if an exception is raised.
//     */
//    @Nullable
//    public static Boolean hasRequestedLegacyExternalStorage(@NonNull final ApplicationInfo applicationInfo) {
//        return isApplicationInfoPrivateFlagSetForPackage("PRIVATE_FLAG_REQUEST_LEGACY_EXTERNAL_STORAGE", applicationInfo);
//    }


//    /**
//     * Check if app is installed and enabled. This can be used by external apps that don't
//     * share `sharedUserId` with the an app.
//     *
//     * If your third-party app is targeting sdk `30` (android `11`), then it needs to add package
//     * name to the `queries` element or request `QUERY_ALL_PACKAGES` permission in its
//     * `AndroidManifest.xml`. Otherwise it will get `PackageSetting{...... package_name/......} BLOCKED`
//     * errors in `logcat` and  {@link PackageManager.NameNotFoundException} may be thrown.
//     * `RUN_COMMAND` intent won't work either.
//     * Check [package-visibility](https://developer.android.com/training/basics/intents/package-visibility#package-name),
//     * `QUERY_ALL_PACKAGES` [googleplay policy](https://support.google.com/googleplay/android-developer/answer/10158779
//     * and this [article](https://medium.com/androiddevelopers/working-with-package-visibility-dc252829de2d) for more info.
//     *
//     * {@code
//     * <manifest
//     *     <queries>
//     *         <package android:name="com.termux" />
//     *    </queries>
//     *
//     *    <application
//     *        ....
//     *    </application>
//     * </manifest>
//     * }
//     *
//     * @param context The context for operations.
//     * @param appName The name of the app.
//     * @param packageName The package name of the package.
//     * @return Returns {@code errmsg} if {@code packageName} is not installed or disabled, otherwise {@code null}.
//     */
//    public static String isAppInstalled(@NonNull final Context context, String appName, String packageName) {
//        String errmsg = null;
//
//        ApplicationInfo applicationInfo = getApplicationInfoForPackage(context, packageName);
//        boolean isAppEnabled = (applicationInfo != null && applicationInfo.enabled);
//
//        // If app is not installed or is disabled
//        if (!isAppEnabled)
//            errmsg = context.getString(R.string.error_app_not_installed_or_disabled_warning, appName, packageName);
//
//        return errmsg;
//    }


//    /** Wrapper for {@link #setComponentState(Context, String, String, boolean, String, boolean, boolean)} with
//     * {@code alwaysShowToast} {@code true}. */
//    public static String setComponentState(@NonNull final Context context, @NonNull String packageName,
//                                           @NonNull String className, boolean newState, String toastString,
//                                           boolean showErrorMessage) {
//        return setComponentState(context, packageName, className, newState, toastString, showErrorMessage, true);
//    }

//    /**
//     * Enable or disable a {@link ComponentName} with a call to
//     * {@link PackageManager#setComponentEnabledSetting(ComponentName, int, int)}.
//     *
//     * @param context The {@link Context} for operations.
//     * @param packageName The package name of the component.
//     * @param className The {@link Class} name of the component.
//     * @param newState If component should be enabled or disabled.
//     * @param toastString If this is not {@code null} or empty, then a toast before setting state.
//     * @param showErrorMessage If an error message toast should be shown.
//     * @param alwaysShowToast If toast should always be shown even if current state matches new state.
//     * @return Returns the errmsg if failed to set state, otherwise {@code null}.
//     */
//    @Nullable
//    public static String setComponentState(@NonNull final Context context, @NonNull String packageName,
//                                           @NonNull String className, boolean newState, String toastString,
//                                           boolean alwaysShowToast, boolean showErrorMessage) {
//        try {
//            PackageManager packageManager = context.getPackageManager();
//            if (packageManager != null) {
//                if (toastString != null && alwaysShowToast) {
//                    UI.showToast(context, toastString, true);
//                    toastString = null;
//                }
//
//                Boolean currentlyDisabled = PackageUtils.isComponentDisabled(context, packageName, className, false);
//                if (currentlyDisabled == null)
//                    throw new UnsupportedOperationException("Failed to find if component currently disabled");
//
//                Boolean setState = null;
//                if (newState && currentlyDisabled)
//                    setState = true;
//                else if (!newState && !currentlyDisabled)
//                    setState = false;
//
//                if (setState == null) return null;
//
//                if (toastString != null) UI.showToast(context, toastString, true);
//                ComponentName componentName = new ComponentName(packageName, className);
//                packageManager.setComponentEnabledSetting(componentName,
//                    setState ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//                    PackageManager.DONT_KILL_APP);
//            }
//            return null;
//        } catch (final Exception e) {
//            String errmsg = context.getString(
//                newState ? R.string.error_enable_component_failed : R.string.error_disable_component_failed,
//                packageName, className) + ": " + e.getMessage();
//            if (showErrorMessage)
//                UI.showToast(context, errmsg, true);
//            return errmsg;
//        }
//    }
//
//    /**
//     * Check if state of a {@link ComponentName} is {@link PackageManager#COMPONENT_ENABLED_STATE_DISABLED}
//     * with a call to {@link PackageManager#getComponentEnabledSetting(ComponentName)}.
//     *
//     * @param context The {@link Context} for operations.
//     * @param packageName The package name of the component.
//     * @param className The {@link Class} name of the component.
//     * @param logErrorMessage If an error message should be logged.
//     * @return Returns {@code true} if disabled, {@code false} if not and {@code null} if failed to
//     * get the state.
//     */
//    public static Boolean isComponentDisabled(@NonNull final Context context, @NonNull String packageName,
//                                              @NonNull String className, boolean logErrorMessage) {
//        try {
//            PackageManager packageManager = context.getPackageManager();
//            if (packageManager != null) {
//                ComponentName componentName = new ComponentName(packageName, className);
//                // Will throw IllegalArgumentException: Unknown component: ComponentInfo{} if app
//                // for context is not installed or component does not exist.
//                return packageManager.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
//            }
//        } catch (final Exception e) {
////            if (logErrorMessage)
////                Logger.logStackTraceWithMessage(context.getString(R.string.error_get_component_state_failed, packageName, className));
//        }
//
//        return null;
//    }
//
//    /**
//     * Check if an {@link android.app.Activity} {@link ComponentName} can be called by calling
//     * {@link PackageManager#queryIntentActivities(Intent, int)}.
//     *
//     * @param context The {@link Context} for operations.
//     * @param packageName The package name of the component.
//     * @param className The {@link Class} name of the component.
//     * @param flags The flags to filter results.
//     * @return Returns {@code true} if it exists, otherwise {@code false}.
//     */
//    public static boolean doesActivityComponentExist(@NonNull final Context context, @NonNull String packageName,
//                                                     @NonNull String className, int flags) {
//        try {
//            PackageManager packageManager = context.getPackageManager();
//            if (packageManager != null) {
//                Intent intent = new Intent();
//                intent.setClassName(packageName, className);
//                return packageManager.queryIntentActivities(intent, flags).size() > 0;
//            }
//        } catch (final Exception e) {
//            // ignore
//        }
//
//        return false;
//    }

}
