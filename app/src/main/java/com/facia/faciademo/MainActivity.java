package com.facia.faciademo;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.facia.faciademo.Login.LoginFragment;

import java.io.File;

public class MainActivity extends AppCompatActivity {
//    private static final int RC_APP_UPDATE = 11;

    /**
     * method to send uncaught exceptions to Crash report to ErrorLog class
     */
    private final Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
            try {
                ErrorLogs.sendStacktraceReport((Exception) ex, this.getClass().getName());
            } catch (Exception e) {
                ErrorLogs.exceptionReport(e, "MainActivity/_unCaughtExceptionHandler");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialization();
    }

    /**
     * method to initialize & to set activity related parameters
     * invoking further method to init exception/crash handler
     * setting app orientation, Mode etc
     * invoking further method to clear app's all cache data
     */
    private void initialization() {
        try {
            MainSetAndGetData.getInstance().setSplashGone(false);
            MainSetAndGetData.getInstance().setActivity(this);
            MainSetAndGetData.getInstance().setContext(this);
//            clearAppCache();
            exceptionInitialization();
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            Fragment fragment = new LoginFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_frame, fragment, fragment.getClass().getSimpleName()).addToBackStack(null).commit();
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "MainActivity/initialization");
        }
    }

    private void clearAppCache() {
        try {
            // Clear cache from internal storage
            File cacheDir = getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                for (File child : cacheDir.listFiles()) {
                    child.delete();
                }
            }
            // Clear cache from external storage (if available)
            File externalCacheDir = getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.isDirectory()) {
                for (File child : externalCacheDir.listFiles()) {
                    child.delete();
                }
            }
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "MainActivity/clearAppCache");
        }
    }

    /**
     * this function is initializing the thread that catch uncaught exceptions
     * sets the app killed state to false
     * invoke initializingClasses method
     */
    private void exceptionInitialization() {
        try {
            Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "MainActivity/exceptionInitialization");
        }
    }


//    //Step 3: Listen to update state
//    InstallStateUpdatedListener installStateUpdatedListener;
//    //Step 1: Add dependency (build.gradle (app)) & declaring following variables
//    private AppUpdateManager mAppUpdateManager;
//
//    {
//        installStateUpdatedListener = new
//                InstallStateUpdatedListener() {
//                    @Override
//                    public void onStateUpdate(InstallState state) {
//                        if (state.installStatus() == InstallStatus.DOWNLOADED) {
//                            //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
//                            popUpSnackBarForCompleteUpdate();
//                        } else if (state.installStatus() == InstallStatus.INSTALLED) {
//                            if (mAppUpdateManager != null) {
//                                mAppUpdateManager.unregisterListener(installStateUpdatedListener);
//                            }
//                        } else {
//                            Log.i("TAG", "InstallStateUpdatedListener: state: " + state.installStatus());
//                        }
//                    }
//                };
//    }

    //Step 2:Check for update availability and start if it's available
    @Override
    protected void onStart() {
        super.onStart();
//        mAppUpdateManager = AppUpdateManagerFactory.create(this);
//        mAppUpdateManager.registerListener(installStateUpdatedListener);
//        mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
//            System.out.println(appUpdateInfo.packageName());
//            System.out.println(appUpdateInfo.availableVersionCode());
//            //0 - Unknown
//            //1 - UPDATE_NOT_AVAILABLE
//            //2 - UPDATE_NOT_AVAILABLE
//            //2 - DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
//            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
//                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
//                try {
//                    mAppUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE,
//                            MainActivity.this, RC_APP_UPDATE);
//                } catch (IntentSender.SendIntentException e) {
//                    e.printStackTrace();
//                }
//            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
//                //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
//                popUpSnackBarForCompleteUpdate();
//            }
//            //if update is not avaiable then
//            else {
//                Log.e("Update Response", "Update not available.");
//            }
//        });
    }

//    //Step 4: Get a callback for update status
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == RC_APP_UPDATE) {
//            if (resultCode != RESULT_OK) {
//                Log.e("onActivity", "onActivityResult: app download failed");
//            }
//        }
//    }
//
//    //Step 5: Flexible update
//    private void popUpSnackBarForCompleteUpdate() {
//        Snackbar snackbar =
//                Snackbar.make(
//                        findViewById(R.id.mainActivityParent),
//                        "New app is ready!",
//                        Snackbar.LENGTH_INDEFINITE);
//        snackbar.setAction("Install", view -> {
//            if (mAppUpdateManager != null) {
//                mAppUpdateManager.completeUpdate();
//            }
//        });
//        snackbar.setActionTextColor(getResources().getColor(com.facia.faciasdk.R.color.dialog_button_text_color));
//        snackbar.show();
//    }
//
//    //Step 6: Don't forget to unregister listener (in onStop method)
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (mAppUpdateManager != null) {
//            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
//        }
//    }
}