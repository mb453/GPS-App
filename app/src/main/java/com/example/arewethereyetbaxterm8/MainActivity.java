package com.example.arewethereyetbaxterm8;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private ActiveListener activeListener = new ActiveListener();

    private final static String TO = "to";
    private final static String TOLAT = "tolat";
    private final static String TOLONG = "tolong";

    private LocationManager locationManager = null;

    private char transport = 'd';

    private double latitude = 0;
    private double longitude = 0;
    private boolean valid = false;

    private double toLatitude = 0;
    private double toLongitude = 0;
    private String to = "";
    private SharedPreferences settings = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Handle an options menu selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.itemSparty:
                newTo("Sparty", 42.731138, -84.487508);
                return true;

            case R.id.itemHome:
                newTo("Big John's", 42.741170, -84.508270);
                return true;

            case R.id.item2250:
                newTo("2250 Engineering", 42.724303, -84.480507);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle setting a new "to" location.
     * @param address Address to display
     * @param lat latitude
     * @param lon longitude
     */
    private void newTo(String address, double lat, double lon) {
        to = address;
        toLatitude = lat;
        toLongitude = lon;

        writePreferences(address);

        setUI();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        to = settings.getString(TO, "2250 Engineering");
        // Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Force the screen to say on and bright
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        toLatitude = Double.parseDouble(settings.getString(TOLAT, "42.724303"));
        toLongitude = Double.parseDouble(settings.getString(TOLONG, "-84.480507"));

        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Also, dont forget to add overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //   int[] grantResults)
                // to handle the case where the user grants the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }

    }

    public void onNew(View view) {
        EditText location = (EditText)findViewById(R.id.editLocation);
        final String address = location.getText().toString().trim();
        newAddress(address);
    }



    public void onDrive(View view) {

        transport = 'd';

    }

    public void onWalk(View view) {

        transport = 'w';

    }

    public void onBike(View view) {

        transport = 'b';

    }

    public void onRoute(View view){


        Uri gmmIntentUri = Uri.parse( "google.navigation:q=" + toLatitude +"," + toLongitude + "&mode=" + transport);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private void newAddress(final String address) {

        if(address.equals("")) {
            // Don't do anything if the address is blank
            return;
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                lookupAddress(address);

            }

        }).start();

    }

    /**
     * Look up the provided address. This works in a thread!
     * @param address Address we are looking up
     */
    private void lookupAddress(String address) {

        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.US);
        boolean exception = false;
        List<Address> locations;
        try {
            locations = geocoder.getFromLocationName(address, 1);
        } catch(IOException ex) {
            // Failed due to I/O exception
            locations = null;
            exception = true;
        }

        final boolean fException = exception;
        final List<Address> fLocations = locations;

        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                newLocation(address, fException, fLocations);
            }
        });
    }

    private void newLocation(String address, boolean exception, List<Address> locations) {

        if(exception) {
            Toast.makeText(MainActivity.this, R.string.exception, Toast.LENGTH_SHORT).show();
        } else {
            if(locations == null || locations.size() == 0) {
                Toast.makeText(this, R.string.couldnotfind, Toast.LENGTH_SHORT).show();
                return;
            }

            EditText location = (EditText)findViewById(R.id.editLocation);
            location.setText("");

            // We have a valid new location
            Address a = locations.get(0);
            newTo(address, a.getLatitude(), a.getLongitude());

            //lookupAddress(address);

        }
    }

    private void writePreferences(String address) {

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TO, address);
        editor.putString(TOLAT, String.format("%f", toLatitude));
        editor.putString(TOLONG, String.format("%f", toLongitude));

        editor.commit();
    }





        private void registerListeners() {
        unregisterListeners();

        // Create a Criteria object
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);

        String bestAvailable = locationManager.getBestProvider(criteria, true);


        if (bestAvailable != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(bestAvailable, 500, 1, activeListener);
            TextView viewProvider = (TextView)findViewById(R.id.textLabelProvider);
            viewProvider.setText(bestAvailable);
            Location location = locationManager.getLastKnownLocation(bestAvailable);
            onLocation(location);

        }


    }

    private void unregisterListeners() {
        locationManager.removeUpdates(activeListener);


    }

    private void onLocation(Location location) {
        if(location == null) {
            return;
        }

        latitude = location.getLatitude();
        longitude = location.getLongitude();
        valid = true;

        setUI();

    }

    /**
     * Set all user interface components to the current state
     */
    private void setUI() {

        TextView viewProvider = (TextView)findViewById(R.id.textTo);

        viewProvider.setText(to);


        if(!valid){

            TextView viewLatitude = (TextView)findViewById(R.id.textLabelLatitude);

            viewLatitude.setText("");

            TextView viewLongitude = (TextView)findViewById(R.id.textLabelLongitude);

            viewLongitude.setText("");

            TextView viewDistance = (TextView)findViewById(R.id.textDistance);

            viewDistance.setText("");



        }

        else{

            TextView viewLatitude = (TextView)findViewById(R.id.textLabelLatitude);

            viewLatitude.setText(String.format("%1$6.1fm", latitude));

            TextView viewLongitude = (TextView)findViewById(R.id.textLabelLongitude);

            viewLongitude.setText(String.format("%1$6.1fm", longitude));

            double ac = Math.abs(latitude - toLatitude);
            double cb = Math.abs(longitude - toLongitude);

            TextView viewDistance = (TextView)findViewById(R.id.textDistance);

            Location locationA = new Location("point A");

            locationA.setLatitude(toLatitude);
            locationA.setLongitude(toLongitude);

            Location locationB = new Location("point B");

            locationB.setLatitude(latitude);
            locationB.setLongitude(longitude);

            float distance = locationA.distanceTo(locationB);




            viewDistance.setText(String.format("%1$6.1fm", distance));





        }


    }

    /**
     * Called when this application becomes foreground again.
     */
    @Override
    protected void onResume() {
        super.onResume();

        TextView viewProvider = (TextView)findViewById(R.id.textLabelProvider);
        viewProvider.setText("");

        setUI();
        registerListeners();

    }

    /**
     * Called when this application is no longer the foreground application.
     */
    @Override
    protected void onPause() {
        unregisterListeners();
        super.onPause();
    }

    private class ActiveListener implements LocationListener {


        @Override
        public void onLocationChanged(Location location) {

            onLocation(location);

        }

        @Override
        public void onStatusChanged(String s, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

            registerListeners();

        }


    };





}