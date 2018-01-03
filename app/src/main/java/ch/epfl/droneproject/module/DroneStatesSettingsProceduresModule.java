package ch.epfl.droneproject.module;


import android.os.Handler;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_COMMON_MAVLINKSTATE_MAVLINKFILEPLAYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;

import java.util.ArrayList;

import ch.epfl.droneproject.DroneApplication;
import ch.epfl.droneproject.drone.ConfigDrone;

import static com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING;
import static com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING;


public class DroneStatesSettingsProceduresModule implements ARDeviceControllerListener{

    private SkyControllerExtensionModule mSKEModule;
    private AutoPilotModule mAutopilot;
    private final Handler mHandler;

    // Current drone and settings states value
    private float fov, panMax, panMin, tiltMax, tiltMin;
    private float tilt, pan;
    private float roll, pitch, yaw, gaz;
    private double lat, lon, alt;

    // Current mission and states flag
    private Mission currentMission;
    private boolean currentMissionRunning;

    /**
     * Default constructor for DroneStatesSettingsProceduresModule
     * @param skeModule (SkyControllerExtensionModule): extension controller module
     * @param autopilot (AutoPilotModule): Linked autopilot
     */
    DroneStatesSettingsProceduresModule(SkyControllerExtensionModule skeModule, AutoPilotModule autopilot){
        this.mSKEModule = skeModule;
        this.mAutopilot = autopilot;
        mHandler = new Handler(DroneApplication.getApplication().getContext().getMainLooper());
        currentMission = finalMissionPart1;
    }

    // States methods. This class is kept up to date by calling the following methods

