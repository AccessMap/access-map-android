package edu.washington.accessmap;

import android.app.Dialog;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int URL_LOADER = 0;

    private MapView mapView = null;
    private EditText addressText = null;
    private Button searchAddressButton = null;
    private ImageButton zoomOutButton = null;
    private ImageButton zoomInButton = null;
    private List<Address> locationList;
    private GoogleApiClient mGoogleApiClient = null;
    private LocationRequest mLocationRequest = null;
    private Geocoder geocoder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buildGoogleApiClient();
        buildLocationRequest();

        /** Create a mapView and give it some properties */
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setStyleUrl(Style.MAPBOX_STREETS);
        mapView.setCenterCoordinate(new LatLng(47.6, -122.3));
        mapView.setZoomLevel(11);
        mapView.onCreate(savedInstanceState);

        addressText = (EditText) findViewById(R.id.address_text);

        searchAddressButton = (Button) findViewById(R.id.address_search_button);
        searchAddressButton.setOnClickListener(searchButtonOnClickListener);

        zoomInButton = (ImageButton) findViewById(R.id.zoom_in_button);
        zoomInButton.setOnClickListener(zoomInButtonOnClickListener);

        zoomOutButton = (ImageButton) findViewById(R.id.zoom_out_button);
        zoomOutButton.setOnClickListener(zoomOutButtonOnClickListener);

        geocoder = new Geocoder(this, Locale.ENGLISH);

        // run data queries in background
        loadSidewalkData();
    }

    View.OnClickListener zoomInButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            double currentZoomLevel = mapView.getZoomLevel();
            mapView.setZoomLevel(currentZoomLevel + 1, true);  // animated zoom in
        }
    };

    View.OnClickListener zoomOutButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            double currentZoomLevel = mapView.getZoomLevel();
            mapView.setZoomLevel(currentZoomLevel - 1, true);  // animated zoom in
        }
    };

    View.OnClickListener searchButtonOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View arg0) {
            String locationName = addressText.getText().toString();

            Toast.makeText(getApplicationContext(),
                    "Search for: " + locationName,
                    Toast.LENGTH_SHORT).show();

            if (locationName == null) {
                Toast.makeText(getApplicationContext(),
                        "locationName == null",
                        Toast.LENGTH_LONG).show();
            } else {
                try {
                    locationList = geocoder.getFromLocationName(locationName, 1);

                    if (locationList == null) {
                        Toast.makeText(getApplicationContext(),
                                "locationList == null",
                                Toast.LENGTH_LONG).show();
                    } else {
                        if(locationList.isEmpty()){
                            Toast.makeText(getApplicationContext(),
                                    "locationList is empty",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "number of result: " + locationList.size(),
                                    Toast.LENGTH_LONG).show();

                            for (Address a : locationList) {
                                if (a.getFeatureName() == null) {
                                    Toast.makeText(getApplicationContext(),
                                            "search result unknown",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            "address resolved to: " + a.getFeatureName(),
                                            Toast.LENGTH_LONG).show();
                                }

                                try {
                                    LatLng searchedPosition = new LatLng(a.getLatitude(), a.getLongitude());
                                    mapView.addMarker(new MarkerOptions()
                                            .position(searchedPosition));
                                    mapView.setCenterCoordinate(searchedPosition, true);  // true animates change
                                } catch (IllegalStateException ise) {
                                    Toast.makeText(getApplicationContext(),
                                            "cannot resolve exact location of searched address",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(),
                            "network unavailable or any other I/O problem occurs" + locationName,
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    };

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected synchronized void buildLocationRequest() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Location services connected.");
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            handleNewLocation(mLastLocation);
        }
    }

    @Override
    public void onLocationChanged(Location mLastLocation) {
        handleNewLocation(mLastLocation);
    }

    private void handleNewLocation(Location mLastLocation) {
        Log.i(TAG, "handling new location");
        double currentLatitude = mLastLocation.getLatitude();
        double currentLongitude = mLastLocation.getLongitude();
        LatLng currentPosition = new LatLng(currentLatitude, currentLongitude);
        mapView.addMarker(new MarkerOptions()
                .position(currentPosition));
        mapView.setCenterCoordinate(currentPosition);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
            int errorCode = connectionResult.getErrorCode();
            if (errorCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
                    errorCode == ConnectionResult.SERVICE_MISSING ||
                    errorCode == ConnectionResult.SERVICE_DISABLED) {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, 1);
                dialog.show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause()  {
        super.onPause();
        mapView.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    public void loadSidewalkData() {
        String urlString = "@string/api_url" + "/sidewalks.geojson";
        new CallAccessMapAPI().execute(urlString);
    }

    private class CallAccessMapAPI extends AsyncTask<String, String, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            System.out.println("DOING IN BACKGROUND");
            Log.i(TAG, "DOING IN BACKGROUND");
            String urlString = params[0];  // URL to call

            JSONObject result = null;

            // HTTP Get
            try {

                String bbox = new BoundingBox(47.679446, -122.290313, 47.646978, -122.357879).toString();
                String charset = "UTF-8";
                String query = String.format("bbox=%s", URLEncoder.encode(bbox, charset));

                URL url = new URL(urlString + "?" + query);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader( new InputStreamReader(urlConnection.getInputStream()));

                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();

                System.out.println(response.toString());
                Log.i(TAG, response.toString());

                result = new JSONObject(response.toString());

            } catch (Exception e ) {
                System.out.println(e.getMessage());
                return null;
            }

            return result;
        }

        protected void onPostExecute(JSONObject result) {
            if (result == null) {
                System.out.println("NULL!!!!");
                // do nothing
            } else {
                Log.i(TAG, "POST EXECUTE!!!");
                System.out.println("We made it!");
            }
        }
    }  // end CallAccessMapAPI
}