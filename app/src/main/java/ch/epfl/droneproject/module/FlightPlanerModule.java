package ch.epfl.droneproject.module;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parrot.arsdk.arcommands.ARCOMMANDS_COMMON_MAVLINK_START_TYPE_ENUM;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.ardatatransfer.ARDATATRANSFER_ERROR_ENUM;
import com.parrot.arsdk.ardatatransfer.ARDATATRANSFER_UPLOADER_RESUME_ENUM;
import com.parrot.arsdk.ardatatransfer.ARDataTransferManager;
import com.parrot.arsdk.ardatatransfer.ARDataTransferUploader;
import com.parrot.arsdk.ardatatransfer.ARDataTransferUploaderCompletionListener;
import com.parrot.arsdk.ardatatransfer.ARDataTransferUploaderProgressListener;
import com.parrot.arsdk.armavlink.ARMavlinkException;
import com.parrot.arsdk.armavlink.ARMavlinkFileGenerator;
import com.parrot.arsdk.armavlink.ARMavlinkMissionItem;
import com.parrot.arsdk.arutils.ARUtilsManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import ch.epfl.droneproject.DroneApplication;
import ch.epfl.droneproject.R;

/**
 * FlightPlanerModule.java
 * @author blchatel
 *
 * This class provide private classes:
 * @see MavLinkFlightPlanUtilities which is useful to create a Mavlink file
 * @see Fix which represent a 4D point on a map (latitude, longitude, altitude and orientation)
 */
public class FlightPlanerModule {

    private int mLastPassedFix;

    /**
     * The drone position which is by definition the -1 element of fixList
     */
    private Fix mCurrentDronePosition;

    private Fix mCurrentOperatorPosition;

    /**
     * A flight plan has a list of fixes
     */
    private ArrayList<Fix> fixList;

    /**
     * The mavlink flight plan utilities
     */
    private MavLinkFlightPlanUtilities mavlink;


    /**
     * Default Flight planner Constructor
     * set the drone to default position and init the mavlink flight plan utilities
     */
    FlightPlanerModule() {
        this.mCurrentDronePosition = new Fix("Bebop", 0,  0, 5, 0);
        this.mLastPassedFix = 0;
        this.mavlink = new MavLinkFlightPlanUtilities(this);
    }


