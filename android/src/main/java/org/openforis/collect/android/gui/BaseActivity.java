package org.openforis.collect.android.gui;

import android.app.ActivityManager;
import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.openforis.collect.R;
import org.openforis.collect.android.Network.Cloud;
import org.openforis.collect.android.Network.ResponseListener;
import org.openforis.collect.android.gui.util.Activities;
import org.openforis.collect.android.gui.util.Dialogs;
import org.openforis.collect.android.gui.util.GetLocation;
import org.openforis.collect.android.gui.util.Keyboard;
import org.openforis.collect.android.sqlite.DataModel.PreferenceSetting;
import org.openforis.collect.android.sqlite.DataModel.RecordHolder;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author Stefano Ricci
 */
public abstract class BaseActivity extends AppCompatActivity implements ResponseListener {

    private CountDownTimer mCountDownTimer;
    private boolean isRunning = false;
    private String mDuration = "59";
    private LocationManager mLocationManager;
    private Cloud mCloud = new Cloud();
    private long duration = 600000 * 2; //20 minutes

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeInitializer.init(this);
        UILanguageInitializer.init(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Keyboard.hide(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //for 5 minute time
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (this.getClass().getSimpleName().equals("MainActivity") && isRunning == false) {
            mCountDownTimer = new CountDownTimer(duration, 1000) {
                @Override
                public void onTick(long l) {
                    isRunning = true;

                    int minutes = (int) (l / 1000) / 60;
                    int seconds = (int) (l / 1000) % 60;

                    mDuration = String.format("%02d:%02d", minutes, seconds);
                    //converted string to log
//                    System.out.println("Time remaining : " + mDuration);
                }

                @Override
                public void onFinish() {
                    //API to fill
                    PreferenceSetting pref = new PreferenceSetting(PreferenceManager.getDefaultSharedPreferences(BaseActivity.this));

                    if (pref.getAlreadySelected()) {
                        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                            GetLocation.confirmLocation(BaseActivity.this);
                        } else {
                            final double latitude = GetLocation.getLattitude(mLocationManager, BaseActivity.this);
                            final double longitude = GetLocation.getLongitude(mLocationManager, BaseActivity.this);
                            String locationAddress = GetLocation.getExactAddress(latitude, longitude, BaseActivity.this);
                            mCloud.LATITUDE = latitude;
                            mCloud.LONGITUDE = longitude;
                            pref.setAddress(locationAddress);
                            System.out.println("Longitude: " + longitude + "\nLatitude: " + latitude + "\nAddress: " + locationAddress);
                        }

                        new Thread(() -> mCloud.createRecord(BaseActivity.this)).start();
                    } else {
                        System.out.println("User is not yet selected");
                        //User is not yet selected
                        //It will loop and set another 5 minutes
                    }

                    isRunning = false;
                    mCountDownTimer.start();
                }
            }.start();
        }
    }

    @Override
    public void onResponse(RecordHolder recordHolder) {

        PreferenceSetting pref = new PreferenceSetting(PreferenceManager.getDefaultSharedPreferences(BaseActivity.this));
        pref.setId(recordHolder.getId());

        new Thread(() -> mCloud.postRecords(recordHolder.getId(), 18, "TEXT", "/surveyors/email",
                "{ \"value\": \""+ pref.getEmail() +"\" }", 1, BaseActivity.this)).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (this.getClass().getSimpleName().equals("MainActivity")) {
            isRunning = true;
        }
    }

    protected void navigateToMainPage() {
        Activities.start(this, MainActivity.class);
    }

    public boolean navigateToSurveyList(MenuItem item) {
        navigateToSurveyList();
        return true;
    }

    protected void navigateToSurveyList() {
        Activities.start(this, SurveyListActivity.class);
    }

    public void navigateToSettings(MenuItem item) {
        Activities.start(this, SettingsActivity.class);
    }

    public void navigateToAboutPage(MenuItem item) {
        Activities.start(this, AboutActivity.class);
    }

    public void exit(MenuItem item) {
        exit();
    }

    protected void exit() {
        Dialogs.confirm(this, R.string.confirm_label, R.string.exit_confirm_message, new Runnable() {
            public void run() {
                BaseActivity.this.finish();
                Bundle bundle = new Bundle();
                bundle.putBoolean(MainActivity.EXIT_FLAG, true);
                Activities.startNewClearTask(BaseActivity.this, MainActivity.class, bundle);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
