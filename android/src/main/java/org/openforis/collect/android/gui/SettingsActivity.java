package org.openforis.collect.android.gui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import org.apache.commons.lang3.StringUtils;
import org.openforis.collect.Collect;
import org.openforis.collect.R;
import org.openforis.collect.android.Settings;
import org.openforis.collect.android.gui.util.Activities;
import org.openforis.collect.android.gui.util.AppDirs;
import org.openforis.collect.android.gui.util.Dialogs;
import org.openforis.collect.android.gui.util.SlowAsyncTask;
import org.openforis.collect.android.util.HttpConnectionHelper;
import org.openforis.collect.android.util.MessageSources;
import org.openforis.collect.android.util.Permissions;
import org.openforis.collect.manager.MessageSource;
import org.openforis.collect.manager.ResourceBundleMessageSource;
import org.openforis.collect.model.CollectSurvey;
import org.openforis.commons.versioning.Version;
import org.openforis.idm.metamodel.Languages;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;

import static org.openforis.collect.android.gui.util.AppDirs.PREFERENCE_KEY;

/**
 * @author Daniel Wiell
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsActivity extends Activity implements DirectoryChooserFragment.OnFragmentInteractionListener {

    public static final String LANGUAGES_RESOURCE_BUNDLE_NAME = "org/openforis/collect/resourcebundles/languages";
    private static final MessageSource LANGUAGE_MESSAGE_SOURCE = new LanguagesResourceBundleMessageSource();
    private static final Map<String, String> LANGUAGES = createLanguagesData();

    public static final String CREW_ID = "crewId";
    public static final String COMPASS_ENABLED = "compassEnabled";
    private final static String SURVEY_PREFERRED_LANGUAGE_MODE = "survey_preferred_language_mode";
    private final static String SURVEY_PREFERRED_LANGUAGE_SPECIFIED = "survey_preferred_language_specified";
    private final static String LANGUAGE_UI_KEY = "language_ui";
    private final static String LANGUAGE_SURVEY_KEY = "language_survey";
    public static final String REMOTE_SYNC_ENABLED = "remoteSyncEnabled";
    public static final String REMOTE_COLLECT_ADDRESS = "remoteCollectAddress";
    public static final String REMOTE_COLLECT_USERNAME = "remoteCollectUsername";
    public static final String REMOTE_COLLECT_PASSWORD = "remoteCollectPassword";
    public static final String REMOTE_COLLECT_TEST = "remoteCollectTest";

    private DirectoryChooserFragment directoryChooserDialog;
    private SettingsFragment settingsFragment;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeInitializer.init(this);
        File workingDir = AppDirs.root(this);
        final DirectoryChooserConfig directoryChooserConfig = DirectoryChooserConfig.builder()
                .initialDirectory(workingDir.getAbsolutePath())
                .newDirectoryName(workingDir.getName())
                .allowNewDirectoryNameModification(true)
                .build();
        directoryChooserDialog = DirectoryChooserFragment.newInstance(directoryChooserConfig);

        // Display the fragment as the main content.
        settingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment)
                .commit();
    }

    public static void init(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Settings.setCrew(preferences.getString(CREW_ID, ""));
        Settings.setCompassEnabled(preferences.getBoolean(COMPASS_ENABLED, true));
        Settings.setPreferredLanguageMode(Settings.PreferredLanguageMode.valueOf(preferences.getString(SURVEY_PREFERRED_LANGUAGE_MODE,
                Settings.PreferredLanguageMode.SYSTEM_DEFAULT.name())));
        Settings.setPreferredLanguage(preferences.getString(SURVEY_PREFERRED_LANGUAGE_SPECIFIED, Locale.getDefault().getLanguage()));
        Settings.setRemoteSyncEnabled(preferences.getBoolean(REMOTE_SYNC_ENABLED, false));
        Settings.setRemoteCollectAddress(preferences.getString(REMOTE_COLLECT_ADDRESS, ""));
        Settings.setRemoteCollectUsername(preferences.getString(REMOTE_COLLECT_USERNAME, ""));
        Settings.setRemoteCollectPassword(preferences.getString(REMOTE_COLLECT_PASSWORD, ""));
    }

    public void onSelectDirectory(@NonNull String workingDir) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCE_KEY, workingDir);
        editor.apply();
        Preference workingDirPreference = settingsFragment.findPreference(PREFERENCE_KEY);
        workingDirPreference.setSummary(workingDir);
        directoryChooserDialog.dismiss();
        ServiceLocator.reset(this);
        Activities.startNewClearTask(this, MainActivity.class);
        this.finish();
    }

    public void onCancelChooser() {
        directoryChooserDialog.dismiss();
    }

    @Override
    public void onBackPressed() {
        SurveyNodeActivity.restartActivity(this);
        super.onBackPressed();
    }

    public static class SettingsFragment extends PreferenceFragment {
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            setupStorageLocationPreference();
            setupCrewIdPreference();
            setupCompassEnabledPreference();
            setupThemePreference();
            setupLanguagePreference();
            setupRemoteSyncEnabledPreference();
            setupRemoteCollectAddressPreference();
            setupRemoteCollectUsernamePreference();
            setupRemoteCollectPasswordPreference();
            setupRemoteCollectConnectionTestPreference();
        }

        private void setupStorageLocationPreference() {
            Preference workingDirPreference = findPreference(PREFERENCE_KEY);
            workingDirPreference.setSummary(AppDirs.root(getActivity()).getAbsolutePath());
            workingDirPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    ((SettingsActivity) getActivity()).directoryChooserDialog.show(getFragmentManager(), null);
                    return true;
                }
            });
        }

        private void setupCrewIdPreference() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Preference preference = findPreference(CREW_ID);
            preference.setSummary(preferences.getString(CREW_ID, ""));
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String crew = newValue.toString();
                    preference.setSummary(crew);
                    Settings.setCrew(crew);
                    return true;
                }
            });
        }

        private void setupCompassEnabledPreference() {
            Preference preference = findPreference(COMPASS_ENABLED);
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Settings.setCompassEnabled((Boolean) newValue);
                    return true;
                }
            });
        }

        private void setupThemePreference() {
            Preference preference = findPreference(ThemeInitializer.THEME_PREFERENCE_KEY);
            ThemeInitializer.Theme theme = ThemeInitializer.determineThemeFromPreferences(getActivity());
            preference.setSummary(getThemeSummary(theme.name()));
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(getThemeSummary((String) newValue));
                    return true;
                }
            });
        }

        private void setupLanguagePreference() {
            final Preference preferredLanguageModePreference = findPreference(SURVEY_PREFERRED_LANGUAGE_MODE);
            final ListPreference preferredLanguagePreference = (ListPreference) findPreference(SURVEY_PREFERRED_LANGUAGE_SPECIFIED);

            preferredLanguagePreference.setEntries(LANGUAGES.values().toArray(new String[0]));
            preferredLanguagePreference.setEntryValues(LANGUAGES.keySet().toArray(new String[0]));

            updateLanguagePreferenceUI();

            preferredLanguageModePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Settings.PreferredLanguageMode newPreferredLanguageMode = Settings.PreferredLanguageMode.valueOf((String) newValue);
                    Settings.setPreferredLanguageMode(newPreferredLanguageMode);

                    String preferredLanguageCode = newPreferredLanguageMode == Settings.PreferredLanguageMode.SPECIFIED
                            ? Locale.getDefault().getLanguage()
                            : null;

                    Settings.setPreferredLanguage(preferredLanguageCode);
                    preferredLanguagePreference.setValue(preferredLanguageCode);

                    handleLanguageChanged(getActivity());
                    return true;
                }
            });

            preferredLanguagePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Settings.setPreferredLanguage((String) newValue);
                    handleLanguageChanged(getActivity());
                    return true;
                }
            });
        }

        private String getPreferredLanguageModeSummary(Settings.PreferredLanguageMode mode) {
            switch (mode) {
                case SYSTEM_DEFAULT:
                    return getString(R.string.settings_preferred_language_mode_system_default);
                case SURVEY_DEFAULT:
                    return getString(R.string.settings_preferred_language_mode_survey_default);
                default:
                    return getString(R.string.settings_preferred_language_mode_specified);
            }
        }

        private String determinePreferredLanguageCode() {
            CollectSurvey selectedSurvey = getSelectedSurvey();
            switch (Settings.getPreferredLanguageMode()) {
                case SYSTEM_DEFAULT:
                    Locale systemLocale = Resources.getSystem().getConfiguration().locale;
                    return systemLocale.getLanguage();
                case SURVEY_DEFAULT:
                    return selectedSurvey == null
                            ? null
                            : selectedSurvey.getDefaultLanguage();
                case SPECIFIED:
                    return Settings.getPreferredLanguage();
                default:
                    return null;
            }
        }

        private String getThemeSummary(String themeName) {
            return getString(themeName.equalsIgnoreCase(ThemeInitializer.Theme.DARK.name())
                    ? R.string.settings_theme_dark_summary
                    : R.string.settings_theme_light_summary
            );
        }

        private void setupRemoteSyncEnabledPreference() {
            Preference preference = findPreference(REMOTE_SYNC_ENABLED);
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Settings.setRemoteSyncEnabled((Boolean) newValue);
                    return true;
                }
            });
        }

        private void setupRemoteCollectAddressPreference() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Preference preference = findPreference(REMOTE_COLLECT_ADDRESS);
            preference.setSummary(preferences.getString(REMOTE_COLLECT_ADDRESS, ""));
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringVal = newValue.toString();
                    preference.setSummary(stringVal);
                    Settings.setRemoteCollectAddress(stringVal);
                    return true;
                }
            });
        }

        private void setupRemoteCollectUsernamePreference() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Preference preference = findPreference(REMOTE_COLLECT_USERNAME);
            preference.setSummary(preferences.getString(REMOTE_COLLECT_USERNAME, ""));
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringVal = newValue.toString();
                    preference.setSummary(stringVal);
                    Settings.setRemoteCollectUsername(stringVal);
                    return true;
                }
            });
        }

        private void setupRemoteCollectPasswordPreference() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Preference preference = findPreference(REMOTE_COLLECT_PASSWORD);
            preference.setSummary(StringUtils.isNotBlank(preferences.getString(REMOTE_COLLECT_PASSWORD, "")) ? "*********" : "");
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringVal = newValue.toString();
                    preference.setSummary(StringUtils.isNotBlank(stringVal) ? "*********" : "");
                    Settings.setRemoteCollectPassword(stringVal);
                    return true;
                }
            });
        }

        private void setupRemoteCollectConnectionTestPreference() {
            Preference preference = findPreference(REMOTE_COLLECT_TEST);
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    if (Permissions.checkInternetPermissionOrRequestIt(getActivity())) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        String rootAddress = preferences.getString(SettingsActivity.REMOTE_COLLECT_ADDRESS, "");
                        String username = preferences.getString(SettingsActivity.REMOTE_COLLECT_USERNAME, "");
                        String password = preferences.getString(SettingsActivity.REMOTE_COLLECT_PASSWORD, "");
                        String address = rootAddress + (rootAddress.endsWith("/") ? "" : "/") + "api/info";
                        new RemoteConnectionTestTask(getActivity(), address, username, password)
                                .execute();
                    }
                    return false;
                }
            });
        }

        private void handleLanguageChanged(Context context) {
            UILanguageInitializer.init(context);
            ServiceLocator.resetModelManager(context);

            updateLanguagePreferenceUI();
        }

        private void updateLanguagePreferenceUI() {
            // preferred language mode
            final Preference preferredLanguageModePreference = findPreference(SURVEY_PREFERRED_LANGUAGE_MODE);

            Settings.PreferredLanguageMode preferredLanguageMode = Settings.getPreferredLanguageMode();
            preferredLanguageModePreference.setSummary(getPreferredLanguageModeSummary(preferredLanguageMode));

            // preferred language specified
            final ListPreference preferredLanguagePreference = (ListPreference) findPreference(SURVEY_PREFERRED_LANGUAGE_SPECIFIED);

            preferredLanguagePreference.setEnabled(Settings.PreferredLanguageMode.SPECIFIED == preferredLanguageMode);
            preferredLanguagePreference.setSummary(getLanguageLabel(Settings.getPreferredLanguage()));

            String preferredLangCode = determinePreferredLanguageCode();

            // language UI
            final Preference languageUiPreference = findPreference(LANGUAGE_UI_KEY);

            languageUiPreference.setSummary(preferredLangCode == null ? null : Settings.UILanguage.isSupported(preferredLangCode)
                    ? getLanguageLabel(preferredLangCode)
                    : getLanguageLabel(Settings.UILanguage.getDefault().getCode())
            );

            // language Survey
            final Preference languageSurveyPreference = findPreference(LANGUAGE_SURVEY_KEY);
            CollectSurvey selectedSurvey = getSelectedSurvey();
            if (selectedSurvey == null) {
                languageSurveyPreference.setEnabled(false);
                languageSurveyPreference.setSummary(null);
            } else {
                languageSurveyPreference.setEnabled(true);
                String summary;
                if (preferredLangCode == null) {
                    summary = null;
                } else {
                    List<String> availableLanguages = selectedSurvey.getLanguages();
                    String surveyLanguageUsed = availableLanguages.contains(preferredLangCode)
                            ? preferredLangCode
                            : selectedSurvey.getDefaultLanguage();
                    summary = getLanguageLabel(surveyLanguageUsed);
                }
                languageSurveyPreference.setSummary(summary);
            }
        }

        @Nullable
        private CollectSurvey getSelectedSurvey() {
            return ServiceLocator.surveyService() == null
                    ? null
                    : ServiceLocator.surveyService().getSelectedSurvey();
        }
    }

    private static Map<String, String> createLanguagesData() {
        List<String> langCodes = Languages.getCodes(Languages.Standard.ISO_639_1);
        Map<String, String> unsortedLanguages = new HashMap<String, String>(langCodes.size());
        for (String langCode : langCodes) {
            unsortedLanguages.put(langCode, getLanguageLabel(langCode));
        }
        List<Map.Entry<String, String>> entries = new LinkedList<Map.Entry<String, String>>(unsortedLanguages.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static String getLanguageLabel(String langCode) {
        if (langCode == null)
            return null;
        String label = MessageSources.getMessage(LANGUAGE_MESSAGE_SOURCE, langCode);
        return String.format("%s (%s)", label, langCode);
    }

    private static class RemoteConnectionTestTask extends SlowAsyncTask<Void, Void, JsonObject> {

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
                    Dialogs.alert(context, context.getString(R.string.settings_remote_sync_test_failed_title), message);
                } else {
                    Dialogs.alert(context, context.getString(R.string.settings_remote_sync_test_successful_title),
                            context.getString(R.string.settings_remote_sync_test_successful_message));
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
            Dialogs.alert(context, context.getString(R.string.settings_remote_sync_test_failed_title), message);
        }
    }

    private static class LanguagesResourceBundleMessageSource extends ResourceBundleMessageSource {

        LanguagesResourceBundleMessageSource() {
            super(Collections.singletonList(LANGUAGES_RESOURCE_BUNDLE_NAME));
        }

        protected PropertyResourceBundle findBundle(Locale locale, String baseName) {
            return (PropertyResourceBundle) PropertyResourceBundle.getBundle(baseName, locale);
        }
    }
}
