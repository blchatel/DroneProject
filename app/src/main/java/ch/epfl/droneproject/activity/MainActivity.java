package ch.epfl.droneproject.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;

import ch.epfl.droneproject.R;


public class MainActivity extends AppCompatActivity {

    private static final int LAUNCHING_TIME = 1;
    private static Context mContext;
    private static String mInternalStoragePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        mInternalStoragePath = mContext.getFilesDir().getAbsolutePath()+ File.separator;

        try {
            synchronized(this){
                wait(LAUNCHING_TIME);
                Intent i = new Intent(getApplicationContext(), SkyControllerActivity.class);
                //Intent i = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivity(i);
            }
        }
        catch(InterruptedException ex){
            // TODO
        }
    }

    public static Context getAppContext() {
        return mContext;
    }
    public static String getAppInternalStoragePath() {
        return mInternalStoragePath;
    }

}
