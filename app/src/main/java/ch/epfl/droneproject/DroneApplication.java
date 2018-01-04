package ch.epfl.droneproject;

import android.app.Application;
import android.content.Context;
import com.parrot.arsdk.ARSDK;

/**
 * DroneApplication.java
 * @author blchatel
 *
 * Welcome in My Drone Application
 * This Android application assume you are piloting a Bebop 2 drone with a SkyController2
 * and acts like an supervised autopilot extension.
 * The project assume the operator is respecting the Swiss and EPFL drone's flight regulations
 * Please use this code with care and be always able to get back controls on your machine to reduce
 * risc of personal and material dammage.
 *
 * This application has been implemented in the context of a Semester project at EPFL
 * with the Media & Design Lab
 * https://ldm.epfl.ch/
 *
 * Bastien Chatelain (Master Student)
 * Immanuel Koh (PhD Supervisor)
 * Jeffrey Huang (Professor)
 */
public class DroneApplication extends Application{

    private static DroneApplication DRONE_APPLICATION;

    private Context mContext;
    private String mInternalStoragePath;
    private ConsoleMessages mConsoleMessage;

    // this block loads the native libraries
    // it is mandatory
    static {
        ARSDK.loadSDKLibs();
    }

    public Context getContext() {
        return mContext;
    }

    public String getAppInternalStoragePath() {
        return mInternalStoragePath;
    }

    public ConsoleMessages getConsoleMessage(){
        return mConsoleMessage;
    }

    public static DroneApplication getApplication(){
        return DRONE_APPLICATION;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mInternalStoragePath = mContext.getFilesDir().getAbsolutePath();
        mConsoleMessage = new ConsoleMessages();

        DroneApplication.DRONE_APPLICATION = this;
    }
}

