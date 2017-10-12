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

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new PagerAdapter(viewPager, getSupportFragmentManager()));

        initIHM();

        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
//        mSkyControllerDrone = new SkyControllerDrone(this, service);
  //      mSkyControllerDrone.addListener(mSkyControllerListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // show a loading view while the bebop drone is connecting
        if ((mSkyControllerDrone != null) &&
                !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mSkyControllerDrone.getSkyControllerConnectionState())))
        {

            showProgressBar(getResources().getString(R.string.connection));
            // if the connection to the Bebop fails, finish the activity
            if (!mSkyControllerDrone.connect()) {
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
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
    public void onDestroy() {
        //mSkyControllerDrone.dispose();
        super.onDestroy();
    }


    private void initIHM() {

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressText = (TextView) findViewById(R.id.progressText);

        findViewById(R.id.emergencyBt).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSkyControllerDrone.skeModule().emergency();
            }
        });

        mDownloadBt = (Button)findViewById(R.id.downloadBt);
        mDownloadBt.setEnabled(false);
        mDownloadBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showProgressBar(getResources().getString(R.string.fetch));
            }
        });
    }

    private void showProgressBar(String text){
        progressBar.setVisibility(View.VISIBLE);// To Show ProgressBar
        progressText.setText(text);
        progressText.setVisibility(View.VISIBLE);
    }
    private void hideProgressBar(){
        progressBar.setVisibility(View.GONE);// To Show ProgressBar
        progressText.setVisibility(View.GONE);
    }



    private class PagerAdapter extends FragmentStatePagerAdapter {

        private static final int NUM_OF_TAB = 2;

        private PagerAdapter(final ViewPager pager, FragmentManager fm) {
            super(fm);

            pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageSelected(int position) {
                    if (position == getCount()-1){
                        pager.setCurrentItem(0, true);
                    }
                }
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }
                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    mVideoFragment = new VideoFragment();
                    return mVideoFragment;
                case 1:
                    mMapFragment = new MapsFragment();
                    return mMapFragment;
                case 2:
                    return new Fragment();
                default:
                    return null;
            }
        }
        @Override
        public int getCount() {
            return NUM_OF_TAB+1;
        }
    }

    private final SkyControllerDrone.Listener mSkyControllerListener = new SkyControllerDrone.Listener() {
        @Override
        public void onSkyControllerConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
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
