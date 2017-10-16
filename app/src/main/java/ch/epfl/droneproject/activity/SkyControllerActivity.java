package ch.epfl.droneproject.activity;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

import ch.epfl.droneproject.R;
import ch.epfl.droneproject.drone.SkyControllerDrone;


public class SkyControllerActivity extends AppCompatActivity {

    private static final String TAG = "SkyControllerActivity";
    private SkyControllerDrone mSkyControllerDrone;

    private ProgressBar progressBar;
    private TextView progressText;

    private VideoFragment mVideoFragment;
    private MapsFragment mMapFragment;

    private Button mDownloadBt;
    private int mNbMaxDownload;
    private int mCurrentDownloadIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sky_controller);

        // Init the Interface (button, etc...)
        initIHM();

        // Init the Device service and the SkyControllerDrone. Make this activity listen the SkyControllerDrone instance
        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        //mSkyControllerDrone = new SkyControllerDrone(this);
        mSkyControllerDrone = new SkyControllerDrone(this, service);
        mSkyControllerDrone.addListener(mSkyControllerListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // show a loading view while the bebop drone is connecting
        //if (false && (mSkyControllerDrone != null) &&
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
    public void onDestroy(){
        mSkyControllerDrone.dispose();
        super.onDestroy();
    }


    /**
     * Initialize all the the UI components and set all needed listener
     */
    private void initIHM() {

        // Init the pager with the two fragment adapter
        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new PagerAdapter(viewPager, getSupportFragmentManager()));

        // Init the progress tool
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressText = (TextView) findViewById(R.id.progressText);

        // init the emergency button
        findViewById(R.id.emergencyBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSkyControllerDrone.skeModule().emergency();
            }
        });

        // inti the download button
        mDownloadBt = (Button)findViewById(R.id.downloadBt);
        mDownloadBt.setEnabled(false);
        mDownloadBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showProgressBar(getResources().getString(R.string.fetch));
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
                mSkyControllerDrone.skeModule().startFlightPlan();
            }
        });
        (findViewById(R.id.pauseFLPBt)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSkyControllerDrone.skeModule().pauseFlightPlan();
            }
        });
        (findViewById(R.id.stopFLPBt)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSkyControllerDrone.skeModule().stopFlightPlan();
            }
        });
        (findViewById(R.id.flatTrimBt)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (mSkyControllerDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mSkyControllerDrone.skeModule().flatTrim();
                        break;
                    default:
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
                    mVideoFragment = new VideoFragment();
                    return mVideoFragment;
                case 1:
                    mMapFragment = new MapsFragment().init(mSkyControllerDrone.skeModule().getFlightPlanModule());
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
            VideoFragment.pushInConsole("SkyCon State:"+state);

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

            VideoFragment.pushInConsole("DroneCon State:"+state);
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

            VideoFragment.pushInConsole("Pilot State:"+state);

            switch (state) {
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    mDownloadBt.setEnabled(true);
                    break;
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    mDownloadBt.setEnabled(false);
                    break;
                default:
                    mDownloadBt.setEnabled(false);
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
        public void onMatchingMediasFound(int nbMedias) {

            hideProgressBar();

            mNbMaxDownload = nbMedias;
            mCurrentDownloadIndex = 1;

            if (nbMedias > 0) {
                showProgressBar(getResources().getString(R.string.downloadMedia));
            }
        }

        @Override
        public void onDownloadProgressed(String mediaName, int progress) {
        }

        @Override
        public void onDownloadComplete(String mediaName) {
            mCurrentDownloadIndex++;

            if (mCurrentDownloadIndex > mNbMaxDownload) {
                hideProgressBar();
            }
        }
    };

}
