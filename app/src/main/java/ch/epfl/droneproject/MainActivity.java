package ch.epfl.droneproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.parrot.arsdk.ARSDK;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ARSDK.loadSDKLibs();





    }
}
