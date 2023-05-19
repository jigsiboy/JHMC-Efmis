package org.openforis.collect;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.android.Settings;
import org.openforis.collect.android.gui.MainActivity;
import org.openforis.collect.android.gui.util.SlowAsyncTask;
import org.openforis.collect.android.util.HttpConnectionHelper;
import org.openforis.commons.versioning.Version;

import java.io.FileNotFoundException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class LoginActivity extends AppCompatActivity {

    EditText editTextUsername, editTextPassword;
    final int MIN_PASSWORD_LENGTH = 5;
    Button loginButton;
    ProgressBar loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewInitializations();
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performSignUp(view);
            }
        });

    }

    void viewInitializations() {
        editTextUsername = findViewById(R.id.username);
        editTextPassword = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        loader = findViewById(R.id.loading);

    }

    // Checking if the input in form is valid
    boolean validateInput() {

        if (editTextUsername.getText().toString().equals("")) {
            editTextUsername.setError("Please Enter Username");
            return false;
        }
        if (editTextPassword.getText().toString().equals("")) {
            editTextPassword.setError("Please Enter Password");
            return false;
        }

        // checking the proper email format
        //if (!isEmailValid(editTextUsername.getText().toString())) {
        //    editTextUsername.setError("Please Enter Valid Email");
        //    return false;
        // }

        // checking minimum password Length
        if (editTextPassword.getText().length() < MIN_PASSWORD_LENGTH) {
            editTextPassword.setError("Password Length must be more than " + MIN_PASSWORD_LENGTH + "characters");
            return false;
        }

        return true;
    }

    boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Hook Click Event

    public void performSignUp(View v) {
        loader.setVisibility(View.VISIBLE);

        if (validateInput()) {
            // Input is valid, here send data to your server
            String username = editTextUsername.getText().toString();
            String password = editTextPassword.getText().toString();
            String address = "http://efmis.app/collect/api/info";

            new RemoteConnectionTestTask(this, address, username, password).execute();

        }
    }

    public static class SettingsFragment extends PreferenceFragment {
        public String username = "";
        public String password = "";

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            setupCrewIdPreference(username);
            setupRemoteCollectUsernamePreference(username);
            setupRemoteCollectPasswordPreference(password);
        }

        public void setupCrewIdPreference(String crew) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Preference preference = findPreference(crew);
            preference.setSummary(preferences.getString(crew, ""));
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String crew = newValue.toString();
                    preference.setSummary(crew);
                    Settings.setCrew(crew);
                    return true;
                }
            });
        }

        public void setupRemoteCollectUsernamePreference(String username) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Preference preference = findPreference(username);
            preference.setSummary(preferences.getString(username, ""));
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringVal = newValue.toString();
                    preference.setSummary(stringVal);
                    Settings.setRemoteCollectUsername(stringVal);
                    return true;
                }
            });
        }

        public void setupRemoteCollectPasswordPreference(String password) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Preference preference = findPreference(password);
            preference.setSummary(StringUtils.isNotBlank(preferences.getString(password, "")) ? "*********" : "");
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringVal = newValue.toString();
                    preference.setSummary(StringUtils.isNotBlank(stringVal) ? "*********" : "");
                    Settings.setRemoteCollectPassword(stringVal);
                    return true;
                }
            });
        }

    }

    private class RemoteConnectionTestTask extends SlowAsyncTask<Void, Void, JsonObject> {

        private final String address;
        private final String username;
        private final String password;

        RemoteConnectionTestTask(Activity context, String address, String username, String password) {
            super(context);
            this.address = address;
            this.username = username;
            this.password = password;
        }

        protected JsonObject runTask() throws Exception {
            HttpConnectionHelper connectionHelper = new HttpConnectionHelper(address, username, password);
            return connectionHelper.getJson();
        }

        @Override
        protected void onPostExecute(JsonObject info) {
            super.onPostExecute(info);
            if (info != null) {
                String remoteCollectVersionStr = info.get("version").getAsString();
                Version remoteCollectVersion = new Version(remoteCollectVersionStr);
                if (Collect.VERSION.compareTo(remoteCollectVersion, Version.Significance.MINOR) > 0) {
                    String message = context.getString(R.string.settings_remote_sync_test_failed_message_newer_version,
                            remoteCollectVersion.toString(), Collect.VERSION.toString());
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                } else {
                    SettingsFragment fragment = new SettingsFragment();
                    fragment.username = this.username;
                    fragment.password = this.password;
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }

        @Override
        protected void handleException(Exception e) {
            super.handleException(e);
            String message;
            if (e instanceof FileNotFoundException) {
                message = context.getString(R.string.settings_remote_sync_test_failed_message_wrong_address);
            } else {
                message = e.getMessage();
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }


}