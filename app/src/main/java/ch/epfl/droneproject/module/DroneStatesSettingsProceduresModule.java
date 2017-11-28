package ch.epfl.droneproject.module;


import android.os.Handler;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;

import java.util.ArrayList;

import ch.epfl.droneproject.DroneApplication;
import ch.epfl.droneproject.drone.ConfigDrone;

import static com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING;
import static com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING;


class DroneStatesSettingsProceduresModule implements ARDeviceControllerListener{

    private SkyControllerExtensionModule mSKEModule;
    private final Handler mHandler;

    private float fov, panMax, panMin, tiltMax, tiltMin;
    private float tilt, pan;

    private float roll, pitch, yaw, gaz;
    private double lat, lon, alt;


    private Mission currentMission;
    private boolean pauseMission;

    private boolean waitForEndConfig, waitForEndTakeOff, waitForEndClimbing, waitForEndTrim;



    DroneStatesSettingsProceduresModule(SkyControllerExtensionModule skeModule){
        this.mSKEModule = skeModule;
        mHandler = new Handler(DroneApplication.getApplication().getContext().getMainLooper());
        currentMission = firstMission;
        this.pauseMission = true;
    }

    void update(float roll, float pitch, float yaw){
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    void update(double lat, double lon, double alt){
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }

    void setCameraSettings(final float fov, final float panMin, final float panMax, final float tiltMin, final float tiltMax){
        this.fov = fov;
        this.panMin = panMin;
        this.panMax = panMax;
        this.tiltMin = tiltMin;
        this.tiltMax = tiltMax;
        this.tilt = 0;
        this.pan = 0;
    }

    void setCameraSettings(float tilt, float pan){
        this.tilt = tilt;
        this.pan = pan;
    }


    // ProcedureMethod
    boolean moveCamera(float deltaTilt, float deltaPan) {
        float newTilt = Math.min(Math.max(tilt + deltaTilt, tiltMin), tiltMax);
        float newPan = Math.min(Math.max(pan + deltaPan, panMin), panMax);
        mSKEModule.cameraOrientation(newTilt, newPan);
        return true;
    }

    boolean moveCameraTiltBy(float deltaTilt){
        float newTilt = Math.min(Math.max(tilt + deltaTilt, tiltMin), tiltMax);
        mSKEModule.cameraOrientation(newTilt, pan);
        return true;
    }


    boolean turnRight(){
        mSKEModule.setYaw((byte) 50);
        return true;
    }
    boolean turnLeft(){
        mSKEModule.setYaw((byte) -50);
        return true;

    }
    boolean fixYaw(){
        if(this.yaw != 0) {
            mSKEModule.setYaw((byte) 0);
            return true;
        }
        return false;
    }

    boolean climb(){
        mSKEModule.setGaz((byte) 50);
        this.gaz = 50;
        return true;

    }

    boolean descend(){
        mSKEModule.setGaz((byte) -50);
        this.gaz = -50;
        return true;
    }

    boolean stabilizeVertically(){
        if(this.gaz != 0) {
            mSKEModule.setGaz((byte) 0);
            this.gaz = 0;
            return true;
        }
        return false;
    }

    boolean climbBy(float dz){
        mSKEModule.moveBy(0,0, dz, 0);

        return true;
    }

    boolean descendBy(float dz){
        mSKEModule.moveBy(0,0, -dz, 0);
        return true;
    }

    /**
     * Sleep during time, a pausing function
     * @param time (long): time to sleep in milisecond, i.e 1000 = 1 sec
     */
    boolean sleep(long time){
        try {
            Thread.sleep(time);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean autoTakeOff(){
        mSKEModule.takeOff();
        return true;
    }

    public boolean autoLand(){
        mSKEModule.land();
        return true;
    }


    public void startMission(){
        pauseMission = false;
        DroneApplication.getApplication().getConsoleMessage().pushMessage("StartMission");

        currentMission.start();
    }


    public void pauseMission(){
        pauseMission = true;
    }


    interface Mission{
        boolean start();
        boolean init();
        boolean next();
    }

    interface Procedure{
        boolean process();
    }

    Mission firstMission = new Mission() {

        int currentPocedure = -1; // -1 is not starting yet
        ArrayList<Procedure> procedures = new ArrayList<>();

        @Override
        public boolean start() {
            DroneApplication.getApplication().getConsoleMessage().pushMessage("Init");
            if (init())
                return next();
            return false;
        }

        @Override
        public boolean init(){

            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    waitForEndConfig = false;
                    mSKEModule.setDroneConfig(ConfigDrone.DFAULT_DRONE_CONFIG);
                    return true;
                }
            });
            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    return sleep(1000);
                }
            });

            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    waitForEndTakeOff = true;
                    return autoTakeOff();
                }
            });


            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    waitForEndTrim = true;
                    mSKEModule.flatTrim();
                    return true;
                }
            });

            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    return sleep(1000);
                }
            });

            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    return turnRight();
                }
            });

            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    return sleep(2000);
                }
            });

            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    return fixYaw();
                }
            });

            return true;
        }

        @Override
        public boolean next() {
            while(!waitForEndClimbing && !waitForEndConfig && !waitForEndTakeOff && !waitForEndTrim
                    && procedures.size() > ++currentPocedure){
                procedures.get(currentPocedure).process();
            }
            return true;
        }

    };



    @Override
    public void onStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
    }

    @Override
    public void onExtensionStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARDISCOVERY_PRODUCT_ENUM product, String name, ARCONTROLLER_ERROR_ENUM error) {
    }

    @Override
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {

        // If axis is grabed (i.e when the autopilot is engage) then disengage autopilot
        if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_MAPPER_GRABAXISEVENT) && (elementDictionary != null)){
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {
                pauseMission = true;
            }
        }

        // if event received is the flying state update
        else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED) && (elementDictionary != null)) {
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {
                final ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE));

                if(waitForEndTakeOff && (state == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING ||state == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING)){
                    waitForEndTakeOff = false;
                    currentMission.next();
                }
            }
        }

        // After flat trim
        if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLATTRIMCHANGED) && (elementDictionary != null)){

            if(waitForEndTrim) {
                waitForEndTrim = false;
                currentMission.next();
            }

        }

        // End of delat
        if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND) && (elementDictionary != null)){
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {

                if(waitForEndClimbing){
                    waitForEndClimbing = false;
                    currentMission.next();
                }
            }
        }


    }

}

/*
            sleep(1000);
            autoTakeOff();
            DroneApplication.getApplication().getConsoleMessage().pushMessage("Took Off");
            sleep(1000);

            mSKEModule.flatTrim();
            DroneApplication.getApplication().getConsoleMessage().pushMessage("Trimed");
            sleep(1000);

            climbBy(3);
            DroneApplication.getApplication().getConsoleMessage().pushMessage("Climbed by");


            sleep(2000);

            turnRight();
            DroneApplication.getApplication().getConsoleMessage().pushMessage("Start Turning right");

            sleep(5000);

            fixYaw();
            DroneApplication.getApplication().getConsoleMessage().pushMessage("Stop turning right");

            descendBy(3);
            DroneApplication.getApplication().getConsoleMessage().pushMessage("Descended by");

            //autoLand();
            //DroneApplication.getApplication().getConsoleMessage().pushMessage("Landed");

            */

