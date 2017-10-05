package ch.epfl.droneproject.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import ch.epfl.droneproject.R;


public class MainActivity extends AppCompatActivity {

    private static final int LAUNCHING_TIME = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            synchronized(this){
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);

                wait(LAUNCHING_TIME);
                Intent i = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivity(i);
            }
        }
        catch(InterruptedException ex){
            // TODO
        }
    }

}