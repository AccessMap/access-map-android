package edu.washington.accessmap;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import org.json.JSONException;
import org.json.JSONObject;

public class Routing extends Activity {
    private EditText fromText = null;
    private Address fromAddress = null;

    private EditText toText = null;
    private Address toAddress = null;

    private RadioGroup radioGroup = null;
    private String selectedMobility = null;

    private Button executeRouteButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);
        fromText = (EditText) findViewById(R.id.from_address);
        toText = (EditText) findViewById(R.id.to_address);
        radioGroup = (RadioGroup) findViewById(R.id.mobility_radio_group);
        executeRouteButton = (Button) findViewById(R.id.execute_route_search_button);

        fromText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent searchAddress = new Intent(Routing.this, SearchAddressActivity.class);
                Routing.this.startActivityForResult(searchAddress, 1);
            }
        });

        toText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent searchAddress = new Intent(Routing.this, SearchAddressActivity.class);
                Routing.this.startActivityForResult(searchAddress, 2);
            }
        });

        executeRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent resultIntent = new Intent();
                // could all be null!!
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
