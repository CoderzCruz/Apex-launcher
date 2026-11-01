package net.kdt.pojavlaunch;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.kdt.pojavlaunch.aerix.ui.ApexBootSplashView;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.tasks.AsyncAssetManager;

import net.kdt.pojavlaunch.R;

public class TestStorageActivity extends Activity {
    private final int REQUEST_STORAGE_REQUEST_CODE = 1;
    private AlertDialog mPermissionRequestDialog;
    private boolean mPermsRequired = false;
    private boolean mPermsDialogShown = false;
    /** The boot artwork with its live progress bar; null while the permission dialog is up. */
    private ApexBootSplashView mSplash;
    private boolean mBootStarted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPermsDialogShown = false;
        setContentView(R.layout.activity_apex_boot);
        mSplash = findViewById(R.id.apex_boot_splash);
        if(Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29 && !isStorageAllowed(this)) {
            mPermsRequired = true;
        } else exit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mPermsRequired) return;
        if(!mPermsDialogShown) requestStoragePermission();
        else showRerequestDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mPermissionRequestDialog != null) mPermissionRequestDialog.dismiss();
    }

    private void showRerequestDialog() {
        if(mPermissionRequestDialog != null) mPermissionRequestDialog.dismiss();
        mPermissionRequestDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.global_error)
                .setMessage(R.string.toast_permission_denied)
                .setPositiveButton(android.R.string.ok,(d,i)->requestStoragePermission())
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mPermsRequired = false;
                exit();
            } else {
                mPermsDialogShown = true;
                showRerequestDialog();
            }
        }
    }

    public static boolean isStorageAllowed(Context context) {
        //Getting the permission status
        int result1 = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);


        //If permission is granted returning true
        return result1 == PackageManager.PERMISSION_GRANTED &&
                result2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_REQUEST_CODE);
    }

    private void exit() {
        if (mBootStarted) return;
        mBootStarted = true;
        if(!Tools.checkStorageRoot(this)) {
            startActivity(new Intent(this, MissingStorageActivity.class));
            finish();
            return;
        }
        //Initialize constants (implicitly) and preferences after we confirm that we have storage.
        if (mSplash != null) mSplash.setProgress(6);
        LauncherPreferences.loadPreferences(this);
        // The splash stays up until the components are unpacked, showing the real progress of it.
        AsyncAssetManager.boot(this, percent -> runOnUiThread(() -> {
            if (mSplash != null) mSplash.setProgress(percent);
        }), () -> {
            if (isFinishing()) return;
            startActivity(new Intent(this, LauncherActivity.class));
            finish();
        });
    }
}
