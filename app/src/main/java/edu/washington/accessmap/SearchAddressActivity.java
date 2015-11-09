package edu.washington.accessmap;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchAddressActivity extends Activity {
    private ListView listView = null;
    private EditText addressText = null;
    private Geocoder geocoder = null;
    private List<Address> locationList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_address);

        addressText = (EditText) findViewById(R.id.address_text);
        listView = (ListView) findViewById(R.id.address_options);
        geocoder = new Geocoder(this, Locale.ENGLISH);

        // detect changed text and get new address options
        addressText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                // AUTO GENERATED
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // AUTO GENERATED
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String locationName = addressText.getText().toString();

                if (locationName == null) {
                    Toast.makeText(getApplicationContext(),
                            "locationName == null",
                            Toast.LENGTH_LONG).show();
                } else {
                    try {
                        locationList = geocoder.getFromLocationName(locationName, 5);

                        if (locationList == null) {
                            Toast.makeText(getApplicationContext(),
                                    "locationList == null",
                                    Toast.LENGTH_LONG).show();
                        } else {  // we have some input
                            if (locationList.isEmpty()) {
                                Toast.makeText(getApplicationContext(),
                                        "locationList is empty",
                                        Toast.LENGTH_LONG).show();
                            } else {  // we have location results

                                // build string version of addresses for listView
                                List<String> locationStrings = new ArrayList<String>();
                                for (Address a : locationList) {
                                    String textAddress = "";
                                    for (int i = 0; i < a.getMaxAddressLineIndex(); i++) {
                                        textAddress += a.getAddressLine(i) + ", ";
                                    }
                                    locationStrings.add(textAddress);
                                }

                                ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(),
                                        android.R.layout.simple_list_item_1, locationStrings);
                                listView.setAdapter(adapter);
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
        });


        // select address from list
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Address selectedAddress = locationList.get(position);
                System.out.println(selectedAddress);

                String result = (String) listView.getAdapter().getItem(position);
                System.out.println(result);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("SELECTED_ADDRESS", selectedAddress);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }
}
