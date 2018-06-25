package iter.bluetoothconnect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,SensorEventListener {

    private static final int MAP_REQUEST = 2;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private Boolean mRequestingLocationUpdates;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private ArrayList<Point> points;

    private SensorManager sm;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] mGravity;
    private float[] mGeomagnetic;
    private float azimuth = 0;

    private CardView infoPanel;
    private TextView panelName, panelDistance;
    private ImageButton actionButton;

    private MyPosition myPos;
    private Circle circle;

    private SharedPreferences sharedPref;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private ListView listViewPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        infoPanel = (CardView) findViewById(R.id.cardView);
        infoPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng latLng = (LatLng)infoPanel.getTag();
                goToPosition(latLng);
            }
        });

        String username = getIntent().getStringExtra(getString(R.string.extra_key_username));

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setSubtitle(username);
        mRequestingLocationUpdates = false;
        toggle = new ActionBarDrawerToggle(this,drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        listViewPoints = (ListView)findViewById(R.id.list_drawer);
        listViewPoints.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerLayout.closeDrawer(Gravity.LEFT);
                Point p = points.get(position);
                goToPosition(p.getLatlng());
                p.marker.showInfoWindow();
            }
        });
        panelName = (TextView) findViewById(R.id.tvNamePoint);
        panelDistance = (TextView) findViewById(R.id.tvDistancePoint);
        actionButton = (ImageButton) findViewById(R.id.btLaunch);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.clearAnimation();
                Intent i = new Intent(MapsActivity.this, DataActivity.class);
                i.putExtra(getString(R.string.extra_campaing_name), getSupportActionBar().getTitle());
                i.putExtra(getString(R.string.extra_key_username), getSupportActionBar().getSubtitle());
                String currentBTMac = getIntent().getStringExtra(getString(R.string.extra_device_address));
                if (currentBTMac == null)
                    currentBTMac = "";
                i.putExtra(getString(R.string.extra_device_address), currentBTMac);
                if (mCurrentLocation != null) {
                    i.putExtra(getString(R.string.extra_lat), mCurrentLocation.getLatitude());
                    i.putExtra(getString(R.string.extra_long), mCurrentLocation.getLongitude());
                    i.putExtra(getString(R.string.extra_accuracy), mCurrentLocation.getAccuracy());
                }
                i.putExtra(getString(R.string.extra_point_name), panelName.getText());
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(i, MAP_REQUEST);
            }
        });

        sharedPref = getSharedPreferences(getString(R.string.key_configs), Context.MODE_PRIVATE);

        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);
        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (mMap != null){
                    if (mCurrentLocation == null){
                        mCurrentLocation = locationResult.getLastLocation();
                        updateMyPos(mCurrentLocation);
                    }else{
                        if ((mCurrentLocation.getLatitude() == locationResult.getLastLocation().getLatitude()) &&
                                (mCurrentLocation.getLongitude() == locationResult.getLastLocation().getLongitude()) &&
                                (mCurrentLocation.getAltitude() == locationResult.getLastLocation().getAltitude())
                                ){
                            return;
                        }else{
                            mCurrentLocation = locationResult.getLastLocation();
                            updateMyPos(mCurrentLocation);
                            updateDistances();
                            updateListView();
                        }
                    }
                }
            }
        };

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
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

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        int mapType = sharedPref.getInt(getString(R.string.key_maptype), 3);
        mMap.setMapType(mapType);

        //load points from shared preferences
        String strJson = sharedPref.getString(getString(R.string.campaing_list),"");
        int currentCamp = getIntent().getIntExtra(getString(R.string.extra_key_position_item),0);
        if ((strJson != null) && (strJson != "") && (currentCamp > 0)) {
            try {
                JSONArray jsonArray = new JSONArray(strJson);
                loadPoints((JSONObject) jsonArray.get(currentCamp - 1));
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        startLocationUpdate();
    }

    @Override
    protected void onStop() {
        if (sm != null)
            sm.unregisterListener(this);
        stopLocationUpdates();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void startLocationUpdate(){
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest).addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                getLocationPermission();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (sm != null){
            sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sm.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.change_map:
                int mapType = mMap.getMapType();
                mapType = ((mapType + 1) % 3) + 1;

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(getString(R.string.key_maptype), mapType);
                editor.apply();
                mMap.setMapType(mapType);
                return true;
            case R.id.myLocation:
                if (mCurrentLocation != null)
                    goToPosition(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == MAP_REQUEST){
            if (resultCode == RESULT_OK){
                String pointName = data.getStringExtra(getString(R.string.extra_point_name));
                if (pointName != null){
                    for (Point p : points){
                        if (p.getName().contentEquals(pointName)){
                            p.setStatus( ""+System.currentTimeMillis());
                            updatePanelInfo(p);
                            break;
                        }
                    }
                }
                updateListView();
                //TODO update point status in server
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        //mLocationPermissionGranted = false;
        mRequestingLocationUpdates = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // mLocationPermissionGranted = true;
                    mRequestingLocationUpdates = true;
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                }
            }
        }
    }

    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            //  Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }
        if ((mFusedLocationClient) != null && (mLocationCallback != null))
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mRequestingLocationUpdates = false;
                        }
                    });
    }

    /**
     * Update marker myPos in map when a new location is available
     * @param location new Location
     */
    private void updateMyPos(Location location){
        mCurrentLocation = location;

        LatLng pos = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        if (myPos == null){
            //Compose marker
            Marker currentPos = mMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(pos).title("Altitude: "+ Math.round(location.getAltitude())+ " m").icon(BitmapDescriptorFactory.fromResource(R.drawable.uparrow)));
            //compose circle
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(pos);
            circleOptions.radius(location.getAccuracy());
            circleOptions.fillColor(0xAA00FFFF);
            circleOptions.strokeColor(Color.GRAY);
            circleOptions.strokeWidth(2.0f);
            Circle circle = mMap.addCircle(circleOptions);
            myPos = new MyPosition(currentPos, circle);
        }else
            myPos.setPosition(pos, location.getAccuracy(), location.getAltitude());
    }

    /**
     * Load points into map
     * @param geoData JSON object with coordinates and info
     */
    private void loadPoints(JSONObject geoData){
        if (geoData != null){
            try {
                String name = geoData.getString("name");
                getSupportActionBar().setTitle(name);

                JSONArray array = geoData.getJSONArray("points");
                int size =  array.length();
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                points = new ArrayList<Point>();
                BitmapDescriptor  bd = BitmapDescriptorFactory.fromResource(android.R.drawable.ic_delete);
                for (int i = 0; i < size; i++){
                    JSONObject point = (JSONObject) array.get(i);
                    String title = point.getString("name");
                    double lat = Double.parseDouble(point.getString("lat"));
                    double lon = Double.parseDouble(point.getString("lon"));
                    int alt = Integer.parseInt(point.getString("alt"));
                    LatLng currentPoint = new LatLng(lat, lon);
                    Marker marker = mMap.addMarker(new MarkerOptions().position(currentPoint).title(title).flat(true).anchor(0.5f, 0.5f).icon(bd));
                    Point p = new Point(marker, "", alt);
                    points.add(p);
                    builder.include(currentPoint);
                }
                //Log.v("map", ""+points.size());
                AdapterPoint ap = new AdapterPoint(this, android.R.layout.two_line_list_item, points);
                listViewPoints.setAdapter(ap);
                int width = getResources().getDisplayMetrics().widthPixels;
                int height = getResources().getDisplayMetrics().heightPixels;
                int padding = (int) (width * 0.05); // offset from edges of the map 5% of screen

                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, padding));
               // mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 5));
            } catch (JSONException e) {

            }
        }
    }

    /**
     * ask for permissions before starting location updates
     */
    private void getLocationPermission() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // mLocationPermissionGranted = true;
            mRequestingLocationUpdates = true;
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (myPos != null){
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values.clone();
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values.clone();
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];

                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    float currentAzimuth = (float) Math.toDegrees(orientation[0]);
                    int azimuthFinal = Math.round(currentAzimuth);
                    if (azimuthFinal < 0)
                        azimuthFinal = 360 + azimuthFinal;
                    if (azimuthFinal != azimuth) {
                        switch (getWindowManager().getDefaultDisplay().getRotation()) {
                            case Surface.ROTATION_90:
                                azimuthFinal = (azimuthFinal + 90) % 360;
                                break;
                            case Surface.ROTATION_180:
                                azimuthFinal = azimuthFinal - 180;
                                if (azimuthFinal < 0) {
                                    azimuthFinal = 360 - azimuthFinal;
                                }
                                break;
                            case Surface.ROTATION_270:
                                azimuthFinal = azimuthFinal - 90;
                                if (azimuthFinal < 0)
                                    azimuthFinal = 360 - azimuthFinal;
                                break;
                        }
                        azimuth = azimuthFinal; // orientation contains: azimuth, pitch and roll
                        myPos.setRotation(azimuth);
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Update distances from a position to each point
     *
     */
    private void updateDistances(){
        if (points != null) {
            if (!points.isEmpty()) {
                LatLng myPos = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                double minDistance = 99999999999.0;
                Point p = null;
                for (Point point : points) {
                    double distance = SphericalUtil.computeDistanceBetween(myPos, point.getLatlng());
                    point.setDistance(distance);
                    if (distance < minDistance) {
                        minDistance = distance;
                        p = point;
                    }
                }
                updatePanelInfo(p);
            }
        }
    }

    /**
     * Update information of the panel
     * @param point Point info needed
     */
    private void updatePanelInfo(Point point){
        if (point != null){
            int color = Color.GREEN;
            if (point.isDone()){
                color = Color.RED;
            }
            infoPanel.setCardBackgroundColor(color);

            panelName.setText(point.getName());
            String distanceText = "" + point.getDistance() + " m";
            if (point.getDistance() > 1000){
                double d = point.getDistance() / 1000;
                DecimalFormat df = new DecimalFormat("#.#");
                df.setRoundingMode(RoundingMode.CEILING);
                distanceText = df.format(d) + " km";
            }
            infoPanel.setTag(point.getLatlng());
            panelDistance.setText(distanceText);
            updateCircle(point.getLatlng());
            //check if pos is overlapping to point and this is not done, then button blinks
            if (circlesAreOverlapping(point.distance, Math.round(circle.getRadius()), myPos.getCircleRadius()) && (!point.isDone())){
                //Star animation
                actionButton.clearAnimation();
                final Animation flashAnimation = new AlphaAnimation(1,0);
                flashAnimation.setDuration(500);
                flashAnimation.setInterpolator(new LinearInterpolator());
                flashAnimation.setRepeatCount(3);
                flashAnimation.setRepeatMode(Animation.RESTART);
                actionButton.startAnimation(flashAnimation);
            }
        }
    }

    /**
     * Check if two circles are overlapping
     * https://stackoverflow.com/questions/33724261/geolocations-how-to-check-if-2-circles-are-overlapping
     * @param distance Between two points
     * @param radiusA Radius of A circle
     * @param radiusB Radius of A circle
     * @return true if circles are overlapping, false otherwise
     */
    private boolean circlesAreOverlapping(long distance, long radiusA, long radiusB){
        long sum = radiusA + radiusB;
        return (sum > distance);
    }

    /**
     * Go to current GPS location
     */
    private void goToPosition(LatLng latLng){
        if ((mCurrentLocation != null) && (mMap != null)){
            //LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15.0f));
        }else{
            Toast.makeText(this,"No GPS coordiates available", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * force redraw listview of points
     */
    private void updateListView(){
        AdapterPoint ap = new AdapterPoint(this, android.R.layout.two_line_list_item, points);
        listViewPoints.setAdapter(ap);
        ap.notifyDataSetChanged();
    }

    private void updateCircle(LatLng currentPoint){
        if (circle == null){
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(currentPoint);
            circleOptions.radius(5);
            circleOptions.fillColor(0x5500ff00);
            circleOptions.strokeColor(Color.YELLOW);
            circleOptions.strokeWidth(2.0f);
            circle = mMap.addCircle(circleOptions);
        }else{
            circle.setCenter(currentPoint);
        }
    }
    /*private void saveCampaingStatus(){
        String strJson = sharedPref.getString(getString(R.string.campaing_list),"");
        int currentCamp = getIntent().getIntExtra(getString(R.string.extra_key_position_item),0);
        if ((strJson != null) && (strJson != "") && (currentCamp > 0)) {
            try {
               JSONArray jsonArray = new JSONArray(strJson);
               JSONObject object = (JSONObject) jsonArray.get(currentCamp - 1);
               //TODO create and save new JSON with new status
               SharedPreferences.Editor prefEditor = sharedPref.edit();
                prefEditor.putString(getString(R.string.campaing_list), JSONARRAY.toString());
                prefEditor.apply();*
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    }*/
}

