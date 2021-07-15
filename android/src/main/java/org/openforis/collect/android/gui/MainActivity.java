package org.openforis.collect.android.gui;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.chivorn.smartmaterialspinner.SmartMaterialSpinner;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.jaredrummler.materialspinner.MaterialSpinner;

import org.json.JSONException;
import org.json.JSONObject;
import org.openforis.collect.R;
import org.openforis.collect.android.Network.Cloud;
import org.openforis.collect.android.Network.ResponseListener;
import org.openforis.collect.android.gui.exception.StorageAccessException;
import org.openforis.collect.android.gui.util.Activities;
import org.openforis.collect.android.gui.util.App;
import org.openforis.collect.android.gui.util.DefaultDialog;
import org.openforis.collect.android.gui.util.GetLocation;
import org.openforis.collect.android.gui.util.Views;
import org.openforis.collect.android.sqlite.DBHelper;
import org.openforis.collect.android.sqlite.DataModel.PreferenceSetting;
import org.openforis.collect.android.sqlite.DataModel.RecordHolder;
import org.openforis.collect.android.sqlite.DataModel.UserData;
import org.openforis.collect.android.util.Permissions;
import org.openforis.collect.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import liquibase.integration.commandline.Main;

/**
 * @author Stefano Ricci
 */
public class MainActivity extends BaseActivity implements ResponseListener {

    static final String EXIT_FLAG = "EXIT";

    private SurveySpinnerAdapter surveyAdapter;
    private Spinner surveySpinner;
    private MaterialSpinner mNameSpinner;
    private Button mAddUser;
    private ArrayList<UserData> mUserData;
    private String mUserEmail;
    private LocationManager mLocationManager;
    private Cloud mCloud;
    private SmartMaterialSpinner mSpinner;
    private ArrayList<String> mFullNameList;



    private void initialize() {
        try {
            ServiceLocator.init(this);

            surveyAdapter = new SurveySpinnerAdapter(this);

            setContentView(R.layout.activity_main);

//            mNameSpinner = (MaterialSpinner) findViewById(R.id.materialSpinner);
            mSpinner = (SmartMaterialSpinner) findViewById(R.id.spinner1);
            mAddUser = (Button) findViewById(R.id.addName);
            mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            PreferenceSetting pref = new PreferenceSetting(PreferenceManager.getDefaultSharedPreferences(MainActivity.this));
            //if no selected user
            pref.setAlreadySelected(false);

            mCloud = new Cloud();

            updateList();
            mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                    for (UserData userData : mUserData) {
                        if (userData.getFullName() == mFullNameList.get(i)) {
                            PreferenceSetting pref = new PreferenceSetting(PreferenceManager.getDefaultSharedPreferences(MainActivity.this));
                            if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                                GetLocation.confirmLocation(MainActivity.this);
                            } else {
                                final double latitude = GetLocation.getLattitude(mLocationManager, MainActivity.this);
                                final double longitude = GetLocation.getLongitude(mLocationManager, MainActivity.this);
                                String locationAddress = GetLocation.getExactAddress(latitude, longitude, MainActivity.this);
                                mCloud.LATITUDE = latitude;
                                mCloud.LONGITUDE = longitude;
                                pref.setAddress(locationAddress);
                                System.out.println("Longitude: " + longitude + "\nLatitude: " + latitude + "\nAddress: " + locationAddress);
                            }

                            pref.setAlreadySelected(true);
                            pref.setEmail(userData.getEmail());
                            pref.setFullName(userData.getFullName());
                            new Thread(() -> mCloud.createRecord(MainActivity.this)).start();
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            checkLocationPermission();

            mAddUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new DefaultDialog.Builder(MainActivity.this)
                            .message("Register user")
                            .detail("We need this info for us to have the tracking of location")
                            .positiveAction("Confirm", new DefaultDialog.OnClickListener() {
                                @Override
                                public void onClick(Dialog dialog, String fullName, String email) {
                                    if (fullName.isEmpty() || email.isEmpty()) {
                                        Toast.makeText(MainActivity.this, "Please provide Details", Toast.LENGTH_SHORT).show();
                                    } else {
                                        DBHelper dbHelper = new DBHelper(MainActivity.this);
                                        dbHelper.open();
                                        boolean isUpdated = dbHelper.insertUser(email, fullName);

                                        if (isUpdated) {
                                            Toast.makeText(MainActivity.this, "User has been added", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "User has not been added", Toast.LENGTH_SHORT).show();
                                        }
                                        dbHelper.close();
                                        updateList();
                                        dialog.dismiss();
                                    }

                                }
                            })
                            .negativeAction("Cancel", new DefaultDialog.OnClickListener() {
                                @Override
                                public void onClick(Dialog dialog, String fullName, String email) {
                                    dialog.dismiss();
                                }
                            }).build().show();
                }
            });

            TextView mainTitle = findViewById(R.id.mainTitle);
            mainTitle.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/caviar_dreams_normal.ttf"));

            TextView versionText = findViewById(R.id.appVersion);
            versionText.setText(App.versionFull(this));

            initializeSurveySpinner();

