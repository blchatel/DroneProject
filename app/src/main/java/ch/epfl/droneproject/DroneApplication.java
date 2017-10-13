package ch.epfl.droneproject;

import android.app.Application;
import android.content.Context;

import java.io.File;

public class DroneApplication extends Application{

    private static Context mContext;
    private static String mInternalStoragePath;

    public static Context getContext() {
        //  return instance.getApplicationContext();
        return mContext;
    }

    public static String getAppInternalStoragePath() {
        return mInternalStoragePath;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mInternalStoragePath = mContext.getFilesDir().getAbsolutePath()+ File.separator;
    }


}
