package edu.washington.accessmap;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.media.Image;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class SearchAddressActivity extends Activity {
    private ListView listView = null;
    private EditText addressText = null;
    private Geocoder geocoder = null;
    private Address previousSearch = null;
    private List<Address> locationList = null;
    private ImageButton clearText = null;

    private double lowerLeftLat = 0;
    private double lowerLeftLong = 0;
    private double upperRightLat = 0;
    private double upperRightLong = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_address);

        previousSearch = this.getIntent().getExtras().getParcelable("PREVIOUS_SEARCH");

        addressText = (EditText) findViewById(R.id.address_text);
        listView = (ListView) findViewById(R.id.address_options);
        geocoder = new Geocoder(this, Locale.ENGLISH);
        clearText = (ImageButton) findViewById(R.id.clear_text);
        clearText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addressText.setText("");
                generateSearch();
            }
        });

        if (previousSearch != null && previousSearch.getAddressLine(0) != null) {
            addressText.setText(DataHelper.extractAddressText(previousSearch));
            generateSearch();
        }

        // bounds for valid addresses
        lowerLeftLat = Double.parseDouble(getString(R.string.lower_left_lat));
        lowerLeftLong = Double.parseDouble(getString(R.string.lower_left_long));
        upperRightLat = Double.parseDouble(getString(R.string.upper_right_lat));
        upperRightLong = Double.parseDouble(getString(R.string.upper_right_long));

        // detect changed text and get new address options
        addressText.addTextChangedListener(new TextWatcher() {
            private long lastSearch = 01;
            private long DELAY = 500;  // in ms

            @Override
            public void afterTextChanged(Editable s) {
                // AUTO GENERATED
                if (System.currentTimeMillis() - lastSearch > DELAY) {
                    lastSearch = System.currentTimeMillis();
                    generateSearch();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // AUTO GENERATED
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // AUTO GENERATED
            }
        });

        // select address from list
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (locationList == null || locationList.size() <= position) {
                    Toast.makeText(getApplicationContext(), "selection is not a valid address", Toast.LENGTH_LONG).show();
                } else {
                    // return selection
                    Address selectedAddress = locationList.get(position);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("SELECTED_ADDRESS", selectedAddress);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
            }
        });
    }

    public void generateSearch() {
        if (addressText.getText().length() >= 3) {
            String locationName = addressText.getText().toString();
            try {
                locationList = geocoder.getFromLocationName(
                        locationName, 5, lowerLeftLat, lowerLeftLong, upperRightLat, upperRightLong);

                if (!locationList.isEmpty()) { // we have location results

                    // build string version of addresses for listView
                    List<String> locationStrings = new ArrayList<String>();
                    for (Address a : locationList) {
                        locationStrings.add(DataHelper.extractAddressText(a));
                    }

                    ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(),
                            R.layout.custom_list_element, R.id.list_content, locationStrings);
                    listView.setAdapter(adapter);
                }
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(),
                        "network unavailable or any other I/O problem occurs" + locationName,
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(),
                    R.layout.custom_list_element, R.id.list_content, new ArrayList<String>());
            listView.setAdapter(adapter);
        }
    }
}
