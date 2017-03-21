package iter.bluetoothconnect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import iter.bluetoothconnect.kml.KmlLayer;
import iter.bluetoothconnect.kml.KmlPlacemark;
import iter.bluetoothconnect.kml.KmlPolygon;

public class MainActivityMap extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private MapView mapView;
    private GoogleMap googleMap;
    TextView status;
    TextView point;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;

    public static final String PARAMETER_KML_NAME="kml_file_name";
    private String PARAMETER_KML_NAME_VALUE="";

    private Boolean loadMap;
    private TypeGoogleMaps mtypeMap;
    private HashMap<String,Object> listMaps;
    private String currentMap;

    private List<KmlPlacemark> listPoints = new ArrayList<>();

    private static final int RECOVER_MAP = 0;

    public String getCurrentMap(){
        return currentMap;
    }
    public void setCurrentMap(String mapName){
        currentMap = mapName;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_map);

        status = (TextView) findViewById(R.id.status_text);
        point = (TextView) findViewById(R.id.point_text);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        setVariables();
        setUpMap();
    }
    private void setVariables(){
        loadMap = true; //That change when new map is selected or it is first load
        mtypeMap = TypeGoogleMaps.MAP_TYPE_NORMAL;

        try {
            PARAMETER_KML_NAME_VALUE= getIntent().getStringExtra(PARAMETER_KML_NAME);

            if (PARAMETER_KML_NAME_VALUE == "" || PARAMETER_KML_NAME_VALUE == null) {
                Toast.makeText(this,"Warning: No se ha podido cargar el mapa de inicio ", Toast.LENGTH_LONG).show();
            }else {
                currentMap = PARAMETER_KML_NAME_VALUE;
                Toast.makeText(this,"Se ha cargado: " + PARAMETER_KML_NAME_VALUE, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }




        //listMaps = new HashMap<>();
        //listMaps.put("Esta")
    }
    private void setUpMap() {
        //Configure GoogleMap optiopns
    }

    @Override
    public void onMapReady(GoogleMap mGoogleMap) {
        // Customize map with markers, polylines, etc.
        googleMap = mGoogleMap;
        googleMap.setMapType(mtypeMap.getValue());

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                googleMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            googleMap.setMyLocationEnabled(true);
        }

    }
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        status.setText("Conectado");
    }
    @Override
    public void onConnectionSuspended(int i) {
        status.setText("Suspendido");
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        if(loadMap && currentMap.length()>0) {
            googleMap.clear();
            retrieveFileFromResource();
        }

        //Load points from the list
        /*for(KmlPlacemark placemark : listPoints) {
            KmlPolygon polygon = (KmlPolygon) placemark.getGeometry();

            //Miramos si solo tiene un punto, es decir, que no es un polígono
            if(polygon.getOuterBoundaryCoordinates().toArray().length == 1){
                LatLng latLng = polygon.getOuterBoundaryCoordinates().get(0);

                listPoints.add(placemark);
                    googleMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            .title(placemark.getProperty("name"))
                            .snippet("Style : " + placemark.getProperty("styleUrl")));
            }


        }
        status.setText("Puntos:" + listPoints.size());*/

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("My Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        mCurrLocationMarker = googleMap.addMarker(markerOptions);


        //move map camera
        if (loadMap) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(9));
        }

        //Show my gps point
        StringBuilder builder =  new StringBuilder();
        builder.append("Mypoint : <Lat:")
                .append(location.getLatitude())
                .append(",Lon:")
                .append(location.getLongitude())
                .append(",Alt:")
                .append(location.getAltitude())
                .append(", Prec:")
                .append(location.getAccuracy())
                .append(">");
        point.setText(builder.toString());

        //stop location updates
        /*if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }*/

        loadMap=false;
    }

    private void retrieveFileFromResource() {
        try {
            int drawableResourceId = this.getResources().getIdentifier(currentMap, "raw", this.getPackageName());

            KmlLayer kmlLayer = new KmlLayer(googleMap,drawableResourceId, getApplicationContext());

            //Original:
            //kmlLayer.addLayerToMap();
            //moveCameraToKml(kmlLayer);
            listPoints.clear();

            for(KmlPlacemark placemark : kmlLayer.getPlacemarks()){

                KmlPolygon polygon = (KmlPolygon) placemark.getGeometry();

                //Miramos si solo tiene un punto, es decir, que no es un polígono
                if(polygon.getOuterBoundaryCoordinates().toArray().length == 1){
                    LatLng latLng = polygon.getOuterBoundaryCoordinates().get(0);

                    //listPoints.add(placemark);
                    googleMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            .title(placemark.getProperty("name"))
                            .snippet("Style : " + placemark.getProperty("styleUrl")));


                }
            }

            kmlLayer.addLayerToMap();


        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        status.setText("Sin conexión");
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        googleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * On selecting action bar icons
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Take appropriate action for each action item click
        switch (item.getItemId()) {
            /*case R.id.action_refresh:
                refreshMap();
                return true;*/
            case R.id.action_map:
                openRoutesActivity();
                return true;
            case R.id.action_setting:
                // check for updates action
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void openRoutesActivity(){
        Intent listMaps = new Intent(MainActivityMap.this,CampaignActivity.class);
        startActivityForResult(listMaps,RECOVER_MAP);
    }
    private void refreshMap(){
        mapView.invalidate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == RECOVER_MAP) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                currentMap = data.getStringExtra("currentMap");
                loadMap = true;
            }
        }
    }

    private enum TypeGoogleMaps{
        MAP_TYPE_NONE(0),
        MAP_TYPE_NORMAL(1),
        MAP_TYPE_SATELLITE(2),
        MAP_TYPE_TERRAIN(3),
        MAP_TYPE_HYBRID(4);

        int value;
        TypeGoogleMaps(int value){this.value = value;}
        public int getValue(){return this.value;}
    }

}
