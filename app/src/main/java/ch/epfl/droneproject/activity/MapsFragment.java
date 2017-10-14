package ch.epfl.droneproject.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import ch.epfl.droneproject.R;
import ch.epfl.droneproject.module.FlightPlanerModule;

/**
 *
 */
public class MapsFragment extends Fragment implements OnMapReadyCallback {

    /**
     * The view where the fragment is part of
     */
    private View mView;

    /**
     * Boolean that indicate if the markers detail's panel is displayed or not
     */
    private boolean mIsDetailsDisplay;

    /**
     * The googleMap object allowing us using google map api for android
     */
    private GoogleMap mMap;

    /**
     * Constant that indicate if we draw the path between the fixes of the flight plan
     */
    private static final boolean DRAW_PATH = true;

    /**
     * Constant that indicate if we draw the fixes marker: Its value must be "true"
     */
    private static final boolean DRAW_FIXES = true;

    /**
     * Constant that indicate if we draw the Drone marker: Its value must be "true"
     */
    private static final boolean DRAW_DRONE = true;


    /**
     * The flight planner module instance (from SkyControllerDrone->SkyControllerExtensionModule->FlightPlannerModule)
     * Used to link display with flight data.
     */
    private FlightPlanerModule mFPLM;


    /**
     * Initialize the Fragment as a constructor should.
     * This method MUST be called in chain with a new instantiation
     * i.e : new MapsFragment().init(fplm);
     * This method is called before createView()
     * @param mFPLM (FlightPlanerModule): the flight plan module used for link UI with data
     */
    public MapsFragment init(FlightPlanerModule mFPLM) {

        mIsDetailsDisplay = false;

        this.mFPLM = mFPLM;
        mFPLM.cleanFix();

        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_maps, container, false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final ListView listview = mView.findViewById(R.id.flightplanList);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                mMap.clear();
                mFPLM.cleanFix();
                final String item = (String) parent.getItemAtPosition(position);
                mFPLM.getMavlink().openMavlinkFile(item);
                hideMavlinkSelection();
                drawFlightPlan();
                ((TextView)mView.findViewById(R.id.flightplan_text)).setText(item);
                if(mFPLM.getFlightPlanSize() > 0) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mFPLM.getPosition(0), 16));
                }
            }
        });

        Button cleanBtn = mView.findViewById(R.id.clean_button);
        cleanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mMap.clear();
                mFPLM.cleanFix();
                hideMarkerDetails();
                drawDrone();
            }
        });

        Button openBtn = mView.findViewById(R.id.open_button);
        openBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideMarkerDetails();
                final ArrayAdapter<String> adapter = new ArrayAdapter<>(mView.getContext(), R.layout.flightplan_item, mFPLM.getMavlink().getMavlinkFiles());
                listview.setAdapter(adapter);
                showMavlinkSelection();
            }
        });

        Button saveBtn = mView.findViewById(R.id.save_button);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideMarkerDetails();
                String filename = mFPLM.getMavlink().generateMavlinkFile();
                if(filename == null){
                    showToast(getResources().getString(R.string.errorSaving));
                }else{
                    showToast(getResources().getString(R.string.confirmSaving)+filename);
                    ((TextView)mView.findViewById(R.id.flightplan_text)).setText(filename);
                }
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
                mFPLM.removeFix(Integer.parseInt(((TextView) mView.findViewById(R.id.id_text)).getText().toString()));
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
                mFPLM.addDefaultFix(point.latitude, point.longitude);
                drawFlightPlan();
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                updateMarkerDetails((int)marker.getTag());
                showMarkerDetails();
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
                mFPLM.setFixPosition((int)marker.getTag(), marker.getPosition().latitude, marker.getPosition().longitude);
                drawFlightPlan();
            }
        });

        //drawFlightPlan();
        drawDrone();
    }

    /**
     * Draw on the map the flight plan.
     * it follows the DRAW_FIXES and DRAW_PATH flags
     * draw the drone icon if drone location available
     */
    private void drawFlightPlan(){

        // First clear the map, before drawing on it
        mMap.clear();

        // Get the Fixes of the flight plan on the MarkerOption form
        ArrayList<MarkerOptions> fixesMarkerOptions = mFPLM.getFixesMarkerOptions();

        // Draw the path if asked using a Polyline
        if(DRAW_PATH) {
            PolylineOptions betweenFixLines = new PolylineOptions();
            for (int i = 0; i < fixesMarkerOptions.size(); i++) {
                betweenFixLines.add(fixesMarkerOptions.get(i).getPosition());
            }
            mMap.addPolyline(betweenFixLines);
        }

        // Draw the fixes if asked using Markers
        if(DRAW_FIXES) {
            for (int i = 0; i < fixesMarkerOptions.size(); i++) {
                Marker m = mMap.addMarker(fixesMarkerOptions.get(i));
                m.setTag(i);
            }
        }

        drawDrone();

    }

    private void drawDrone(){
        if(DRAW_DRONE){
            Marker m = mMap.addMarker(mFPLM.getCurrentDronePosition());
            m.setTag(-1);
        }
    }

    /**
     * Hide the Marker Details Panel with its current data
     */
    private void hideMarkerDetails(){
        mView.findViewById(R.id.markerPanel).setVisibility(View.GONE);
        mIsDetailsDisplay = false;
    }

    /**
     * Show the Marker Details Panel with its current data !
     * Be careful to set new data before showing the panel !
     */
    private void showMarkerDetails(){
        mView.findViewById(R.id.markerPanel).setVisibility(View.VISIBLE);
        mIsDetailsDisplay = true;
    }

    /**
     * Hide the Mavlink file selector Panel with its current data
     */
    private void hideMavlinkSelection(){
        mView.findViewById(R.id.listPanel).setVisibility(View.GONE);
    }

    /**
     * Show the Mavlink file selector Panel with its current data
     */
    private void showMavlinkSelection(){
        mView.findViewById(R.id.listPanel).setVisibility(View.VISIBLE);
    }

    private void showToast(String text){
        Toast.makeText(mView.getContext(), text, Toast.LENGTH_LONG).show();
    }


    /**
     * Update the content of the Maker Detail panel with the idTag selected marker
     * @param idTag (int): selected marker
     */
    private void updateMarkerDetails(int idTag){

        // Simply get into the flight planer module to obtain needed values.
        ((TextView) mView.findViewById(R.id.id_text)).setText(String.valueOf(idTag));
        ((EditText) mView.findViewById(R.id.title_text)).setText(mFPLM.getTitle(idTag));
        LatLng latLng = mFPLM.getPosition(idTag);
        ((EditText) mView.findViewById(R.id.lat_text)).setText(String.valueOf(latLng.latitude));
        ((EditText) mView.findViewById(R.id.lon_text)).setText(String.valueOf(latLng.longitude));
        ((EditText) mView.findViewById(R.id.alt_text)).setText(String.valueOf(mFPLM.getAlt(idTag)));
        ((EditText) mView.findViewById(R.id.yaw_text)).setText(String.valueOf(mFPLM.getYaw(idTag)));
    }


    /**
     * When you press DONE after a modification into the Marker details panel, the whole UI must be
     * adapted to this modification.
     * i.e. The parker position, or the marker details fields.
     */
    private final TextView.OnEditorActionListener onKeyDoneListener = new TextView.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

            if(textView.getText().length() > 0){

                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.KEYCODE_BACK) {
                    mFPLM.setFix(Integer.parseInt(((TextView) mView.findViewById(R.id.id_text)).getText().toString()),
                            ((EditText) mView.findViewById(R.id.title_text)).getText().toString(),
                            Double.parseDouble(((EditText) mView.findViewById(R.id.lat_text)).getText().toString()),
                            Double.parseDouble(((EditText) mView.findViewById(R.id.lon_text)).getText().toString()),
                            Double.parseDouble(((EditText) mView.findViewById(R.id.alt_text)).getText().toString()),
                            Double.parseDouble(((EditText) mView.findViewById(R.id.yaw_text)).getText().toString())
                            );
                    drawFlightPlan();
                }
                return false;
            }
            return true;
        }
    };

}