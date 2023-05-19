package org.openforis.collect.android.sqlite.DataModel;

import android.content.SharedPreferences;

public class PreferenceSetting {

    private final String ID = "id";
    private final String EMAIL = "email";
    private final String FULL_NAME = "full_name";
    private final String ADDRESS = "address";
    private final double LATITUDE = 0.0;
    private final double LONGITUDE = 0.0;
    private final boolean isAlreadySelected = false;

    private SharedPreferences sharedPreferences;

    public PreferenceSetting(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public String getId() {
        return sharedPreferences.getString(ID, null);
    }

    public String getEmail() {
        return sharedPreferences.getString(EMAIL, null);
    }

    public String getFullName() {
        return sharedPreferences.getString(FULL_NAME, null);
    }

    public String getAddress() {
        return sharedPreferences.getString(ADDRESS, null);
    }

    public double getLongitude() {
        return sharedPreferences.getFloat(String.valueOf(LONGITUDE), 0);
    }

    public double getLatitude() {
        return sharedPreferences.getFloat(String.valueOf(LATITUDE), 0);
    }

    public boolean getAlreadySelected() {
        return sharedPreferences.getBoolean(String.valueOf(isAlreadySelected), false);
    }

    public void setId(String value) {
        sharedPreferences.edit()
                .putString(ID, value)
                .apply();
    }

    public void setEmail(String value) {
        sharedPreferences.edit()
                .putString(EMAIL, value)
                .apply();
    }

    public void setFullName(String value) {
        sharedPreferences.edit()
                .putString(FULL_NAME, value)
                .apply();
    }

    public void setAddress(String value) {
        sharedPreferences.edit()
                .putString(ADDRESS, value)
                .apply();
    }

    public void setLongitude(Double value) {
        sharedPreferences.edit()
                .putFloat(String.valueOf(LONGITUDE), value.floatValue())
                .apply();
    }

    public void setLatitude(Double value) {
        sharedPreferences.edit()
                .putFloat(String.valueOf(LATITUDE), value.floatValue())
                .apply();
    }

    public void setAlreadySelected(boolean value) {
        sharedPreferences.edit()
                .putBoolean(String.valueOf(isAlreadySelected), value)
                .apply();
    }

}
