package ch.epfl.droneproject.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import ch.epfl.droneproject.R;


public class MapsFragment extends Fragment implements OnMapReadyCallback{

    private View mView;

    private GoogleMap mMap;

    private static final boolean DRAW_PATH = true;
    private static final boolean DRAW_FIXES = true;

    private PolylineOptions fixList;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_maps, container, false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fixList = new PolylineOptions();

        Button cleanBtn = mView.findViewById(R.id.clean_button);
        cleanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mMap.clear();
                fixList = new PolylineOptions();
            }
        });

        Button openBtn = mView.findViewById(R.id.open_button);
        openBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mMap.clear();
                fixList = new PolylineOptions();
            }
        });

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
                fixList.add(point);
                drawFlightPlan();
            }
        });

    }

    private void drawFlightPlan(){

        mMap.clear();

        if(DRAW_PATH) {
            mMap.addPolyline(fixList);
        }

        if(DRAW_FIXES) {
            List<LatLng> fL = fixList.getPoints();

            for (int i = 0; i < fL.size(); i++) {
                mMap.addMarker(new MarkerOptions().position(fL.get(i)));
            }
        }
    }

}