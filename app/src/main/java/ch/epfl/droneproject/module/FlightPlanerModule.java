package ch.epfl.droneproject.module;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parrot.arsdk.armavlink.ARMavlinkException;
import com.parrot.arsdk.armavlink.ARMavlinkFileGenerator;
import com.parrot.arsdk.armavlink.ARMavlinkMissionItem;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import ch.epfl.droneproject.DroneApplication;
import ch.epfl.droneproject.R;

/**
 *
 */
public class FlightPlanerModule {

    private int mLastPassedFix;
    /**
     * The drone position which is by definition the -1 element of fixList
     */
    private Fix mCurrentDronePosition;
    /**
     * A flight plan has a list of fixes
     */
    private ArrayList<Fix> fixList;


    public FlightPlanerModule() {
        this.mCurrentDronePosition = new Fix("Bebop", 0,  0, 5, 0);
        this.mLastPassedFix = 0;
    }


    /**
     * Clean all the fixes by initiating a new empty list
     */
    public void cleanFix(){
        fixList = new ArrayList<>();
    }

    /**
     * Remove the fix at index
     * @param index (int):
     */
    public void removeFix(int index){
        fixList.remove(index);
    }

    /**
     * Add a default fix with only a latitude and longitude
     * @param lat (double): latitude [degree]
     * @param lon (double): longitude [degree]
     */
    public void addDefaultFix(double lat, double lon){
        fixList.add(new Fix(Fix.DEFAULT_TITLE+fixList.size(), lat, lon, Fix.DEFAULT_ALTITUDE, Fix.DEFAULT_YAW));
    }

    /**
     * Set the position of a given index fix by updating the latitude and longitude
     * @param index: int
     * @param lat (double): new latitude [degree]
     * @param lon (double): new longitude [degree]
     */
    public void setFixPosition(int index, double lat, double lon){
        if(index == -1){
            mCurrentDronePosition = new Fix(mCurrentDronePosition.title, lat, lon, mCurrentDronePosition.alt, mCurrentDronePosition.yaw);
        }else {
            Fix fix = fixList.get(index);
            fixList.set(index, new Fix(fix.title, lat, lon, fix.alt, fix.yaw));
        }
    }

    /**
     * Update completely a given index fix by updating all its component
     * @param index (int):
     * @param title (String): new title
     * @param lat (double): new latitude [degree]
     * @param lon (double): new longitude [degree]
     * @param alt (double): new altitude [m]
     * @param yaw (double): new yaw angle [degree]
     */
    public void setFix(int index, String title, double lat, double lon, double alt, double yaw){

        if(index == -1){
            mCurrentDronePosition = new Fix(title, lat, lon, alt, yaw);
        }else {
            fixList.set(index, new Fix(title, lat, lon, alt, yaw));
        }
    }


    /**
     * Get the title of a given index fix
     * @param id (int):
     * @return (String): The title of the given index fix
     */
    public String getTitle(int id){
        if(id==-1){
            return mCurrentDronePosition.getTitle();
        }
        return fixList.get(id).getTitle();
    }

    /**
     * Get the position of a given index fix
     * @param id (int):
     * @return (LatLng): The position of the given index fix
     */
    public LatLng getPosition(int id){
        if(id==-1){
            return mCurrentDronePosition.getPosition();
        }
        return fixList.get(id).getPosition();
    }

    /**
     * Get the altitude of a given index fix
     * @param id (int):
     * @return (double): The altitude of the given index fix
     */
    public double getAlt(int id){
        if(id==-1){
            return mCurrentDronePosition.getAlt();
        }
        return fixList.get(id).getAlt();
    }

    /**
     * Get the yaw angle of a given index fix
     * @param id (int):
     * @return (double): The yaw angle of the given index fix
     */
    public double getYaw(int id){
        if(id==-1){
            return mCurrentDronePosition.getYaw();
        }
        return fixList.get(id).getYaw();
    }