    /**
     * Update the angle states of the drone. Triggered when the drone's attitude change
     * @param roll (float): in radian
     * @param pitch (float): in radian
     * @param yaw (float): : in radian
     */
    void update(float roll, float pitch, float yaw){
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    /**
     * Update the 3D state of the drones
     * @param lat (double): Latitude value
     * @param lon (double): Longitude value
     * @param alt (double): Altitude value
     */
    void update(double lat, double lon, double alt){
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }

    /**
     * Camera Settings initialization: triggered once on init.
     * @param fov (float): Value of the camera horizontal fov (in degree)
     * @param panMax (float): Value of max pan (right pan) (in degree)
     * @param panMin (float): Value of min pan (left pan) (in degree)
     * @param tiltMax (float): Value of max tilt (top tilt) (in degree)
     * @param tiltMin (float): Value of min tilt (bottom tilt) (in degree)
     */
    void setCameraSettings(final float fov, final float panMin, final float panMax, final float tiltMin, final float tiltMax){
        this.fov = fov;
        this.panMin = panMin;
        this.panMax = panMax;
        this.tiltMin = tiltMin;
        this.tiltMax = tiltMax;
        this.tilt = 0;
        this.pan = 0;
    }

    /**
     * Camera orientation with float arguments.
     * @param tilt (float): Tilt camera consign for the drone [deg]
     * @param pan (float): Pan camera consign for the drone [deg]
     */
    void setCameraSettings(float tilt, float pan){
        this.tilt = tilt;
        this.pan = pan;
    }

    // ProcedureMethod: the following methods change the current states and can be used as procedures
    // The return boolean to indicate success

    /**
     * Move the camera by a delta tilt and pan
     * @param deltaTilt (float)
     * @param deltaPan (float)
     * @return true (boolean)
     */
    boolean moveCamera(float deltaTilt, float deltaPan) {
        float newTilt = Math.min(Math.max(tilt + deltaTilt, tiltMin), tiltMax);
        float newPan = Math.min(Math.max(pan + deltaPan, panMin), panMax);
        mSKEModule.cameraOrientation(newTilt, newPan);
        return true;
    }

    /**
     * Move the camera by a delta tilt only
     * @param deltaTilt (float)
     * @return true (boolean)
     */
    boolean moveCameraTiltBy(float deltaTilt){
        float newTilt = Math.min(Math.max(tilt + deltaTilt, tiltMin), tiltMax);
        mSKEModule.cameraOrientation(newTilt, pan);
        return true;
    }

    /**
     * Make the drone turning right by setting yaw rotation to 50% of the max rotation speed
     * Recall rotation speed is given between -100 and +100
     * @return true (boolean)
     */
    boolean turnRight(){
        mSKEModule.setYaw((byte) 50);
        return true;
    }

    /**
     * Make the drone turning left by setting yaw rotation to 50% of the min rotation speed
     * Recall rotation speed is given between -100 and +100
     * @return true (boolean)
     */
    boolean turnLeft(){
        mSKEModule.setYaw((byte) -50);
        return true;
    }

    /**
     * Make the drone stop turning by setting yaw rotation to 0
     * Recall rotation speed is given between -100 and +100
     * @return true (boolean)
     */
    boolean fixYaw(){
        if(this.yaw != 0) {
            mSKEModule.setYaw((byte) 0);
            return true;
        }
        return false;
    }

    /**
     * Make the drone climb by setting gaz to 50% of the max gaz value
     * Recall gaz value is given between -100 and +100
     * @return true (boolean)
     */
    boolean climb(){
        mSKEModule.setGaz((byte) 50);
        this.gaz = 50;
        return true;
    }

    /**
     * Make the drone descend by setting gaz to 50% of the min gaz value
     * Recall gaz value is given between -100 and +100
     * @return true (boolean)
     */
    boolean descend(){
        mSKEModule.setGaz((byte) -50);
        this.gaz = -50;
        return true;
    }

    /**
     * Make the drone vertically stable by setting gaz to 0
     * Recall gaz value is given between -100 and +100
     * @return true (boolean)
     */
    boolean stabilizeVertically(){
        if(this.gaz != 0) {
            mSKEModule.setGaz((byte) 0);
            this.gaz = 0;
            return true;
        }
        return false;
    }

    /**
     * Make the drone climb by dz using moveBy
     * @param dz (float)
     * @return true (boolean)
     */
    boolean climbBy(float dz){
        mSKEModule.moveBy(0,0, dz, 0);
        return true;
    }

    // TODO check axis orientation !!

    /**
     * Make the drone descend by dz using moveBy
     * @param dz (float)
     * @return true (boolean)
     */
    boolean descendBy(float dz){
        mSKEModule.moveBy(0,0, -dz, 0);
        return true;
    }


    /**
     * Make the drone get closer to the central point of its vision using moveBy.
     * Use the tilt angle and assume x axis is always oriented with the drone horizontal vision.
     * @return true (boolean)
     */
    boolean getCloserTo(){
        double dTilt = (double)tilt;
        // TODO Check the tilt positive direction
        mSKEModule.moveBy((float)Math.cos(dTilt),0, (float)Math.sin(dTilt), 0);
        return true;
    }

    /**
     * Cancel previous moveBy to stabilize the drone when got closer to
     * @return true (boolean)
     */
    boolean stopGetCloserTo(){
        mSKEModule.moveBy(0,0, 0, 0);
        return false;
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

    /**
     * Make the drone take off automatically
     * @return true (boolean)
     */
    boolean autoTakeOff(){
        mSKEModule.takeOff();
        return true;
    }

    /**
     * Make the drone land automatically
     * @return true (boolean)
     */
    public boolean autoLand(){
        mSKEModule.land();
        return true;
    }

    /**
     * Start a Mission or continue it if already started
     */
    void startMission(){
        DroneApplication.getApplication().getConsoleMessage().pushMessage("Start or continue Mission");
        currentMissionRunning = true;
        currentMission.start();
    }

    /**
     * Pause the current mission before next step.
     * Autopilot disengage will pause the mission in the current step
     * Warning: Hence Please use this method only on autopilot disengage call
     */
    void pauseMission(){
        currentMissionRunning = false;
        currentMission.pause();
    }

    /**
     * Start the Happy Mission if no mission is currently running
     */
    void startHappy() {
        if (!currentMissionRunning){
            currentMission = happyMission;
            currentMission.reset();
            startMission();
        }
    }

    /**
     * Start properly the Angry Mission if no mission is currently running
     */
    void startAngry(){
        if (!currentMissionRunning) {
            currentMission = angryMission;
            currentMission.reset();
            startMission();
        }
    }

    /**
     * Start properly the  Unkonwn Mission if no mission is currently running
     */
    void startUnknown(){
        if (!currentMissionRunning) {
            currentMission = unknownMission;
            currentMission.reset();
            startMission();
        }
    }

    /**
     * Start properly the Part1 Mission if no mission is currently running
     */
    void startFinalPart1(){
        if (!currentMissionRunning) {
            currentMission = finalMissionPart1;
            currentMission.reset();
            startMission();
        }
    }

    /**
     * Abstract Mission Class:
     * An iterative mission is made of a list of procedures. Each procedure is processed one after
     * the others. The Mission can wait specific callbacks before iterating.
     * Current mission procedure is stored in currentProcedure variable.
     */
    abstract class Mission{

        int currentProcedure = -1; // -1 is not starting yet
        ArrayList<Procedure> procedures = new ArrayList<>();

        // "Wait for callback" flags
        boolean pauseMission = true, waitForConfigEnd, waitForFlyingStateChanged, waitForPlayingStateChanged, waitForMoveByEnd;

        /**
         * Reset the mission. This method implies on next start, init will be called
         * @return true (boolean)
         */
        boolean reset(){
            currentProcedure = -1;
            procedures = new ArrayList<>();
            pauseMission = true;
            waitForConfigEnd = false; waitForFlyingStateChanged = false; waitForPlayingStateChanged = false; waitForMoveByEnd = false;
            return true;
        }

        /**
         * Start the mission. If the mission is already started, this method continue it
         * - In the very special case where the mission was in a autonomous flight plan before the pause
         *   and because the mission is waiting for flight plan end, the next() will do nothing.
         *   By the autopilot re-engage, the flight plan will continue and call next() by itself once ended
         * - In any other case, the current mission procedure is dropped and waiting flags are reset to false.
         *   Warning: Take into account the last procedure (before the pause) was maybe not ended, especially if
         *   the next one (after the pause) assumes it.
         * @return (boolean)
         */
        boolean start(){
            DroneApplication.getApplication().getConsoleMessage().pushMessage("Init");
            pauseMission = false;
            waitForConfigEnd = false; waitForFlyingStateChanged = false; waitForPlayingStateChanged = false; waitForMoveByEnd = false;

            // Continue
            if(currentProcedure > -1 ) {
                return next();
            }
            // Init and start
            return init() && next();
        }

        /**
         * Pause the current mission before the next procedure in the current states
         * If the mission is paused and the manual control not handel, the drone can be dangerous
         * i.e. if the drone is not hovering, it will continue on its current states (climbing, moving, etc.)
         * Hence please use this pause method only on autopilot disengage !
         * @return true (boolean)
         */
        boolean pause(){
            pauseMission = true;
            return true;
        }

        /**
         * This method must be implemented in all mission. It initialize the iterative procedure list
         * @return (boolean)
         */
        abstract boolean init();

        /**
         * Proceed the next not already processed procedure if the mission is not waiting for any callback.
         * Call @see end if the iterative list is ended
         * @return (boolean)
         */
        boolean next(){
            while(!pauseMission && !waitForMoveByEnd && !waitForConfigEnd && !waitForFlyingStateChanged
                    && procedures.size() > currentProcedure+1){
                currentProcedure++;
                procedures.get(currentProcedure).process();
            }
            if(currentProcedure == procedures.size()-1){
                end();
            }
            return true;
        }

        /**
         * Indicate the linked autopilot this mission is ended.
         */
        void end(){
            mAutopilot.endMission();
        }

        /**
         * Append a other mission at the end of the current one.
         * This allow to have "tree mission" where next procedure depend of the result in current one
         * We can also start a completely new Mission if we wont end of memory
         * @param other (Mission): other mission to append
         */
        void append(Mission other){
            procedures.addAll(other.procedures);
        }
    }

    /**
     * All procedures are made of a single process method which return a boolean as success
     */
    interface Procedure{
        boolean process();
    }

    /**
     * Example of a iterative mission. This first mission proceed:
     *  - Config and wait end of config
     *  - hovering while turning right for 2 second
     *  - Stop turning right
     *  - Climb for 2 second
     *  - Stop climbing
     */
    private Mission firstMission = new Mission() {

        @Override
        public boolean init(){

            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    waitForConfigEnd = true;
                    mSKEModule.setDroneConfig(ConfigDrone.INDOOR_DRONE_CONFIG);
                    return true;
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

            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    return climb();
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
                    return stabilizeVertically();
                }
            });

            return true;
        }
    };

    /**
     * This Mission is made of the following procedures:
     *  - Config the drone and wait end of config
     *  - Take-off and wait end of take-off
     *  - Proceed Mavlink flight plan to reach the Mission starting point.
     *  - Start hovering and turning right to see periodically around
     *  - Wait for the user inputs
     */
    private Mission finalMissionPart1 = new Mission() {

        @Override
        public boolean init(){

            // Config
            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    waitForConfigEnd = true;
                    mSKEModule.setDroneConfig(ConfigDrone.INDOOR_DRONE_CONFIG);
                    return true;
                }
            });

            // Take-off
            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    waitForFlyingStateChanged = true;
                    return autoTakeOff();
                }
            });

            // Start the Flight plan mission
            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    waitForPlayingStateChanged = true;
                    mAutopilot.startAutoFlightPlan();
                    return mAutopilot.isEngaged();
                }
            });

            // Hovering by turning right once destination is reach
            procedures.add(new Procedure() {
                @Override
                public boolean process() {
                    return turnRight();
                }
            });

            // Wait for blob selection, become closer, and face detection/recognition

            return true;
        }
    };

    /**
     * If the drone is happy !
     *  - Climb and wait end of climb
     *  - Flip if enough battery
     *  - descend and wait end of descend
     */
    private Mission happyMission = new Mission() {

        @Override
        public boolean init(){

            return true;
        }
    };

    /**
     * If the drone is angry !
     *  - Make fast climb/descend operation while getting closer from the enemy.
     *  - For security implement the moving forward in last ! // TODO
     */
    private Mission angryMission = new Mission() {

        @Override
        public boolean init(){

            return true;
        }
    };

    /**
     * If the drone unknown
     *  - Making small movement to make the subject confident and allowing several picturing of it
     *    for the training model
     */
    private Mission unknownMission = new Mission() {

        @Override
        public boolean init(){

            return true;
        }
    };




    // The listener part. Get callback from the drone

    @Override
    public void onStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
    }

    @Override
    public void onExtensionStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARDISCOVERY_PRODUCT_ENUM product, String name, ARCONTROLLER_ERROR_ENUM error) {
    }

    @Override
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {

        // This event is called after the last config update
        if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_SPEEDSETTINGSSTATE_HULLPROTECTIONCHANGED) && (elementDictionary != null)){
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (currentMission.waitForConfigEnd && args != null) {
                currentMission.waitForConfigEnd = false;
                currentMission.next();
            }
        }

        // if event received is the flying state update
        else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED) && (elementDictionary != null)) {
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {
                final ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE));

                if(currentMission.waitForFlyingStateChanged && (state == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING ||state == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING)){
                    currentMission.waitForFlyingStateChanged = false;
                    currentMission.next();
                }
            }
        }

        // End of MoveBy
        if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGEVENT_MOVEBYEND) && (elementDictionary != null)){
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {

                if(currentMission.waitForMoveByEnd){
                    currentMission.waitForMoveByEnd = false;
                    currentMission.next();
                }
            }
        }

        // End of the flight plan
        if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_MAVLINKSTATE_MAVLINKFILEPLAYINGSTATECHANGED) && (elementDictionary != null)){
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {
                ARCOMMANDS_COMMON_MAVLINKSTATE_MAVLINKFILEPLAYINGSTATECHANGED_STATE_ENUM state = ARCOMMANDS_COMMON_MAVLINKSTATE_MAVLINKFILEPLAYINGSTATECHANGED_STATE_ENUM.getFromValue((Integer)args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_MAVLINKSTATE_MAVLINKFILEPLAYINGSTATECHANGED_STATE));
                if(state == ARCOMMANDS_COMMON_MAVLINKSTATE_MAVLINKFILEPLAYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_COMMON_MAVLINKSTATE_MAVLINKFILEPLAYINGSTATECHANGED_STATE_STOPPED){

                    mAutopilot.endFlightPlan();

                    if(currentMission.waitForPlayingStateChanged){
                        currentMission.waitForPlayingStateChanged = false;
                        currentMission.next();
                    }
                }
            }
        }
    }

}