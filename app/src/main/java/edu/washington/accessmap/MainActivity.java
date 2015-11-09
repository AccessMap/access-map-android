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
import com.google.android.gms.maps.model.Polyline;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final String TAG = MainActivity.class.getSimpleName();

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

        addressText = (EditText) findViewById(R.id.address_text_bar);
        addressText.setOnClickListener(searchAddress);

        searchAddressButton = (Button) findViewById(R.id.address_search_button);
        searchAddressButton.setOnClickListener(searchButtonOnClickListener);

        zoomInButton = (ImageButton) findViewById(R.id.zoom_in_button);
        zoomInButton.setOnClickListener(zoomInButtonOnClickListener);

        zoomOutButton = (ImageButton) findViewById(R.id.zoom_out_button);
        zoomOutButton.setOnClickListener(zoomOutButtonOnClickListener);

        geocoder = new Geocoder(this, Locale.ENGLISH);

        // run data queries in background
        loadData();
    }

    View.OnClickListener searchAddress = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            System.out.println("StartingNewActivity!!!");
            Intent searchAddress = new Intent(MainActivity.this, SearchAddressActivity.class);
            MainActivity.this.startActivityForResult(searchAddress, 1);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                Address selectedAddress = data.getExtras().getParcelable("SELECTED_ADDRESS");

                String textAddress = "";
                for (int i = 0; i < selectedAddress.getMaxAddressLineIndex(); i++) {
                    textAddress += selectedAddress.getAddressLine(i) + ", ";
                }
                addressText.setText(textAddress);

                try {
                    LatLng searchedPosition = new LatLng(selectedAddress.getLatitude(), selectedAddress.getLongitude());
                    mapView.addMarker(new MarkerOptions()
                            .position(searchedPosition));
                    mapView.setCenterCoordinate(searchedPosition, true);  // true animates change
                } catch (IllegalStateException ise) {
                    Toast.makeText(getApplicationContext(),
                            "cannot resolve exact location of searched address",
                            Toast.LENGTH_LONG).show();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    } //onActivityResult

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

    public void loadData() {
        String api_url = getString(R.string.api_url);
        new CallAccessMapAPI().execute(api_url, "/curbs.geojson");
        new CallAccessMapAPI().execute(api_url, "/sidewalks.geojson");
    }

    private class CallAccessMapAPI extends AsyncTask<String, String, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            System.out.println("DOING IN BACKGROUND");
            Log.i(TAG, "DOING IN BACKGROUND");
            String urlString = params[0] + params[1];  // URL to call

            JSONObject result = null;

            // HTTP Get
            try {

                String bbox = new BoundingBox(47.679446, -122.290313, 47.646978, -122.357879).toString();
                bbox = "47.646978,-122.357879,47.679446,-122.290313";
                System.out.println(bbox);
                String charset = "UTF-8";
                String query = String.format("bbox=%s", bbox); // URLEncoder.encode(bbox, charset));

                URL url = new URL(urlString + "?" + query);
                //URL url = new URL(urlString);
                System.out.println(urlString + "?" + query);
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
                result.put("class", params[1]);  // track data type

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
                try {
                    switch (result.getString("class")) {
                        case "/curbs.geojson":
                            drawCurbs(result);
                            break;
                        case "/sidewalks.geojson":
                            drawSidewalks(result);
                            break;
                    }
                } catch (JSONException je) {
                    Log.i(TAG, "JSON ERROR");
                }
            }
        }
    }  // end CallAccessMapAPI

    public void drawSidewalks(JSONObject sidewalkData) {
        try {
            JSONArray features = sidewalkData.getJSONArray("features");
            List<PolylineOptions> polylines = new ArrayList<PolylineOptions>();
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");

                LatLng latlngStart = convertToLatLng(coordinates.getJSONArray(0));
                LatLng latlngEnd = convertToLatLng(coordinates.getJSONArray(1));
                polylines.add(new PolylineOptions().add(latlngStart, latlngEnd));
            }
            mapView.addPolylines(polylines);
        } catch (JSONException je) {
            Log.i(TAG, "JSON ERROR");
        }
    }

    public void drawCurbs(JSONObject curbData) {
        try {
            JSONArray features = curbData.getJSONArray("features");
            List<MarkerOptions> markers = new ArrayList<MarkerOptions>();
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                LatLng latlng = new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
                markers.add(new MarkerOptions().position(latlng));
            }
            mapView.addMarkers(markers);
        } catch (JSONException je) {
            Log.i(TAG, "JSON ERROR");
        }
    }

    private LatLng convertToLatLng(JSONArray coordinates) throws JSONException {
        return new LatLng(coordinates.getDouble(1), coordinates.getDouble(0));
    }
}