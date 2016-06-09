package edu.washington.accessmap;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import java.util.Locale;

public class Routing extends Activity {
    private static final String FROM_CURRENT_LOCATION = "From: Your Current Location";
    private static final Address DUMMY = new Address(Locale.US);

    private EditText fromText = null;
    private Address fromAddress = null;
    private Address fromAddressStart = null;

    private EditText toText = null;
    private Address toAddress = null;

    private RadioGroup radioGroup = null;
    private String selectedMobility = null;

    private Button executeRouteButton = null;

    private Location userLocation = null;
    private Address toAddressStart = null;

    private boolean useUserLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);
        fromText = (EditText) findViewById(R.id.from_address);
        toText = (EditText) findViewById(R.id.to_address);
        radioGroup = (RadioGroup) findViewById(R.id.mobility_radio_group);
        executeRouteButton = (Button) findViewById(R.id.execute_route_search_button);

        useUserLocation = this.getIntent().getExtras().getBoolean("USER_LOCATION");
        if (useUserLocation) {  // first time routing
            fromText.setText(FROM_CURRENT_LOCATION);
        } else {  // reopened_route
            fromAddressStart = this.getIntent().getExtras().getParcelable("FROM_ADDRESS");
            if (fromAddressStart.getMaxAddressLineIndex() != 1
                    && fromAddressStart.getAddressLine(0).equals(FROM_CURRENT_LOCATION)) {  // still use user location
                useUserLocation = true;
                fromText.setText(FROM_CURRENT_LOCATION);
            } else {  // use passes in address
                fromText.setText("From: " + DataHelper.extractAddressText(fromAddressStart));
                fromAddress = fromAddressStart;
            }
        }

        toAddressStart = this.getIntent().getExtras().getParcelable("TO_ADDRESS");
        if (toAddressStart != null) {
            toText.setText("To: " + DataHelper.extractAddressText(toAddressStart));
            toAddress = toAddressStart;
        }

        fromText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent searchAddress = new Intent(Routing.this, SearchAddressActivity.class);
                searchAddress.putExtra("PREVIOUS_SEARCH", DUMMY);
                Routing.this.startActivityForResult(searchAddress, 1);
            }
        });

        toText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent searchAddress = new Intent(Routing.this, SearchAddressActivity.class);
                searchAddress.putExtra("PREVIOUS_SEARCH", DUMMY);
                Routing.this.startActivityForResult(searchAddress, 2);
            }
        });

        executeRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent resultIntent = new Intent();
                // could all be null!!
                resultIntent.putExtra("USER_LATLNG", useUserLocation);
                resultIntent.putExtra("FROM_ADDRESS", fromAddress);
                resultIntent.putExtra("TO_ADDRESS", toAddress);
                resultIntent.putExtra("MOBILITY_SELECTION", radioGroup.getCheckedRadioButtonId());
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {  // fromAddress
            if (resultCode == Activity.RESULT_OK) {
                fromAddress = data.getExtras().getParcelable("SELECTED_ADDRESS");
                fromText.setText("From: " + DataHelper.extractAddressText(fromAddress));
                useUserLocation = false;
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        } else if (requestCode == 2) {  // to Address
            if (resultCode == Activity.RESULT_OK) {
                toAddress = data.getExtras().getParcelable("SELECTED_ADDRESS");
                toText.setText("To: " + DataHelper.extractAddressText(toAddress));
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    } //onActivityResult
}
