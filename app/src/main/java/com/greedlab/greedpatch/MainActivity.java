package com.greedlab.greedpatch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.greedlab.patch.Greedpatch;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // config access token
        Greedpatch.getInstance(this).token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE0NzM2NDg2MzA0ODgsImlkIjoiNTdkM2JmMmY5MDE1ZWU0N2ZjYzNjYWJhIiwic2NvcGUiOiJwYXRjaDpjaGVjayJ9.YPedieEibUgLecWDmuIVIdkY_Ra-4Qa2HeIQpE7Z_k8";
        // config project ID
        Greedpatch.getInstance(this).projectId = "57e0db7108e1483add770ad1";

        // request at the end of this method
        Greedpatch.getInstance(this).requestPatch();
    }
}
