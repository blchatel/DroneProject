package ch.epfl.droneproject.activity;

import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

import java.util.Locale;

import ch.epfl.droneproject.DroneApplication;
import ch.epfl.droneproject.R;
import ch.epfl.droneproject.drone.ConfigDrone;
import ch.epfl.droneproject.drone.SkyControllerDrone;

import static com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING;
import static com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING;


/**
 * SkyControllerActivity.java
 * @author blchatel
 *
 * Class from Parrot Samples adapted for the need of this project
 *
 */
public class SkyControllerActivity extends AppCompatActivity {

    private static final String TAG = "SkyControllerActivity";
    private SkyControllerDrone mSkyControllerDrone;

    private ProgressBar progressBar;
    private TextView progressText;

    private VideoFragment mVideoFragment;
    private MapsFragment mMapFragment;


    private Button mAutoPilotBt;
    private Button mCloserBt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("CYCLE", "Create");

        setContentView(R.layout.activity_sky_controller);

        // Init the Interface (button, etc...)
        initIHM();

        // Init the Device service and the SkyControllerDrone. Make this activity listen the SkyControllerDrone instance
        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        mSkyControllerDrone = new SkyControllerDrone(this, service);
        mSkyControllerDrone.addListener(mSkyControllerListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("CYCLE", "Start");

        // show a loading view while the bebop drone is connecting
        if ((mSkyControllerDrone != null) &&
                !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mSkyControllerDrone.getSkyControllerConnectionState()))){

            showProgressBar(getResources().getString(R.string.connection));
            // if the connection to the Bebop fails, finish the activity
            if (!mSkyControllerDrone.connect()) {
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {

        // Disconnect from the drone and controller
        if (mSkyControllerDrone != null)
        {
            showProgressBar(getResources().getString(R.string.disconnection));

            if (!mSkyControllerDrone.disconnect()) {
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onPause() {
        Log.d("CYCLE", "Pause");
        // i.e. home button we want to disconnect
        if (!this.isFinishing()){
            this.onBackPressed();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("CYCLE", "Stop");
        super.onStop();
    }

    @Override
    public void onDestroy(){
        Log.d("CYCLE", "Destroy");
        mSkyControllerDrone.dispose();
        super.onDestroy();
    }


    /**
     * Initialize all the the UI components and set all needed listener
     */
    private void initIHM() {

        // Init the pager with the two fragment adapter
        final ViewPager viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(new PagerAdapter(viewPager, getSupportFragmentManager()));

        // Init the progress tool
        progressBar =  findViewById(R.id.progressBar);
        progressText =  findViewById(R.id.progressText);

        // init the emergency button
        findViewById(R.id.emergencyBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSkyControllerDrone.skeModule().emergency();
            }
        });

        // init the autopilot button
        mAutoPilotBt = findViewById(R.id.autopilot);
        mAutoPilotBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mSkyControllerDrone.autoPilotModule().isEngaged()){
                    // disengage
                    mSkyControllerDrone.autoPilotModule().disengage();
                    mAutoPilotBt.setTextColor(Color.RED);
                    mAutoPilotBt.setText(R.string.engageap);
                    mAutoPilotBt.setBackgroundResource(R.drawable.emergency_btn);
                }else{
                    // engage
                    mSkyControllerDrone.autoPilotModule().engage();
                    mAutoPilotBt.setTextColor(Color.GREEN);
                    mAutoPilotBt.setText(R.string.disengageap);
                    mAutoPilotBt.setBackgroundResource(R.drawable.green_btn);
                }
            }
        });

        mCloserBt = findViewById(R.id.closerBtn);
        mCloserBt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                boolean valid = mSkyControllerDrone.getFlyingState() == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING ||
                        mSkyControllerDrone.getFlyingState() == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if(valid) {
                            v.setPressed(true);
                            mSkyControllerDrone.autoPilotModule().getCloserTo();
                        }
                        break;

                    case MotionEvent.ACTION_UP:

                        if(valid) {
                            v.setPressed(false);
                            mSkyControllerDrone.autoPilotModule().stopGetCloserTo();
                        }
                        break;

                    default:
                        break;
                }
                return true;
            }

        });


        (findViewById(R.id.leftFlipBt)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mSkyControllerDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mSkyControllerDrone.skeModule().makeAFlip(ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_LEFT);
                        break;
                    default:
                }
            }
        });
        (findViewById(R.id.deltaFrontBt)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mSkyControllerDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mSkyControllerDrone.skeModule().moveBy(1, 0, 0, 0);
                        break;
                    default:
                }
            }
        });
        (findViewById(R.id.deltaBackBt)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mSkyControllerDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mSkyControllerDrone.skeModule().moveBy(-1, 0, 0, 0);
                        break;
                    default:
                }
            }
        });
        (findViewById(R.id.startFLPBt)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSkyControllerDrone.autoPilotModule().startAutoFlightPlan();
            }
        });
        (findViewById(R.id.pauseFLPBt)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSkyControllerDrone.autoPilotModule().pauseAutoFlightPlan();
            }
        });
        (findViewById(R.id.stopFLPBt)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSkyControllerDrone.autoPilotModule().stopAutoFlightPlan();
            }
        });
        (findViewById(R.id.startMission)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSkyControllerDrone.autoPilotModule().startMission();

            }
        });
        (findViewById(R.id.flatTrimBt)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mSkyControllerDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mSkyControllerDrone.skeModule().setDroneConfig(ConfigDrone.MISSION_DRONE_CONFIG);
                        DroneApplication.pushInfoMessage("Config");
                        break;
                    default:
                        break;
                }
            }
        });
    }


    /**
     * Show the progress bar with the text label
     * @param text (String): progression label
     */
    private void showProgressBar(String text){
        progressBar.setVisibility(View.VISIBLE);
        progressText.setText(text);
        progressText.setVisibility(View.VISIBLE);
    }

    /**
     * Hide the progression bar and its text label
     */
    private void hideProgressBar(){
        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
    }


    /**
     * Page adapter for the two fragment.
     * - Video fragment
     * - Map fragment
     * It is implemented in a way we only swipe right to change the "tab"
     */
    private class PagerAdapter extends FragmentStatePagerAdapter {

        /**
         * There is two tab, the video fragment and the map fragment
         */
        private static final int NUM_OF_TAB = 2;

        private PagerAdapter(final ViewPager pager, FragmentManager fm) {
            super(fm);

            // Listener to set selected tab to 0 when we swipe right on the last tab
            // to simulate a loop swiper
            pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageSelected(int position) {
                    if (position == getCount()-1){
                        pager.setCurrentItem(0, true);
                    }
                }
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
                @Override
                public void onPageScrollStateChanged(int state) {}
            });
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    mVideoFragment = new VideoFragment().init(mSkyControllerDrone.autoPilotModule());
                    return mVideoFragment;
                case 1:
                    mMapFragment = new MapsFragment().init(mSkyControllerDrone.autoPilotModule().getFlightPlanerModule());
                    return mMapFragment;
                case 2:
                    return new Fragment();
                default:
                    return null;
            }
        }
        @Override
        public int getCount() {
            // Return +1 because we need an empty buffer tab at the end to switch back to first one
            return NUM_OF_TAB+1;
        }
    }

    /**
     * Listener which listen SkyControllerDrone instance for this SkyController Activity
     */
    private final SkyControllerDrone.Listener mSkyControllerListener = new SkyControllerDrone.Listener() {
        @Override
        public void onSkyControllerConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            DroneApplication.pushInfoMessage("SkyCon State:"+state);

            switch (state)
            {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    hideProgressBar();
                    // if no drone is connected, display a message
                    if (!ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mSkyControllerDrone.getDroneConnectionState())) {
                        mVideoFragment.makeConnectionLabelVisible(true);
                    }
                    break;

                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    // if the deviceController is stopped, go back to the previous activity
                    hideProgressBar();
                    finish();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            DroneApplication.pushInfoMessage("DroneCon State:"+state);
            switch (state)
            {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    mVideoFragment.makeConnectionLabelVisible(false);
                    break;
                default:
                    mVideoFragment.makeConnectionLabelVisible(true);
                    break;
            }
        }

        @Override
        public void onSkyControllerBatteryChargeChanged(int batteryPercentage) {
            mVideoFragment.setControllerBatteryLabel(String.format(Locale.getDefault(),"%d%%", batteryPercentage));
        }

        @Override
        public void onDroneBatteryChargeChanged(int batteryPercentage) {
            mVideoFragment.setDroneBatteryLabel(String.format(Locale.getDefault(), "%d%%", batteryPercentage));
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            DroneApplication.pushInfoMessage("Pilot State:"+state);

            switch (state) {
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    break;
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
            Log.i(TAG, "Picture has been taken");
        }

        @Override
        public void configureDecoder(ARControllerCodec codec) {
            mVideoFragment.configureDecoder(codec);
        }

        @Override
        public void onFrameReceived(ARFrame frame) {
            mVideoFragment.displayFrame(frame);
        }

        @Override
        public void onAutoPilotDisengage() {
            DroneApplication.pushInfoMessage("Disengage AP");
            mAutoPilotBt.setTextColor(Color.RED);
            mAutoPilotBt.setText(R.string.engageap);
            mAutoPilotBt.setBackgroundResource(R.drawable.emergency_btn);
        }

        @Override
        public void onDronePositionChange() {
            mMapFragment.drawDrone();
        }
    };
}
