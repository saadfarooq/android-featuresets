package com.github.saadfarooq.featureset;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class FeatureDebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.debug_activity)).setText("FeatureDebugActivity");
        getString(R.string.test_value);
    }
}