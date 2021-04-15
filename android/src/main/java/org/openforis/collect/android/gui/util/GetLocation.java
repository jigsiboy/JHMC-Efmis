package org.openforis.collect.android.gui.util;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.List;
import java.util.Locale;


public class GetLocation {

    public static String getExactAddress(double lattitude, double longitude, Context context){
        String address = "";
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(lattitude, longitude, 1);
            if (addresses != null){
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                address = strReturnedAddress.toString();
                Log.d("CurrentLocation", strReturnedAddress.toString());
            } else {
                Log.d("CurrentLocation", "No Location Returned");
            }
        } catch (Exception e){
            e.printStackTrace();
            Log.d("CurrentLocation", "Cannot get Address!");
        }

        return address;
    }

    public static double getLattitude(LocationManager locationManager, Context context){
        double lattitude = 0;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            Location location1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            Location location2 = locationManager.getLastKnownLocation(LocationManager. PASSIVE_PROVIDER);

            if (location != null) {
                lattitude = location.getLatitude();

            } else  if (location1 != null) {
                lattitude = location1.getLatitude();


            } else  if (location2 != null) {
                lattitude = location2.getLatitude();

            }else{
                Toast.makeText(context,"Unable to Trace your location", Toast.LENGTH_SHORT).show();
            }
        }

        return lattitude;
    }

    public static double getLongitude(LocationManager locationManager, Context context){

        double longitude = 0;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            Location location1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            Location location2 = locationManager.getLastKnownLocation(LocationManager. PASSIVE_PROVIDER);

            if (location != null) {
                longitude = location.getLongitude();

            } else  if (location1 != null) {
                longitude = location1.getLongitude();


            } else  if (location2 != null) {
                longitude = location2.getLongitude();

            }else{
                Toast.makeText(context,"Unable to Trace your location", Toast.LENGTH_SHORT).show();
            }
        }

        return longitude;
    }

    public static void confirmLocation(final Context context){

        new DefaultDialog.Builder(context)
                .message("Allow to turn-on GPS")
                .detail("Turning on device GPS will provide specific information within your place of existence.")
                .hideEditText(View.GONE)
                .negativeAction("No", new DefaultDialog.OnClickListener() {
                    @Override
                    public void onClick(Dialog dialog, String fullName, String email) {
                        dialog.dismiss();
                    }
                })
                .positiveAction("Yes", new DefaultDialog.OnClickListener() {
                    @Override
                    public void onClick(Dialog dialog, String fullName, String email) {
                        dialog.dismiss();
                        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .build()
                .show();

    }

}
