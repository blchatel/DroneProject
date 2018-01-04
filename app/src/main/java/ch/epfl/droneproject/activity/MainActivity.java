package ch.epfl.droneproject.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import ch.epfl.droneproject.R;

/**
 * MainActivity.java
 * @author blchatel
 *
 * Initial Activity with EPFL icon. This activity is just a Starting one
 * @see DeviceListActivity
 *
 * TODO understand why EPFL icon is invisible
 */
public class MainActivity extends AppCompatActivity {

    private static final int LAUNCHING_TIME = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            synchronized(this){
                wait(LAUNCHING_TIME);
                Intent i = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivity(i);
            }
        }
        catch(InterruptedException ex){
            Log.e("MainActivity", "Error while synchronizing");
        }
    }
}
