package ch.epfl.droneproject;

import android.app.Application;
import android.content.Context;
import com.parrot.arsdk.ARSDK;
import java.util.ArrayList;
import java.util.List;


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