    /**
     * Getter for the mavlink utilities. Allow to call utilities function from outside
     * @return (MavLinkFlightPlanUtilities): the mavlink flight plan utilities
     */
    public MavLinkFlightPlanUtilities getMavlink() {
        return mavlink;
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
     * Add a complete fix
     * @param title (String): new title
     * @param lat (double): new latitude [degree]
     * @param lon (double): new longitude [degree]
     * @param alt (double): new altitude [m]
     * @param yaw (double): new yaw angle [degree]
     */
    private void addFix(String title, double lat, double lon, double alt, double yaw){
        fixList.add(new Fix(title, lat, lon, alt, yaw));
    }

    /**
     * Set the position of a given index fix by updating the latitude and longitude
     * @param index: int
     * @param lat (double): new latitude [degree]
     * @param lon (double): new longitude [degree]
     */
    public void setFixPosition(int index, double lat, double lon){
        if(index == -1)
            return;

        Fix fix = fixList.get(index);
        fixList.set(index, new Fix(fix.title, lat, lon, fix.alt, fix.yaw));
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
        if(index > -1)
            fixList.set(index, new Fix(title, lat, lon, alt, yaw));
    }

    /**
     * Update Current drone position
     * @param lat (double): new latitude [degree]
     * @param lon (double): new longitude [degree]
     * @param alt (double): new altitude [m]
     */
    void updateDronePosition(double lat, double lon, double alt){
        mCurrentDronePosition.lat = lat;
        mCurrentDronePosition.lon = lon;
        mCurrentDronePosition.alt = alt;
    }


    /**
     * Update Current drone orientation
     * @param yaw (double): new yaw angle [degree]
     */
    void updateDroneOrientation(double yaw){
        mCurrentDronePosition.yaw = yaw;
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

    public MarkerOptions getCurrentDronePosition(){

        Matrix matrix = new Matrix();
        matrix.postRotate((float)mCurrentDronePosition.yaw);
        Bitmap icon = BitmapFactory.decodeResource(DroneApplication.getApplication().getContext().getResources(), R.drawable.drone30);
        Bitmap rotatedIcon =  Bitmap.createBitmap(icon, 0, 0, icon.getWidth(), icon.getHeight(), matrix, true);

        return new MarkerOptions()
                .position(mCurrentDronePosition.getPosition())
                .draggable(false)
                .title(mCurrentDronePosition.getTitle())
                .icon(BitmapDescriptorFactory.fromBitmap(rotatedIcon));
    }

    /**
     * Comvert fixes into MarkerOption making maps understand them
     * @return (ArrayList of MarkerOptions): The adapted fixes list for the map drawing
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
     * Getter for the fixes List size
     * @return (int): The size of FixList
     */
    public int getFlightPlanSize(){
        return fixList.size();
    }


    /**
     * Fix class which represent a 4D point on a map (latitude, longitude, altitude and orientation)
     */
    @SuppressWarnings("WeakerAccess")
    public class Fix{

        private static final String DEFAULT_TITLE = "FIX";
        private static final double DEFAULT_ALTITUDE = 5;
        private static final double DEFAULT_YAW = 0;

        String title;
        double lat;
        double lon;
        double alt;
        double yaw;

        // Not used for now
        //private boolean isTakeOff;
        //private boolean isLanding;

        public Fix(String title, double lat, double lon, double alt, double yaw) {
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

            //isTakeOff = false;
            //isLanding = false;
        }

        public LatLng getPosition(){
            return new LatLng(lat, lon);
        }

        public String getTitle() {
            return title;
        }

        public double getAlt() {
            return alt;
        }

        public double getYaw() {
            return yaw;
        }

        public String toString(){
            return title + ":  lat:"+lat + ", lon:"+lon+", alt:"+alt+", yaw:"+yaw;
        }
    }

    /**
     * Mavlink flight plan utilities, useful for creation of a mavlink file and
     * transmit it to the drone. It assume the phone is connected to the drone wifi for sending the
     * plan
     */
    @SuppressWarnings("WeakerAccess")
    public class MavLinkFlightPlanUtilities{

        private final static String TAG = "MavLinkFlightPlan";

        /**
         * The Folder name into internal app storage where to save the mavlink flight plans.
         */
        private final static String MAVLINK_FOLDER_NAME = "/Mavlink/";
        /**
         * Remote address of the bebop drone
         */
        private final static String MAVLINK_REMOTE_ADDRESS = "192.168.42.1";
        private final static int MAVLINK_REMOTE_PORT = 61;
        /**
         * Remote given file name for the uploaded flight plan
         */
        private final static String MAVLINK_REMOTE_FILENAME = "/flightPlan.mavlink";

        private FlightPlanerModule mFpm;

        MavLinkFlightPlanUtilities(FlightPlanerModule fpm) {
            this.mFpm = fpm;
        }

        private void addDelay(ARMavlinkFileGenerator generator, float duration){
            generator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkDelay(duration));
        }

        private void addMissionFix(ARMavlinkFileGenerator generator, Fix fix){
            generator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkNavWaypointMissionItem((float)fix.lat, (float)fix.lon, (float)fix.alt, (float)fix.yaw));
        }

        /**
         * Generate a Mavlink file, save it on the phone, and return its file name
         * The format of the created file is:
         * - (line 1): GC WPL 120 (for Waypoint Protocol version 120)
         * - (line 2 to end):
         *    - seq: Sequence index
         *    - current: current way point
         *    - frame: frame coordinate
         *    - command: MAV_CMD identifier (i.e 16 is for MAV_CMD_NAV_WAYPOINT)
         *    see : http://mavlink.org/messages/common for more details
         *    - param1:
         *    - param2:
         *    - param3:
         *    - param4/yaw:
         *    - param5/longitude/X:
         *    - param6/latitude/Y:
         *    - param7/altitude/Z:
         *    - autocontinue:
         *
         * @return (String): Filename of the created file
         */
        public String generateMavlinkFile(String filename) {
            try {
                // Create a MavlinkFile generator Instance
                ARMavlinkFileGenerator generator = new ARMavlinkFileGenerator();

                for(int i = 0; i < mFpm.fixList.size(); i++){
                    addMissionFix(generator, mFpm.fixList.get(i));
                }

                /*
                // Example:
                generator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkDelay(10));
                generator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkTakeoffMissionItem((float)35.1,(float)-101.595, 10,(float)1.5, 0));
                generator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkNavWaypointMissionItem((float)35.0093, (float)-101.595,(float)1.5, 0));
                generator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkNavWaypointMissionItem((float)35.0097,(float)-101.592,(float)1.5, 0));
                generator.addMissionItem(ARMavlinkMissionItem.CreateMavlinkLandMissionItem((float)35.1,(float)-101.500, 0, 0));
                */

                // direct to external directory
                String externalDirectory = Environment.getExternalStorageDirectory().toString().concat(MAVLINK_FOLDER_NAME);

                // if the directory doesn't exist, create it
                File f = new File(externalDirectory);
                if(!(f.exists() && f.isDirectory())) {
                    boolean success = f.mkdir();
                    if (!success) {
                        Log.e(TAG, "Failed to create the folder " + externalDirectory);
                    }
                }
                // Then compute the filename and save the file.
                if(filename == null || filename.length() < 1) {
                    final Calendar calendar = Calendar.getInstance();
                    final String time = "" + calendar.getTime().getTime();
                    filename = "flightPlan" + time + ".mavlink";
                }

                final String filePath = externalDirectory + "/"+filename;
                final File mavFile = new File(filePath);
                boolean mavIsDeleted = mavFile.delete();

                if(!mavIsDeleted){
                    Log.e("MavLinkFlightPlan", "MavFile should be deleted");
                }

                // Create the file in the mavlink format
                generator.CreateMavlinkFile(filePath);
                return filename;

            }catch (ARMavlinkException e) {
                Log.e("MavLinkFlightPlan", "generateMavlinkFile: " + e.getMessage(), e);
                return null;
            }

        }

        /**
         * Open the mavlink file at filename. And update directly the Flight plan
         * Assume the file is on following mavlink format:
         * The format of the created file is:
         * - (line 1): GC WPL 120 (for Waypoint Protocol version 120)
         * - (line 2 to end):
         *    - seq: Sequence index
         *    - current: current way point
         *    - frame: frame coordinate
         *    - command: MAV_CMD identifier (i.e 16 is for MAV_CMD_NAV_WAYPOINT)
         *    see : http://mavlink.org/messages/common for more details
         *    - param1:
         *    - param2:
         *    - param3:
         *    - param4/yaw:
         *    - param5/longitude/X:
         *    - param6/latitude/Y:
         *    - param7/altitude/Z:
         *    - autocontinue:
         *
         * So open the file and parse it into ARMavlinkMissionItem
         * Notice: The provided parser does not work. I need to implement it in place in this method
         *
         * @param filename (String): The name of the file to open. Assume this file is in MAVLINK_FOLDER_NAME
         * @return (ArrayList of ARMavlinkMissionItem): An array of all the read mission items.
         */
        public ArrayList<ARMavlinkMissionItem> openMavlinkFile(String filename){

            ArrayList<ARMavlinkMissionItem> items = new ArrayList<>();

            try {

                String fileToOpen = Environment.getExternalStorageDirectory().toString().concat(MAVLINK_FOLDER_NAME).concat(filename);

                FileInputStream is;
                BufferedReader reader;
                final File file = new File(fileToOpen);

                if (file.exists()) {

                    is = new FileInputStream(file);
                    reader = new BufferedReader(new InputStreamReader(is));

                    // get the first line with the version number;
                    String line = reader.readLine();

                    if(line!=null){
                        mFpm.cleanFix();
                    }

                    // Then for each item, create an item
                    while(line != null){
                        line = reader.readLine();
                        if(line != null && line.length() > 0){

                            String[] params = line.split("\t");

                            ///
                            if(Integer.parseInt(params[3]) == 16) {
                                mFpm.addFix(params[0], Double.parseDouble(params[8]), Double.parseDouble(params[9]), Double.parseDouble(params[10]), Double.parseDouble(params[7]));
                            }
                            ///

                            items.add(ARMavlinkMissionItem.CreateMavlinkMissionItemWithAllParams(
                                Float.parseFloat(params[4]),   // Param 1
                                Float.parseFloat(params[5]),   // Param 2
                                Float.parseFloat(params[6]),   // Param 3
                                Float.parseFloat(params[7]),   // Param 4, yaw
                                Float.parseFloat(params[8]),   // Param 5, latitude, x
                                Float.parseFloat(params[9]),   // Param 6, longitude, y
                                Float.parseFloat(params[10]),  // Param 7, altitude, z
                                Integer.parseInt(params[3]),   // Command
                                Integer.parseInt(params[0]),   // SEQ
                                Integer.parseInt(params[2]),   // Frame
                                Integer.parseInt(params[1]),   // Current
                                Integer.parseInt(params[11])   // Autocontinue
                            ));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return items;
        }

        /**
         * Return a list of all available mavlink file into the MAVLINK_FOLDER_NAME
         * @return (ArrayList of String): An array list of all filename as string.
         */
        public ArrayList<String> getMavlinkFiles(){

            ArrayList<String> filenames = new ArrayList<>();

            // direct to external directory
            String externalDirectory = Environment.getExternalStorageDirectory().toString().concat(MAVLINK_FOLDER_NAME);

            // if the directory doesn't exist, create it
            File mavlinkDir = new File(externalDirectory);
            if(!(mavlinkDir.exists() && mavlinkDir.isDirectory())) {
                boolean success = mavlinkDir.mkdir();
                if (!success) {
                    Log.e(TAG, "Failed to create the folder " + externalDirectory);
                }
            }

            for (File f : mavlinkDir.listFiles()) {
                if (f.isFile()) {
                    filenames.add(f.getName());
                }
            }

            return filenames;
        }

        /**
         * Transmit the Current open FlightPlan MavlinkFile to the Drone bebop at address
         * MAVLINK_REMOTE_ADDRESS via MAVLINK_REMOTE_PORT and call it with MAVLINK_REMOTE_FILENAME
         * Be careful, the Phone must be on the same network as the drone (i.e. on the local drone wifi)
         * This method first save the current flight plan automatically to take account of exact map
         * display
         * @param featureCommon (ARFeatureCommon):
         */
        void transmitMavlinkFile(final ARFeatureCommon featureCommon) {
            try {
                String autoSave = generateMavlinkFile("autoSaveFlightPlan.mavlink");

                ARDataTransferManager dataTransferManager = new ARDataTransferManager();
                ARDataTransferUploader uploader = dataTransferManager.getARDataTransferUploader();
                ARUtilsManager uploadManager = new ARUtilsManager();
                HandlerThread uploadHandlerThread = new HandlerThread("mavlink_uploader");

                uploadManager.initWifiFtp(MAVLINK_REMOTE_ADDRESS, MAVLINK_REMOTE_PORT, "", "");

                String fileToUpload = Environment.getExternalStorageDirectory().toString().concat(MAVLINK_FOLDER_NAME).concat(autoSave);
                final UploadListener listener = new UploadListener(featureCommon, dataTransferManager, uploader, uploadManager, uploadHandlerThread);
                uploader.createUploader(uploadManager, MAVLINK_REMOTE_FILENAME, fileToUpload, listener, null, listener, null, ARDATATRANSFER_UPLOADER_RESUME_ENUM.ARDATATRANSFER_UPLOADER_RESUME_FALSE);


                uploadHandlerThread.start();

                Runnable uploadRunnable = uploader.getUploaderRunnable();
                Handler uploadHandler = new Handler(uploadHandlerThread.getLooper());

                uploadHandler.post(uploadRunnable);

            } catch (Exception e) {
                Log.e(TAG, "transmitMavlinkFile exception: " + e.getMessage(), e);
            }
        }

        private class UploadListener implements ARDataTransferUploaderProgressListener, ARDataTransferUploaderCompletionListener {

            private final ARFeatureCommon featureCommon;

            private ARDataTransferManager dataTransferManager;
            private ARDataTransferUploader uploader;
            private ARUtilsManager uploadManager;
            private HandlerThread uploadHandlerThread;

            /**
             * Default Upload Listener constructor
             * @param featureCommon (ARFeatureCommon)
             * @param dataTransferManager (ARDataTransferManager)
             * @param uploader (ARDataTransferUploader)
             * @param uploadManager (ARUtilsManager)
             * @param uploadHandlerThread (HandlerThread)
             */
            private UploadListener(final ARFeatureCommon featureCommon, ARDataTransferManager dataTransferManager,
                                   ARDataTransferUploader uploader,  ARUtilsManager uploadManager, HandlerThread uploadHandlerThread) {
                this.featureCommon = featureCommon;
                this.dataTransferManager = dataTransferManager;
                this.uploader = uploader;
                this.uploadManager = uploadManager;
                this.uploadHandlerThread = uploadHandlerThread;
            }

            @Override
            public void didUploadComplete(Object arg, final ARDATATRANSFER_ERROR_ENUM error) {

                // BE CAREFUL : Do Not Get mConsole in this Thread !

                final Object lock = new Object();

                synchronized (lock) {
                    new Thread() {
                        @Override
                        public void run() {
                            synchronized (lock) {

                                uploader.cancelThread();
                                uploader.dispose();
                                uploader = null;

                                uploadManager.closeWifiFtp();
                                uploadManager.dispose();
                                uploadManager = null;

                                dataTransferManager.dispose();
                                dataTransferManager = null;

                                uploadHandlerThread.quit();
                                uploadHandlerThread = null;

                                if (featureCommon != null && error == ARDATATRANSFER_ERROR_ENUM.ARDATATRANSFER_OK) {
                                    featureCommon.sendMavlinkStart(MAVLINK_REMOTE_FILENAME, ARCOMMANDS_COMMON_MAVLINK_START_TYPE_ENUM.ARCOMMANDS_COMMON_MAVLINK_START_TYPE_FLIGHTPLAN);
                                }
                            }
                        }
                    }.start();
                }
            }
            @Override
            public void didUploadProgress(Object arg, float percent) {}
        }
    }

}
