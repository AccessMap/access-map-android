package edu.washington.accessmap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by samuelfelker on 11/19/15.
 */
public class FeatureAdjuster extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final MapFeature[] mapFeatures = (MapFeature[]) getArguments().getParcelableArray("MAP_FEATURES_STATE");
        final Map<String, MapFeature> featureMap = new HashMap<String, MapFeature>();
        boolean[] defaultChecked = new boolean[mapFeatures.length];
        final String[] listView = new String[mapFeatures.length];
        for (int i = 0; i < mapFeatures.length; i++) {
            featureMap.put(mapFeatures[i].getName(), mapFeatures[i]);
            listView[i] = mapFeatures[i].getName();
            defaultChecked[i] = mapFeatures[i].isVisible();
        }
        builder.setTitle("Select Features")
                .setMultiChoiceItems(listView, defaultChecked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                MapFeature checkedFeature = featureMap.get(listView[which]);
                                checkedFeature.setVisibility(isChecked);
                            }
                        })
                        // Set the action buttons
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        MapFeature[] results = new MapFeature[mapFeatures.length];
                        for (int i = 0; i < mapFeatures.length; i++) {
                            results[i] = featureMap.get(mapFeatures[i].getName());
                        }

                        MainActivity.setMapFeatures(results);
                    }
                });
        return builder.create();
    }
}
