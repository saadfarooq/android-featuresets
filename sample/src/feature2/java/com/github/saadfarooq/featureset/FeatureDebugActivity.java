package com.github.saadfarooq.featureset;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class FeatureDebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getString(R.string.test_value);
    }
}