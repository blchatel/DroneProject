package ch.epfl.droneproject.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;

import ch.epfl.droneproject.R;
import ch.epfl.droneproject.view.BebopVideoView;
import ch.epfl.droneproject.view.CVClassifierView;
import ch.epfl.droneproject.view.ConsoleView;


public class VideoFragment extends Fragment {

    /**
     * The view where the fragment is part of
     */
    private View mView;

    private BebopVideoView mVideoView;
    private CVClassifierView mCVCView;
    //private ImageView mImageView;

    private TextView mDroneBatteryLabel;
    private TextView mSkyController2BatteryLabel;
    private TextView mDroneConnectionLabel;
    private static ConsoleView mConsole;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_video, container, false);

        mVideoView = mView.findViewById(R.id.videoView);
        mVideoView.setSurfaceTextureListener(mVideoView);
        mCVCView = mView.findViewById(R.id.cvcView);
        //mImageView = (ImageView) mView.findViewById(R.id.videoView);

        mSkyController2BatteryLabel =  mView.findViewById(R.id.skyBatteryLabel);
        mDroneBatteryLabel = mView.findViewById(R.id.droneBatteryLabel);

        mDroneConnectionLabel = mView.findViewById(R.id.droneConnectionLabel);

        mConsole = mView.findViewById(R.id.console);

        return mView;
    }

    @Override
    public void onResume(){
        super.onResume();
        mCVCView.resume(mVideoView);
    }

    @Override
    public void onPause(){
        super.onPause();
        mCVCView.pause();
    }


    public void makeConnectionLabelVisible(boolean visible) {
        if (visible) {
            mDroneConnectionLabel.setVisibility(View.VISIBLE);
        }else{
            mDroneConnectionLabel.setVisibility(View.GONE);
        }
    }

    public static void pushInConsole(String text){
        if(mConsole != null)
            mConsole.push(text);
    }


    public void setControllerBatteryLabel(String label){
        mSkyController2BatteryLabel.setText(label);
    }
    public void setDroneBatteryLabel(String label){
        mDroneBatteryLabel.setText(label);
    }

    public void configureDecoder(ARControllerCodec codec){
        mVideoView.configureDecoder(codec);
    }

    public void displayFrame(ARFrame frame){
        mVideoView.displayFrame(frame);
    }
}