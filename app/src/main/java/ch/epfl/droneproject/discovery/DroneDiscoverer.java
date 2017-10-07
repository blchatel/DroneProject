package ch.epfl.droneproject.discovery;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import java.util.ArrayList;
import java.util.List;

public class DroneDiscoverer
{
    private static final String TAG = "DroneDiscoverer";

    public interface Listener {
        /**
         * Called when the list of seen drones is updated
         * Called in the main thread
         * @param dronesList list of ARDiscoveryDeviceService which represents all available drones
         *                   Content of this list respect the drone types given in startDiscovery
         */
        void onDronesListUpdated(List<ARDiscoveryDeviceService> dronesList);
    }

    /** */
    private final List<Listener> mListeners;

    /** */
    private final Context mCtx;

    /** */
    private ARDiscoveryService mARDiscoveryService;

    /** */
    private ServiceConnection mARDiscoveryServiceConnection;

    /** */
    private final ARDiscoveryServicesDevicesListUpdatedReceiver mARDiscoveryServicesDevicesListUpdatedReceiver;

    /** */
    private final List<ARDiscoveryDeviceService> mMatchingDrones;

    /** */
    private boolean mStartDiscoveryAfterConnection;


    /**
     * Default Drone Discoverer constructor
     * @param ctx : Context
     */
    public DroneDiscoverer(Context ctx) {
        mCtx = ctx;

        // Init the listeners, the matching drones and the discovery services
        mListeners = new ArrayList<>();
        mMatchingDrones = new ArrayList<>();
        mARDiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(mDiscoveryListener);
    }

    /**
     * Add a listener
     * All callbacks of the interface Listener will be called within this function
     * Should be called in the main thread
     * @param listener an object that implements the {@link Listener} interface
     */
    public void addListener(Listener listener) {
        mListeners.add(listener);

        // notify the matching drones we added a listener
        notifyServiceDiscovered(mMatchingDrones);
    }

    /**
     * remove a listener from the listener list
     * @param listener an object that implements the {@link Listener} interface
     */
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
        // TODO Why not notify as in add listener ?
    }

    /**
     * Setup the drone discoverer
     * Should be called before starting discovering
     */
    public void setup() {
        // registerReceivers
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(mCtx);
        localBroadcastMgr.registerReceiver(mARDiscoveryServicesDevicesListUpdatedReceiver,
                new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));

        // create the service connection if not already created
        if (mARDiscoveryServiceConnection == null) {
            mARDiscoveryServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mARDiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();

                    if (mStartDiscoveryAfterConnection) {
                        startDiscovering();
                        mStartDiscoveryAfterConnection = false;
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mARDiscoveryService = null;
                }
            };
        }

        if (mARDiscoveryService == null) {
            // if the discovery service doesn't exists, bind to it
            Intent i = new Intent(mCtx, ARDiscoveryService.class);
            mCtx.bindService(i, mARDiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Cleanup the object
     * Should be called when the object is not used anymore
     */
    public void cleanup() {
        stopDiscovering();
        //close discovery service
        Log.d(TAG, "closeServices ...");

        if (mARDiscoveryService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mARDiscoveryService.stop();

                    mCtx.unbindService(mARDiscoveryServiceConnection);
                    mARDiscoveryService = null;
                }
            }).start();
        }

        // unregister receivers
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(mCtx);
        localBroadcastMgr.unregisterReceiver(mARDiscoveryServicesDevicesListUpdatedReceiver);
    }

    /**
     * Start discovering Parrot drones
     * For Wifi drones, the device should be on the drone's network
     * When drones will be discovered, you will be notified through {@link Listener#onDronesListUpdated(List)}
     */
    public void startDiscovering() {
        if (mARDiscoveryService != null) {
            Log.i(TAG, "Start discovering");
            mDiscoveryListener.onServicesDevicesListUpdated();
            mARDiscoveryService.start();
            mStartDiscoveryAfterConnection = false;
        } else {
            mStartDiscoveryAfterConnection = true;
        }
    }

    /**
     * Stop discovering Parrot drones
     */
    public void stopDiscovering() {
        if (mARDiscoveryService != null) {
            Log.i(TAG, "Stop discovering");
            mARDiscoveryService.stop();
        }
        mStartDiscoveryAfterConnection = false;
    }

    private void notifyServiceDiscovered(List<ARDiscoveryDeviceService> dronesList) {
        Log.e("REFRESH", "NOTIFY");
        List<Listener> listenersCpy = new ArrayList<>(mListeners);
        for (Listener listener : listenersCpy) {
            listener.onDronesListUpdated(dronesList);
        }
    }

    private final ARDiscoveryServicesDevicesListUpdatedReceiverDelegate mDiscoveryListener =
            new ARDiscoveryServicesDevicesListUpdatedReceiverDelegate() {
                @Override
                public void onServicesDevicesListUpdated() {
                    if (mARDiscoveryService != null) {
                        // clear current list
                        mMatchingDrones.clear();
                        List<ARDiscoveryDeviceService> deviceList = mARDiscoveryService.getDeviceServicesArray();

                        if (deviceList != null)
                        {
                            for (ARDiscoveryDeviceService service : deviceList)
                            {
                                mMatchingDrones.add(service);
                            }
                        }
                        notifyServiceDiscovered(mMatchingDrones);
                    }
                }
            };
}
