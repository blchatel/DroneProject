package ch.epfl.droneproject.module;

import android.content.Context;
import android.util.Log;
import com.parrot.arsdk.armavlink.ARMavlinkException;
import com.parrot.arsdk.armavlink.ARMavlinkFileGenerator;
import com.parrot.arsdk.armavlink.ARMavlinkMissionItem;
import java.io.File;
import java.util.Calendar;

import ch.epfl.droneproject.activity.MainActivity;


public class FlightPlanModule {

    public final static String MAVLINK_FOLDER_NAME = "Mavlink";

    private ARMavlinkFileGenerator mGenerator;

    public FlightPlanModule() {
        // Create a MavlinkFile generator Instance
        try {
            mGenerator = new ARMavlinkFileGenerator();
        } catch (ARMavlinkException e) {
            Log.e("FlightPlanModule", "generateMavlinkFile: " + e.getMessage(), e);
        }
    }


    public void addMissionFix(float lat, float lon, float alt, float yaw){
        mGenerator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkNavWaypointMissionItem(lat, lon, alt, yaw));
    }

    public void setMissionFix(int index, float lat, float lon, float alt, float yaw){
        mGenerator.replaceMissionItem(ARMavlinkMissionItem.CreateMavlinkNavWaypointMissionItem(lat, lon, alt, yaw), index);
    }




    public String generateMavlinkFile() {

        String mavlinkStorageDirectoryPath = MainActivity.getAppInternalStoragePath()+MAVLINK_FOLDER_NAME;

        /*
        mGenerator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkDelay(10));
        mGenerator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkTakeoffMissionItem(lat,lng, alt,(float)1.5, 0));
        mGenerator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkNavWaypointMissionItem((float)35.0093, (float)-101.595,(float)1.5, 0));
        mGenerator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkNavWaypointMissionItem((float)35.0097,(float)-101.592,(float)1.5, 0));
        mGenerator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkLandMissionItem(lat,lng, 0, 0));
        */

        // Save our Mavlink file
        // First create the recursive directories if not exist
        final File file = new File(mavlinkStorageDirectoryPath);
        file.mkdirs();

        // Then compute the filename and save the file.
        final Calendar calendar = Calendar.getInstance();
        final String time = ""+calendar.getTime().getTime();
        final String filename = mavlinkStorageDirectoryPath + File.separator + "flightPlan"+time+".mavlink";
        final File mavFile = new File(filename);
        mavFile.delete();

        mGenerator.CreateMavlinkFile(filename);

        return filename;
    }


    public void getMavlinkFiles(){

        String mavlinkStorageDirectoryPath = MainActivity.getAppInternalStoragePath()+MAVLINK_FOLDER_NAME;

        File mavlinkDir = new File(mavlinkStorageDirectoryPath);
        for (File f : mavlinkDir.listFiles()) {
            if (f.isFile()) {
                String name = f.getName();
                Log.e("PLAN", name);
            }
        }
    }

}
