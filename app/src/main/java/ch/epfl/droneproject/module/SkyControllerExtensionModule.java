package ch.epfl.droneproject.module;

import android.content.Context;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARDeviceController;

import ch.epfl.droneproject.drone.ConfigDrone;

public class SkyControllerExtensionModule {


    private Context mContext;
    private ARDeviceController mDeviceController;
    private ARCONTROLLER_DEVICE_STATE_ENUM mSkyControllerState;
    private FlightPlanModule mFlightPlanModule;
    private AutoPilotModule mAutoPilot;


    /**
     * Default SkyController 2 Extension controller
     * @param deviceController
     * @param skyController2State
     */
    public SkyControllerExtensionModule(Context context, ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM skyController2State) {
        this.mContext = context;
        this.mDeviceController = deviceController;
        this.mSkyControllerState = skyController2State;
        this.mFlightPlanModule = new FlightPlanModule(context);
    }

    public void setDroneConfig(ConfigDrone config){

        if ((mDeviceController != null) &&
                (mSkyControllerState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) &&
                (mDeviceController.getExtensionState().equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {

            mDeviceController.getFeatureARDrone3().sendPilotingSettingsMaxAltitude(config.getMaxAlt());
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsMaxDistance(config.getMaxDistance());
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsNoFlyOverMaxDistance(config.getShouldNotFlyOver());

            mDeviceController.getFeatureARDrone3().sendPilotingSettingsMaxTilt(config.getMaxTilt());
            mDeviceController.getFeatureARDrone3().sendSpeedSettingsMaxPitchRollRotationSpeed(config.getMaxTiltS());

            mDeviceController.getFeatureARDrone3().sendPilotingSettingsBankedTurn(config.getBankedTurn());

            mDeviceController.getFeatureARDrone3().sendPilotingSettingsSetAutonomousFlightMaxHorizontalSpeed(config.getMaxAutonomousHS());
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsSetAutonomousFlightMaxVerticalSpeed(config.getMaxAutonomousVS());
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsSetAutonomousFlightMaxHorizontalAcceleration(config.getMaxAutonomousHA());
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsSetAutonomousFlightMaxVerticalAcceleration(config.getMaxAutonomousVA());
            mDeviceController.getFeatureARDrone3().sendPilotingSettingsSetAutonomousFlightMaxRotationSpeed(config.getMaxAutonomousRS());

            mDeviceController.getFeatureARDrone3().sendSpeedSettingsMaxVerticalSpeed(config.getMaxVS());
            mDeviceController.getFeatureARDrone3().sendSpeedSettingsMaxRotationSpeed(config.getMaxRS());

            mDeviceController.getFeatureARDrone3().sendSpeedSettingsHullProtection(config.getHasHullProtection());
        }
    }



    /**
     * Do a flat trim of the accelerometer/gyro.
     * Could be useful when the drone is sliding in hover mode.
     * Result:
     * Accelerometer and gyroscope are calibrated then event FlatTrimChanged is triggered.
     */
    public void flatTrim(){
        if ((mDeviceController != null) &&
                (mSkyControllerState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) &&
                (mDeviceController.getExtensionState().equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingFlatTrim();
        }
    }


    /**
     * Move the drone.
     * The libARController is sending the command each 50ms.
     * @param flag (u8): Boolean flag: 1 if the roll and pitch values should be taken in consideration. 0 otherwise
     * @param roll (i8): Roll angle as signed percentage.
     *             Roll angle expressed as signed percentage of the max pitch/roll setting, in range [-100, 100].
     *             -100 corresponds to a roll angle of max pitch/roll to the left (drone will fly left)
     *             100 corresponds to a roll angle of max pitch/roll to the right (drone will fly right)
     *             This value may be clamped if necessary, in order to respect the maximum supported physical tilt of the copter.
     * @param pitch (i8): Pitch angle as signed percentage.
     *              Expressed as signed percentage of the max pitch/roll setting, in range [-100, 100]
     *              -100 corresponds to a pitch angle of max pitch/roll towards sky (drone will fly backward)
     *              100 corresponds to a pitch angle of max pitch/roll towards ground (drone will fly forward)
     *              This value may be clamped if necessary, in order to respect the maximum supported physical tilt of the copter.
     * @param yaw (i8): Yaw rotation speed as signed percentage.
     *            Expressed as signed percentage of the max yaw rotation speed setting, in range [-100, 100].
     *            -100 corresponds to a counter-clockwise rotation of max yaw rotation speed
     *            100 corresponds to a clockwise rotation of max yaw rotation speed
     *            This value may be clamped if necessary, in order to respect the maximum supported physical tilt of the copter.
     * @param gaz (i8): Throttle as signed percentage.
     *            Expressed as signed percentage of the max vertical speed setting, in range [-100, 100]
     *            -100 corresponds to a max vertical speed towards ground
     *            100 corresponds to a max vertical speed towards sky
     *            This value may be clamped if necessary, in order to respect the maximum supported physical tilt of the copter.
     *            During the landing phase, putting some positive gaz will cancel the land.
     * @param timestampAndSeqNum (u32): Command timestamp in milliseconds (low 24 bits) + command sequence number (high 8 bits) [0;255].
     *
     *
     * Result:
     * The drone moves!
     * Event SpeedChanged, AttitudeChanged and PositionChanged (only if gps of the drone has fixed) are triggered.
     */
    public void setPCDM(byte flag, byte roll, byte pitch, byte yaw, byte gaz, int timestampAndSeqNum){
        if ((mDeviceController != null) &&
                (mSkyControllerState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) &&
                (mDeviceController.getExtensionState().equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMD(flag, roll, pitch, yaw, gaz, timestampAndSeqNum);
        }
    }


    /**
     * Ask the drone to take off.
     * Result:
     * The drone takes off if its FlyingState was landed.
     * Then, event FlyingState is triggered.
     */
    public void takeOff() {
        if ((mDeviceController != null) &&
                (mSkyControllerState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) &&
                (mDeviceController.getExtensionState().equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingTakeOff();
        }
    }

    /**
     * Land.
     * Please note that, if you put some positive gaz (in the PilotingCommand) during the landing, it will cancel it.
     * Result:
     * The drone lands if its FlyingState was taking off, hovering or flying.
     * Then, event FlyingState is triggered.
     */
    public void land() {
        if ((mDeviceController != null) &&
                (mSkyControllerState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) &&
                (mDeviceController.getExtensionState().equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingLanding();
        }
    }

    /**
     * Cut out the motors for emergency.
     * This cuts immediatly the motors. The drone will fall.
     * This command is sent on a dedicated high priority buffer which will infinitely retry to send it if the command is not delivered.
     * Result:
     * The drone immediatly cuts off its motors.
     * Then, event FlyingState is triggered.
     */
    public void emergency() {
        if ((mDeviceController != null) &&
                (mSkyControllerState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) &&
                (mDeviceController.getExtensionState().equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingEmergency();
        }
    }

    /**
     * Return home.
     * Ask the drone to fly to its HomePosition.
     * The availability of the return home can be get from ReturnHomeState.
     * Please note that the drone will wait to be hovering to start its return home.
     * This means that it will wait to have a flag set at 0.
     * @param start (u8): 1 to start the navigate home, 0 to stop it
     * Result:
     * The drone will fly back to its home position.
     * Then, event ReturnHomeState is triggered.
     * You can get a state pending if the drone is not ready to start its return home process but will
     * do it as soon as it is possible.
     */
    public void goHome(byte start){
        if ((mDeviceController != null) &&
                (mSkyControllerState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) &&
                (mDeviceController.getExtensionState().equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingNavigateHome(start);
        }
    }

    /**
     * Move the drone to a relative position and rotate heading by a given angle.
     * Moves are relative to the current drone orientation, (drone’s reference).
     * Also note that the given rotation will not modify the move (i.e. moves are always rectilinear).
     * @param dX (float): Wanted displacement along the front axis [m]
     * @param dY (float): Wanted displacement along the right axis [m]
     * @param dZ (float): Wanted displacement along the down axis [m]
     * @param dPsi (float): Wanted rotation of heading [rad]
     *
     * The drone will move of the given offsets.
     * Then, event RelativeMoveEnded is triggered.
     * If you send a second relative move command, the drone will trigger a RelativeMoveEnded with the
     * offsets it managed to do before this new command and the value of error set to interrupted.
     */
    public void moveBy(float dX, float dY, float dZ, float dPsi) {
        if ((mDeviceController != null) &&
                (mSkyControllerState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) &&
                (mDeviceController.getExtensionState().equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendPilotingMoveBy(dX, dY, dZ, dPsi);
        }
    }


    /**
     * Make a flip.
     * @param direction (enum): Direction for the flip
     *                  front: Flip direction front
     *                  back: Flip direction back
     *                  right: Flip direction right
     *                  left: Flip direction left
     *
     * Result:
     * The drone will make a flip if it has enough battery.
     */
    public void makeAFlip(ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM direction){
        if ((mDeviceController != null) &&
                (mSkyControllerState.equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)) &&
                (mDeviceController.getExtensionState().equals(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING))) {
            mDeviceController.getFeatureARDrone3().sendAnimationsFlip(direction);
        }

    }
}
