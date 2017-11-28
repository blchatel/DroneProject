package ch.epfl.droneproject.drone;

public class ConfigDrone {

    /**
     * Name of the configuration
     */
    private String configName;

    /**
     * Max altitude.
     * The drone will not fly over this max altitude when it is in manual piloting.
     * Please note that if you set a max altitude which is below the current drone altitude, the drone will not go to given max altitude.
     * You can get the bounds in the event MaxAltitude.
     * (float): [m]
     */
    private float maxAlt;

    /**
     * Max distance.
     * You can get the bounds from the event MaxDistance.
     * If Geofence is activated (i.e shouldNotFlyOver==true),
     * the drone won’t fly over the given max distance.
     * (float): [m]
     */
    private float maxDistance;

    /**
     * Geofence Flag.
     * If geofence is enabled, the drone won’t fly over the given maxDistance.
     * You can get the max distance from the event MaxDistance.
     * The distance is computed from the controller position, if this position is not known, it will use the take off.
     * (u8): 1 if the drone can’t fly further than max distance, 0 if no limitation on the drone should be done
     */
    private byte shouldNotFlyOver;

    /**
     * Max pitch/roll.
     * This represent the max inclination allowed by the drone.
     * You can get the bounds with the commands MaxPitchRoll.
     * (float): [degree]
     */
    private float maxTilt;

    /**
     * Set max pitch/roll (tilt) rotation speed.
     * (float): [degree/s]
     */
    private float maxTiltS;


    /**
     * Max vertical speed.
     * (float): [m/s]
     */
    private float maxVS;


    /**
     * Max yaw rotation speed.
     * (float): [degree/s]
     */
    private float maxRS;

    /**
     * Autonomous flight max horizontal speed.
     * This will only be used during autonomous flights such as moveBy.
     * (float): [m/s]
     */
    private float maxAutonomousHS;

    /**
     * Autonomous flight max vertical speed.
     * This will only be used during autonomous flights such as moveBy.
     * (float): [m/s]
     */
    private float maxAutonomousVS;

    /**
     * Autonomous flight max horizontal acceleration.
     * This will only be used during autonomous flights such as moveBy.
     * (float): [m/s2]
     */
    private float maxAutonomousHA;

    /**
     * Autonomous flight max vertical acceleration.
     * This will only be used during autonomous flights such as moveBy.
     * (float): [m/s2]
     */
    private float maxAutonomousVA;

    /**
     * Autonomous flight max yaw rotation speed.
     * This will only be used during autonomous flights such as moveBy.
     * (float): [deg/s]
     */
    private float maxAutonomousRS;

    /**
     * Banked flag for turn mode.
     * When banked turn mode is enabled, the drone will use yaw values from the piloting command to
     * infer with roll and pitch on the drone when its horizontal speed is not null.
     * (u8): 1 to enable, 0 to disable
     */
    private byte bankedTurn;


    /**
     * Presence flag of a hull protection.
     * (u8): 1 if present, 0 if not present
     */
    private byte hasHullProtection;


    /**
     * Default Config drone constructor. Wait for :
     * @param name: (String) Name of the configuration. Could be null
     * @param maxAlt: (float) [m]
     * @param maxDistance: (float) [m]
     * @param shouldNotFlyOver: (byte) [1|0]
     * @param maxTilt: (float) [degree]
     * @param maxTiltS: (float)[degree/s]
     * @param maxVS: (float) [m/s]
     * @param maxRS: (float) [m/s]
     * @param maxAutonomousHS: (float) [m/s]
     * @param maxAutonomousVS: (float) [m/s]
     * @param maxAutonomousHA: (float) [m/s2]
     * @param maxAutonomousVA: (float) [m/s2]
     * @param maxAutonomousRS: (float) [m/s]
     * @param bankedTurn: (byte) [1|0]
     * @param hasHullProtection: (byte) [1|0]
     */
    private ConfigDrone(String name, float maxAlt, float maxDistance, byte shouldNotFlyOver, float maxTilt,
                       float maxTiltS, float maxVS, float maxRS, float maxAutonomousHS, float maxAutonomousVS,
                       float maxAutonomousHA, float maxAutonomousVA, float maxAutonomousRS,
                       byte bankedTurn, byte hasHullProtection) {
        this.configName = name;
        this.maxAlt = maxAlt;
        this.maxDistance = maxDistance;
        this.shouldNotFlyOver = shouldNotFlyOver;
        this.maxTilt = maxTilt;
        this.maxTiltS = maxTiltS;
        this.maxVS = maxVS;
        this.maxRS = maxRS;
        this.maxAutonomousHS = maxAutonomousHS;
        this.maxAutonomousVS = maxAutonomousVS;
        this.maxAutonomousHA = maxAutonomousHA;
        this.maxAutonomousVA = maxAutonomousVA;
        this.maxAutonomousRS = maxAutonomousRS;
        this.bankedTurn = bankedTurn;
        this.hasHullProtection = hasHullProtection;
    }