    public MarkerOptions getmCurrentDronePosition(){

        Matrix matrix = new Matrix();
        matrix.postRotate((float)mCurrentDronePosition.yaw);
        Bitmap icon = BitmapFactory.decodeResource(DroneApplication.getContext().getResources(), R.drawable.drone30);
        Bitmap rotatedIcon =  Bitmap.createBitmap(icon, 0, 0, icon.getWidth(), icon.getHeight(), matrix, true);


        return new MarkerOptions()
                .position(mCurrentDronePosition.getPosition())
                .draggable(false)
                .title(mCurrentDronePosition.getTitle())
                .icon(BitmapDescriptorFactory.fromBitmap(rotatedIcon));
    }

    /**
     * Comvert fixes into MarkerOption making maps understand them
     * @return (ArrayList<MarkerOptions>): The adapted fixes list for the map drawing
     */
    public ArrayList<MarkerOptions> getFixesMarkerOptions() {

        ArrayList<MarkerOptions> options = new ArrayList<>();

        for(int i = 0; i<fixList.size(); i++){
            Fix fix = fixList.get(i);
            options.add(new MarkerOptions().position(fix.getPosition()).draggable(true).title(fix.getTitle()));
        }
        return options;
    }


    /**
     *
     */
    class Fix{

        static final String DEFAULT_TITLE = "FIX";
        static final double DEFAULT_ALTITUDE = 5;
        static final double DEFAULT_YAW = 0;

        String title;
        double lat;
        double lon;
        double alt;
        double yaw;

        boolean isTakeOff;
        boolean isLanding;


        Fix(String title, double lat, double lon, double alt, double yaw) {
            this.title = title;

            this.lat = lat%180;
            if(this.lat > 90){
                this.lat = -180+this.lat;
            }
            this.lon = lon%360;
            if(this.lon > 180){
                this.lon = -360+this.lon;
            }
            this.alt = alt;
            this.yaw = yaw%360;

            isTakeOff = false;
            isLanding = false;
        }

        LatLng getPosition(){
            return new LatLng(lat, lon);
        }

        String getTitle() {
            return title;
        }

        double getAlt() {
            return alt;
        }

        double getYaw() {
            return yaw;
        }

        public String toString(){
            return title + ":  lat:"+lat + ", lon:"+lon+", alt:"+alt+", yaw:"+yaw;
        }
    }

    /**
     *
     */
    class MavLinkFlightPlanUtilities{

        /**
         * The Folder name into internal app storage where to save the mavlink flight plans.
         */
        public final static String MAVLINK_FOLDER_NAME = "Mavlink";

        private ARMavlinkFileGenerator mGenerator;


        public void addMissionFix(float lat, float lon, float alt, float yaw){
            mGenerator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkNavWaypointMissionItem(lat, lon, alt, yaw));
        }

        public void setMissionFix(int index, float lat, float lon, float alt, float yaw){
            mGenerator.replaceMissionItem(ARMavlinkMissionItem.CreateMavlinkNavWaypointMissionItem(lat, lon, alt, yaw), index);
        }



        public String generateMavlinkFile() {

            // Create a MavlinkFile generator Instance
            try {
                mGenerator = new ARMavlinkFileGenerator();
            } catch (ARMavlinkException e) {
                Log.e("FlightPlanerModule", "generateMavlinkFile: " + e.getMessage(), e);
            }

            String mavlinkStorageDirectoryPath = DroneApplication.getAppInternalStoragePath()+MAVLINK_FOLDER_NAME;

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

            String mavlinkStorageDirectoryPath = DroneApplication.getAppInternalStoragePath()+MAVLINK_FOLDER_NAME;

            File mavlinkDir = new File(mavlinkStorageDirectoryPath);
            for (File f : mavlinkDir.listFiles()) {
                if (f.isFile()) {
                    String name = f.getName();
                    Log.e("PLAN", name);
                }
            }
        }




    }



}
