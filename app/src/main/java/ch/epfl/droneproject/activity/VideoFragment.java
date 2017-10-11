package ch.epfl.droneproject.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;

import ch.epfl.droneproject.R;
import ch.epfl.droneproject.view.BebopVideoView;
import ch.epfl.droneproject.view.ConsoleView;


public class VideoFragment extends Fragment {

    private BebopVideoView mVideoView;

    private TextView mDroneBatteryLabel;
    private TextView mSkyController2BatteryLabel;
    private TextView mDroneConnectionLabel;
    private ConsoleView mConsole;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView;

        mView = inflater.inflate(R.layout.fragment_video, container, false);

        mVideoView = mView.findViewById(R.id.videoView);

        mSkyController2BatteryLabel =  mView.findViewById(R.id.skyBatteryLabel);
        mDroneBatteryLabel = mView.findViewById(R.id.droneBatteryLabel);

        mDroneConnectionLabel = mView.findViewById(R.id.droneConnectionLabel);

        mConsole = mView.findViewById(R.id.console);

        for (int i = 0; i< 100; i++){
            mConsole.push("Message number "+i);
        }

        return mView;
    }

    public void makeConnectionLabelVisible(boolean visible) {
        if (visible) {
            mDroneConnectionLabel.setVisibility(View.VISIBLE);
        }else{
            mDroneConnectionLabel.setVisibility(View.GONE);
        }
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