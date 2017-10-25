package ch.epfl.droneproject.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.epfl.droneproject.R;
import ch.epfl.droneproject.discovery.DroneDiscoverer;


public class DeviceListActivity extends AppCompatActivity{
    public static final String EXTRA_DEVICE_SERVICE = "EXTRA_DEVICE_SERVICE";

    private static final String TAG = "DeviceListActivity";

    /** List of runtime permission we need. */
    private static final String[] PERMISSIONS_NEEDED = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    /** Code for permission request result handling. */
    private static final int REQUEST_CODE_PERMISSIONS_REQUEST = 1;

    /** DroneDiscoverer object*/
    public DroneDiscoverer mDroneDiscoverer;

    /** List of discovered drones*/
    private final List<ARDiscoveryDeviceService> mDronesList = new ArrayList<>();

    // this block loads the native libraries
    // it is mandatory
    // See in Drone Application for the call
    //static {
    //    ARSDK.loadSDKLibs();
    //}

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Call parent and set content layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        // Check for permission
        Set<String> permissionsToRequest = new HashSet<>();
        for (String permission : PERMISSIONS_NEEDED) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    Toast.makeText(this, "Please allow permission " + permission, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                } else {
                    permissionsToRequest.add(permission);
                }
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                    REQUEST_CODE_PERMISSIONS_REQUEST);
        }


        // Init the drone discoverer
        mDroneDiscoverer = new DroneDiscoverer(this);

        // Assign the onRefresh listener of the swipe refresh layout
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                // Do something if needed
                 swipeLayout.setRefreshing(false);
            }
        });

        // Assign adapter to ListView and assign an "onItemClick" listener
        final ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {

                // launch the activity related to the type of discovery device service
                Intent intent = null;

                ARDiscoveryDeviceService service = (ARDiscoveryDeviceService)mAdapter.getItem(position);
                ARDISCOVERY_PRODUCT_ENUM product = ARDiscoveryService.getProductFromProductID(service.getProductID());
                switch (product) {
                    case ARDISCOVERY_PRODUCT_SKYCONTROLLER_2:
                        intent = new Intent(DeviceListActivity.this, SkyControllerActivity.class);
                        break;
                    case ARDISCOVERY_PRODUCT_BEBOP_2:
                        intent = new Intent(DeviceListActivity.this, BebopActivity.class);
                    default:
                        Toast.makeText(view.getContext(), "The type " + product + " is not supported by this sample", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "The type " + product + " is not supported by this sample");
                }

                if (intent != null) {
                    intent.putExtra(EXTRA_DEVICE_SERVICE, service);
                    startActivity(intent);
                }
            }
        });

    }

    @Override
    protected void onResume()
    {
        // Call parent
        super.onResume();

        // setup the drone discoverer and register as listener
        mDroneDiscoverer.setup();
        mDroneDiscoverer.addListener(mDiscovererListener);

        // start discovering
        mDroneDiscoverer.startDiscovering();
    }

    @Override
    protected void onPause()
    {
        // Call parent
        super.onPause();

        // clean the drone discoverer object
        mDroneDiscoverer.stopDiscovering();
        mDroneDiscoverer.cleanup();
        mDroneDiscoverer.removeListener(mDiscovererListener);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean denied = false;
        if (permissions.length == 0) {
            // canceled, finish
            denied = true;
        } else {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    denied = true;
                }
            }
        }

        if (denied) {
            Toast.makeText(this, "At least one permission is missing.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // Do nothing ! This override is mandatory to avoid back on title
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // NO MENU FOR NOW
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu., menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // NO MENU ITEM FOR NOW
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Drone discover listener which override onDroneListUpdate*/
    private final DroneDiscoverer.Listener mDiscovererListener = new  DroneDiscoverer.Listener() {

        @Override
        public void onDronesListUpdated(List<ARDiscoveryDeviceService> dronesList) {
            mDronesList.clear();
            mDronesList.addAll(dronesList);

            mAdapter.notifyDataSetChanged();
        }
    };


    /** NEED TO UNDERSTAND USE OF THIS CLASS*/
    private static class ViewHolder {
        TextView text;
    }

    /** ListView personalized adapter */
    private final BaseAdapter mAdapter = new BaseAdapter()
    {
        @Override
        public int getCount()
        {
            return mDronesList.size();
        }

        @Override
        public Object getItem(int position)
        {
            return mDronesList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                LayoutInflater inflater = getLayoutInflater();
                rowView = inflater.inflate(android.R.layout.simple_list_item_1, null);
                // configure view holder
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.text = rowView.findViewById(android.R.id.text1);
                rowView.setTag(viewHolder);
            }

            // fill data
            ViewHolder holder = (ViewHolder) rowView.getTag();
            ARDiscoveryDeviceService service = (ARDiscoveryDeviceService)getItem(position);

            // Can use the product here to setText better
            //ARDISCOVERY_PRODUCT_ENUM product = ARDiscoveryService.getProductFromProductID(service.getProductID());

            holder.text.setText(service.getName() + " on " + service.getNetworkType());

            return rowView;
        }
    };

}
