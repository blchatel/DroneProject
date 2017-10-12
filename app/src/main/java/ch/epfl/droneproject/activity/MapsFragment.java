package ch.epfl.droneproject.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ch.epfl.droneproject.R;


public class MapsFragment extends Fragment implements OnMapReadyCallback {

    /**
     * Interface making available MapsFragment listening
     */
    private interface Listener{
        void onOpenClick();
    }
    /**
     * All the listeners of an MapsFragment instance
     */
    List<Listener> mListeners;
    /**
     * Add a new listener to the list
     * @param listener
     */
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }
    /**
     * Remove a listener of the list
     * @param listener
     */
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }


    /**
     * The view where the fragment is part of
     */
    private View mView;

    /**
     * Boolean that indicate if the markers detail's panel is displayed or not
     */
    private boolean mIsDetailsDisplay;
    private GoogleMap mMap;

    /**
     * Constant that indicate if we draw the path between the fixes of the flight plan
     */
    private static final boolean DRAW_PATH = true;

    /**
     * Constant that indicate if we draw the fixes marker
     */
    private static final boolean DRAW_FIXES = true;


    private ArrayList<Fix> fixList;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_maps, container, false);
        mListeners = new ArrayList<>();
        mIsDetailsDisplay = false;
        fixList = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        Button cleanBtn = mView.findViewById(R.id.clean_button);
        cleanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mMap.clear();
                fixList = new ArrayList<>();
                hideMarkerDetails();
            }
        });

        Button openBtn = mView.findViewById(R.id.open_button);
        openBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mMap.clear();
                fixList = new ArrayList<>();
                hideMarkerDetails();
            }
        });

        Button closeBtn = mView.findViewById(R.id.close_marker_button);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideMarkerDetails();
            }
        });

        Button removeBtn = mView.findViewById(R.id.delete_marker_button);
        removeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                fixList.remove(Integer.parseInt(((TextView) mView.findViewById(R.id.id_text)).getText().toString()));
                drawFlightPlan();
                hideMarkerDetails();
            }
        });

        ((EditText)mView.findViewById(R.id.title_text)).setOnEditorActionListener(onKeyDoneListener);
        ((EditText)mView.findViewById(R.id.lat_text)).setOnEditorActionListener(onKeyDoneListener);
        ((EditText)mView.findViewById(R.id.lon_text)).setOnEditorActionListener(onKeyDoneListener);
        ((EditText)mView.findViewById(R.id.alt_text)).setOnEditorActionListener(onKeyDoneListener);
        ((EditText)mView.findViewById(R.id.yaw_text)).setOnEditorActionListener(onKeyDoneListener);

        return mView;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ActivityCompat.checkSelfPermission(mView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(mView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                hideMarkerDetails();
                //fixList.add(new MarkerOptions().position(point).draggable(true));
                fixList.add(new Fix(Fix.DEFAULT_TITLE+fixList.size(), point.latitude, point.longitude, Fix.DEFAULT_ALTITUDE, Fix.DEFAULT_YAW));
                drawFlightPlan();
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                showMarkerDetails();
                updateMarkerDetails((int)marker.getTag());
                return false;
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                if(mIsDetailsDisplay){
                    updateMarkerDetails((int)marker.getTag());
                }
            }
            @Override
            public void onMarkerDrag(Marker marker) {

                if(mIsDetailsDisplay){
                    ((EditText) mView.findViewById(R.id.lat_text)).setText(String.valueOf(marker.getPosition().latitude));
                    ((EditText) mView.findViewById(R.id.lon_text)).setText(String.valueOf(marker.getPosition().longitude));
                }
            }
            @Override
            public void onMarkerDragEnd(Marker marker) {
                //fixList.set((int)marker.getTag(), new MarkerOptions().position(marker.getPosition()).draggable(true));
                fixList.set((int)marker.getTag(), new Fix(marker.getTitle(), marker.getPosition().latitude, marker.getPosition().longitude, Fix.DEFAULT_ALTITUDE, Fix.DEFAULT_YAW));
                drawFlightPlan();
            }
        });
    }

    private void drawFlightPlan(){

        mMap.clear();

        if(DRAW_PATH) {
            PolylineOptions betweenFixLines = new PolylineOptions();
            for (int i = 0; i < fixList.size(); i++) {
                betweenFixLines.add(fixList.get(i).getPosition());
            }
            mMap.addPolyline(betweenFixLines);
        }

        if(DRAW_FIXES) {
            for (int i = 0; i < fixList.size(); i++) {
                Fix fix = fixList.get(i);
                Marker m = mMap.addMarker(new MarkerOptions().position(fix.getPosition()).draggable(true).title(fix.getTitle()));
                m.setTag(i);
            }
        }
    }

    private void hideMarkerDetails(){
        mView.findViewById(R.id.markerPanel).setVisibility(View.GONE);
        mIsDetailsDisplay = false;
    }
    private void showMarkerDetails(){
        mView.findViewById(R.id.markerPanel).setVisibility(View.VISIBLE);
        mIsDetailsDisplay = true;
    }

    private void updateMarkerDetails(int idTag){

        Fix fix = fixList.get(idTag);

        ((TextView) mView.findViewById(R.id.id_text)).setText(String.valueOf(idTag));
        ((EditText) mView.findViewById(R.id.title_text)).setText(fix.getTitle());
        ((EditText) mView.findViewById(R.id.lat_text)).setText(String.valueOf(fix.getPosition().latitude));
        ((EditText) mView.findViewById(R.id.lon_text)).setText(String.valueOf(fix.getPosition().longitude));
        ((EditText) mView.findViewById(R.id.alt_text)).setText(String.valueOf(fix.getAlt()));
        ((EditText) mView.findViewById(R.id.yaw_text)).setText(String.valueOf(fix.getYaw()));
    }




    private final TextView.OnEditorActionListener onKeyDoneListener = new TextView.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

            Log.e("KEY", "KEY1");

            if(textView.getText().length() > 0){

                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.KEYCODE_BACK) {
                    Log.e("KEY", "KEY2");

                    Fix fix = new Fix(
                            ((EditText) mView.findViewById(R.id.title_text)).getText().toString(),
                            Double.parseDouble(((EditText) mView.findViewById(R.id.lat_text)).getText().toString()),
                            Double.parseDouble(((EditText) mView.findViewById(R.id.lon_text)).getText().toString()),
                            Double.parseDouble(((EditText) mView.findViewById(R.id.alt_text)).getText().toString()),
                            Double.parseDouble(((EditText) mView.findViewById(R.id.yaw_text)).getText().toString())
                    );
                    Log.e("KEY", ((TextView) mView.findViewById(R.id.id_text)).getText().toString());

                    fixList.set(Integer.parseInt(((TextView) mView.findViewById(R.id.id_text)).getText().toString()), fix);
                    drawFlightPlan();
                }
                return false;
            }
            return true;
        }
    };

    private class Fix{

        static final String DEFAULT_TITLE = "FIX";
        static final double DEFAULT_ALTITUDE = 5;
        static final double DEFAULT_YAW = 0;

        String title;
        double lat;
        double lon;
        double alt;
        double yaw;

        boolean isTakeOff;
        boolean isLanding;


        Fix(String title, double lat, double lon, double alt, double yaw) {
            this.title = title;

            this.lat = lat%180;
            if(this.lat > 90){
                this.lat = -180+this.lat;
            }
            this.lon = lon%360;
            if(this.lon > 180){
                this.lon = -360+this.lon;
            }
            this.alt = alt;
            this.yaw = yaw%360;

            isTakeOff = false;
            isLanding = false;
        }

        LatLng getPosition(){
            return new LatLng(lat, lon);
        }

        String getTitle() {
            return title;
        }

        double getAlt() {
            return alt;
        }

        double getYaw() {
            return yaw;
        }

        public String toString(){
            return title + ":  lat:"+lat + ", lon:"+lon+", alt:"+alt+", yaw:"+yaw;
        }

    }
}