    public static final ConfigDrone DFAULT_DRONE_CONFIG = new ConfigDrone(
            "Default Config",
            (float) 5,  //  float maxAlt [m]
            (float) 10,  //  float maxDistance [m]
            (byte) 1,   //  byte shouldNotFlyOver [1|0]
            (float) 10,  //  float maxTilt [degree]
            (float) 10,  //  float maxTiltS [degree/s]
            (float) 2,//  float maxVS [m/s]
            (float) 120, //  float maxRS [degree/s]
            (float) 2,  //  float maxAutonomousHS [m/s]
            (float) 2,//  float maxAutonomousVS [m/s]
            (float) 1,  //  float maxAutonomousHA [m/s2]
            (float) 1,//  float maxAutonomousVA [m/s2],
            (float) 120, //  float maxAutonomousRS [degree/s]
            (byte) 0,   //  byte bankedTurn [1|0]
            (byte) 0    //  byte hasHullProtection [1|0]
    );


    public static final ConfigDrone INDOOR_DRONE_CONFIG = new ConfigDrone(
            "Indoor Config",
            (float) 2,//  float maxAlt [m]
            (float) 3,  //  float maxDistance [m]
            (byte) 1,   //  byte shouldNotFlyOver [1|0]
            (float) 8,  //  float maxTilt [degree]
            (float) 8,  //  float maxTiltS [degree/s]
            (float) 0.5,//  float maxVS [m/s]
            (float) 60, //  float maxRS [degree/s]
            (float) 2,  //  float maxAutonomousHS [m/s]
            (float) 0.5,//  float maxAutonomousVS [m/s]
            (float) 1,  //  float maxAutonomousHA [m/s2]
            (float) 0.5,//  float maxAutonomousVA [m/s2],
            (float) 60, //  float maxAutonomousRS [degree/s]
            (byte) 0,   //  byte bankedTurn [1|0]
            (byte) 0    //  byte hasHullProtection [1|0]
    );


    /// GETTERS

    public String getConfigName(){
        if(configName != null)
            return configName;
        else
            return "unnamed";
    }

    public float getMaxAlt() {
        return maxAlt;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public byte getShouldNotFlyOver() {
        return shouldNotFlyOver;
    }

    public float getMaxTilt() {
        return maxTilt;
    }

    public float getMaxTiltS() {
        return maxTiltS;
    }

    public float getMaxVS() {
        return maxVS;
    }

    public float getMaxRS() {
        return maxRS;
    }

    public float getMaxAutonomousHS() {
        return maxAutonomousHS;
    }

    public float getMaxAutonomousVS() {
        return maxAutonomousVS;
    }

    public float getMaxAutonomousHA() {
        return maxAutonomousHA;
    }

    public float getMaxAutonomousVA() {
        return maxAutonomousVA;
    }

    public float getMaxAutonomousRS() {
        return maxAutonomousRS;
    }

    public byte getBankedTurn() {
        return bankedTurn;
    }

    public byte getHasHullProtection() {
        return hasHullProtection;
    }
}