            findViewById(R.id.goToDataEntry).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int selectedSurveyPosition = surveySpinner.getSelectedItemPosition();
                    boolean surveySelected = surveyAdapter.isSurveyItem(selectedSurveyPosition);
                    handleGoToDataEntryButtonClick(surveySelected);
                }
            });
            findViewById(R.id.importDemoSurvey).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    //empty survey list, show survey list activity
                    SurveyListActivity.startActivity(MainActivity.this);
                }
            });
            findViewById(R.id.importNewSurvey).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    handleImportNewSurvey();
                }
            });
        } catch (WorkingDirNotWritable ignore) {
            DialogFragment newFragment = new SecondaryStorageNotFoundFragment();
            newFragment.show(getSupportFragmentManager(), "secondaryStorageNotFound");
        } catch (StorageAccessException e) {
            handleStorageAccessException(e);
        }
    }

    public void updateList() {
        DBHelper dbHelper = new DBHelper(MainActivity.this);
        mFullNameList = new ArrayList<String>();
        dbHelper.open();
        mUserData = dbHelper.retrieveAllUser();
        if (mUserData.size() != 0) {
            for (UserData userData : mUserData){
                mFullNameList.add(userData.getFullName());
            }
            mSpinner.setItem(mFullNameList);
        }
        dbHelper.close();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedState) {
        super.onCreate(savedState);

        if (getIntent().getBooleanExtra(EXIT_FLAG, false)) {
            finish();
            return;
        }

        if (Permissions.checkStoragePermissionOrRequestIt(this)) {
            initialize();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!Permissions.isPermissionGranted(grantResults)) {
            return;
        }

        if (requestCode == Permissions.Request.STORAGE.getCode()) {
            initialize();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (surveyAdapter != null) {
            try {
                surveyAdapter.reloadSurveys();
            } catch (StorageAccessException e) {
                // it should never happen here
                handleStorageAccessException(e);
            }

            if (surveyAdapter.isSurveyListEmpty()) {
                Views.hide(findViewById(R.id.notEmptySurveyListFrame));
                Views.show(findViewById(R.id.emptySurveyListFrame));
            } else {
                Views.hide(findViewById(R.id.emptySurveyListFrame));
                Views.show(findViewById(R.id.notEmptySurveyListFrame));
            }
        }
    }

    private void initializeSurveySpinner() {
        surveySpinner = findViewById(R.id.surveySpinner);
        surveySpinner.setAdapter(surveyAdapter);

        String currentSurveyName = SurveyImporter.selectedSurvey(this);
        surveySpinner.setSelection(surveyAdapter.getItemPosition(currentSurveyName));

        surveySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (surveyAdapter.isImportSurveyItem(position)) {
                    handleImportNewSurvey();
                } else {
                    SurveyBaseAdapter.SurveyItem selectedSurveyItem = ((SurveyBaseAdapter.SurveyItem) surveyAdapter.getItem(position));
                    String selectedSurveyName = selectedSurveyItem.name;
                    handleSurveySelected(selectedSurveyName);
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void handleSurveySelected(String selectedSurveyName) {
        String currentSurveyName = SurveyImporter.selectedSurvey(this);
        if (! selectedSurveyName.equals(currentSurveyName)) {
            SurveyImporter.selectSurvey(selectedSurveyName, this);
            SurveyNodeActivity.restartActivity(this);
        }
    }

    private void handleImportNewSurvey() {
        SurveyListActivity.startActivityAndShowImportDialog(this);
    }

    private void handleGoToDataEntryButtonClick(boolean surveySelected) {
        if (surveySelected) {
            if (ServiceLocator.init(this)) {
                SurveyNodeActivity.restartActivity(this);
            }
        } else {
            SurveyListActivity.startActivity(this);
        }
    }

    private void handleStorageAccessException(StorageAccessException e) {
        Toast.makeText(this, R.string.settings_error_working_directory, Toast.LENGTH_LONG).show();
        //Dialogs.info(this, R.string.settings_invalid_dialog_title, R.string.settings_invalid_dialog_wrong_storage_folder);
        Activities.start(this, SettingsActivity.class);
    }

    private void checkLocationPermission() {
        //Getting the location
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }


    @Override
    public void onResponse(RecordHolder recordHolder) {
        if(recordHolder != null) {

            PreferenceSetting preferenceSetting = new PreferenceSetting(PreferenceManager.getDefaultSharedPreferences(this));
            preferenceSetting.setId(recordHolder.getId());

            new Thread(() -> mCloud.postRecords(preferenceSetting.getId(), 18, "TEXT", "/surveyors/email",
                                    "{ \"value\": \""+ preferenceSetting.getEmail() +"\" }", 1, MainActivity.this)).start();

//            System.out.println("newRecord --> " +       recordHolder.getNewRecord());
//            System.out.println("errors --> " +          recordHolder.getErrors());
//            System.out.println("skipped --> " +         recordHolder.getSkipped());
//            System.out.println("missing --> " +         recordHolder.getMissing());
//            System.out.println("missingErrors --> " +   recordHolder.getMissingErrors());
//            System.out.println("missingWarnings --> " + recordHolder.getMissingWarnings());
//            System.out.println("warnings --> " +        recordHolder.getWarnings());
//
//            System.out.println("creationDate --> " +    recordHolder.getCreationDate());
//            System.out.println("modifiedDate --> " +    recordHolder.getModifiedDate());
//            System.out.println("surveyId --> " +        recordHolder.getSurveyId());
//            System.out.println("id --> " +            recordHolder.getId());
//
//            System.out.println("owner id --> " +            recordHolder.getOwner().getId());
//            System.out.println("owner username --> " +      recordHolder.getOwner().getUsername());
//            System.out.println("owner password --> " +      recordHolder.getOwner().getPassword());
//            System.out.println("owner rawPassword --> " +   recordHolder.getOwner().getRawPassword());
//            System.out.println("owner roles --> " +         recordHolder.getOwner().getRoles().get(0));
        }
    }
}
