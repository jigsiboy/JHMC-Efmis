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


}
