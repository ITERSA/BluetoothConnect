package iter.bluetoothconnect;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This activity scan and show all bluetooth conections enabled
 */
public class MainActivity extends AppCompatActivity{

    private  BluetoothAdapter mBluetoothAdapter;
    private static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 1;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private List<String> list;

    private Toast globalToast;
    private ProgressBar  progressBar;

    private static final int REQUEST_ENABLE_BT = 1000;

    private EditText etFieldName;
    private Spinner spinner;
    private ListView listItems;
    private SharedPreferences sharedPref;
    private String currentBTMac;
    private AlertDialog alert;
    private Button btStart;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                String name = "-";
                if (device.getName() != null)
                    name = device.getName();
                //check repeated items
                if (!checkIfExist(name + "\n" + device.getAddress())){
                    list.add(name + "\n" + device.getAddress());
                    mNewDevicesArrayAdapter.notifyDataSetChanged();
                }
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                //progressbar
                int count = list.size();
                if (count == 0) {
                    showGlobalToast(getResources().getString(R.string.no_device));
                }
                progressBar.setVisibility(View.GONE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPref = getSharedPreferences(getString(R.string.key_configs),Context.MODE_PRIVATE);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            getSupportActionBar().setTitle("FluxMeter "+version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        globalToast = Toast.makeText(getApplicationContext(), null, Toast.LENGTH_LONG);
        spinner = (Spinner)findViewById(R.id.spinnerMap);
        currentBTMac = "";
        etFieldName = (EditText)findViewById(R.id.campo_texto);
        Button btDiscover = (Button) findViewById(R.id.button);
        btDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((listItems != null) && (listItems.getCheckedItemPosition() != AdapterView.INVALID_POSITION))
                    listItems.setItemChecked(listItems.getCheckedItemPosition(), false);
                if (mBluetoothAdapter.isEnabled()){
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                        showGlobalToast(getResources().getString(R.string.restart));
                    }
                    list.clear();
                    mNewDevicesArrayAdapter.notifyDataSetChanged();
                   askPermissions();
                }else{
                    //showGlobalToast(getResources().getString(R.string.bt_inactive));

                    // Zeus: solicitar activación de bluetooth
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                    alertDialog.setIcon(R.mipmap.ic_launcher);
                    alertDialog.setTitle(R.string.bluetooth);
                    alertDialog.setMessage(R.string.bt_inactive);
                    alertDialog.setPositiveButton(R.string.activate, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int which) {
                            //Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            //startActivity(discoverableIntent);
                            BluetoothAdapter.getDefaultAdapter().enable();
                        }
                    });
                    // on pressing cancel button
                    alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    alert = alertDialog.create();
                    if (!alert.isShowing())
                        alertDialog.show();

                }
            }
        });

        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        listItems = (ListView)findViewById(R.id.leads_list);
        //RelativeLayout emptyText = (RelativeLayout) findViewById(R.id.relLayoutEmptyText);
        Group emptyText = (Group)findViewById(R.id.text_group);
        listItems.setEmptyView(emptyText);
        list = new ArrayList<String>();

        //mNewDevicesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_activated_1, list);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(),R.layout.bt_item_activated, list);
        listItems.setAdapter(mNewDevicesArrayAdapter);

        listItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Make an intent to start next activity while taking an extra which is the MAC address.
            mBluetoothAdapter.cancelDiscovery();
            progressBar.setVisibility(View.GONE);
            if (listItems.isItemChecked(position)){
                currentBTMac= "";
                TextView tv = (TextView)view;
                String info = tv.getText().toString();
                // Get the device MAC address, which is the last 17 chars in the View
                currentBTMac= info.substring(info.length() - 17);
                //listItems.setItemChecked(position, true);
                //btStart.setBackground(R.drawable.my_button_active);
                btStart.setBackgroundResource(R.drawable.my_button_active);
                TextView tvTemporal = (TextView)findViewById(R.id.campo_texto_temporal);
                tvTemporal.setText(info);
            }
            }
        });

       mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
            btDiscover.setEnabled(false);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        boolean firstTime = sharedPref.getBoolean(getString(R.string.key_firsttime),true);
        if (firstTime){
            requestJson();  //Update spinner from internet
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putBoolean(getString(R.string.key_firsttime),false);
            prefEditor.apply();
        }else
            loadJsonFromSharedpreferences();
        btStart = (Button)findViewById(R.id.btMap);
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            if (!currentBTMac.equals("")){
                //Log.v("MAC", currentBTMac);
                int position = listItems.getCheckedItemPosition();
                if (position != ListView.INVALID_POSITION){
                    int campaingPosition = spinner.getSelectedItemPosition();
                    String userName = etFieldName.getText().toString();
                    if (userName.equals("")){
                        userName = "-";
                    }
                    if (campaingPosition > 0){// MAP ACTIVITY
                        Intent i = new Intent(MainActivity.this, MapsActivity.class);
                        i.putExtra(getString(R.string.extra_device_address), currentBTMac);
                        i.putExtra(getString(R.string.extra_key_username), userName);
                        i.putExtra(getString(R.string.extra_key_position_item), campaingPosition);
                        startActivity(i);
                    }else{
                        Intent i = new Intent(MainActivity.this, DataActivity.class);
                        i.putExtra(getString(R.string.extra_key_username), userName);
                        i.putExtra(getString(R.string.extra_device_address), currentBTMac);
                        startActivity(i);
                    }
                }
            }else
                showGlobalToast(getResources().getString(R.string.bt_select));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (list != null && mNewDevicesArrayAdapter != null){
            list.clear();
            mNewDevicesArrayAdapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
        }
        invalidateOptionsMenu();
    }

    // Zeus
   /* @Override
    protected void onStart() {
        super.onStart();
        final LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
            alertDialog.setIcon(R.mipmap.ic_launcher);
            alertDialog.setTitle(R.string.gps);
            alertDialog.setMessage(R.string.gps_inactive);
            alertDialog.setPositiveButton(R.string.activate, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    dialog.dismiss();
                    startActivity(intent);

                }
            });
            // on pressing cancel button
            alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
           alert = alertDialog.create();
            if (!alert.isShowing())
                alertDialog.show();
        }
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_ENABLE_BT) && (resultCode == RESULT_OK))
            //Toast.makeText(getApplicationContext(),R.string.bt_on,Toast.LENGTH_SHORT).show();
            showGlobalToast(getResources().getString(R.string.bt_on));
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION: {

                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //Log.i(TAG, "Permission has been denied by user");
                } else {
                    //Log.i(TAG, "Permission has been granted by user");
                    mBluetoothAdapter.startDiscovery();
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_init,menu);
       /* MenuItem item = menu.findItem(R.id.myswitch);
        item.setActionView(R.layout.switch_layout);

        SwitchCompat switchCompat = (SwitchCompat)item.getActionView().findViewById(R.id.switchForActionBar);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }else{
                        showGlobalToast(getResources().getString(R.string.bt_on);
                    }
                }else{

                }
            }
        });
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            switchCompat.setEnabled(false);
            switchCompat.setChecked(false);
        }else
            switchCompat.setChecked(mBluetoothAdapter.isEnabled());

        checkIfUploadFiles(menu.findItem(R.id.action_upload));
*/
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_upload:
                Set<String> set = sharedPref.getStringSet(getString(R.string.key_filelist), new HashSet<String>());
                if (set.size() > 0){
                    FtpFileUpload uploader = new FtpFileUpload(set, MainActivity.this);
                    uploader.execute();
                    //TODO if files uploaded, turn button disable
                }else{
                    item.setEnabled(false);
                    showGlobalToast(getResources().getString(R.string.no_files));
                }
                return true;
            case R.id.action_reload:
                requestJson();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Show global toast
     * @param text String text to show in Toast
     */
    private void showGlobalToast(String text){
        if (globalToast != null) {
            globalToast.setText(text);
            globalToast.setDuration(Toast.LENGTH_LONG);
            globalToast.show();
        }
    }

    private boolean checkIfExist(String item){
        boolean result = false;
        if (list != null){
            for (int i = 0; i < list.size(); i++){
                String currentItem = list.get(i);
                if (currentItem.isEmpty() || currentItem.equals(item)){
                    if (!currentItem.equals("-")){
                        result = true;
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * check if there are any files pending of uploading
     * @param item
     */
    private void checkIfUploadFiles(MenuItem item){

        Set<String> set = sharedPref.getStringSet(getString(R.string.key_filelist), new HashSet<String>());
        if (set.size() > 0){
            item.setEnabled(true);
            //TODO blink icon
        }
        else
            item.setEnabled(false);
    }

    /**
     * Update spinner with a array of strings
     * @param listSpinner Array of string
     */
    private void updateSpinner(ArrayList<String> listSpinner ){
        if (listSpinner == null){
            listSpinner = new ArrayList<>();
        }
        if (listSpinner.size() == 0){
            listSpinner.add("-");
        }

        //ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listSpinner);
        //spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, listSpinner);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item);

        spinner.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();
    }

    private void askPermissions(){

        int permission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            mBluetoothAdapter.startDiscovery();
            progressBar.setVisibility(View.VISIBLE);
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
            }
        }
    }

    /**
     * Request json with the curren campaing a data from the host
     */
    private void requestJson(){
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        //https://medium.com/@ssaurel/how-to-retrieve-an-unique-id-to-identify-android-devices-6f99fd5369eb
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        String name = etFieldName.getText().toString();
        if (name == "")
            name = "-";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, getString(R.string.HOST) + "?id=" + androidId + "&name=" + name,null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                SharedPreferences.Editor prefEditor = sharedPref.edit();
                prefEditor.putString(getString(R.string.campaing_list), response.toString());
                prefEditor.apply();
                ArrayList nameList = loadJson(response);
                updateSpinner(nameList);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String currentError ="Error";
                if (error.getMessage() != null)
                    currentError = error.getMessage();
                loadJsonFromSharedpreferences();
                Log.e("Response", currentError);
            }
        });
        requestQueue.add(jsonArrayRequest);
    }

    /*
    * Get the campaing names of a json
    * */
    private ArrayList<String> loadJson(JSONArray jsonArray){
        ArrayList<String> nameList = new ArrayList<>();
        //String single = getResources().getString(R.string.single); // Zeus
        nameList.add("  -  ");
        try {
            int size = jsonArray.length();
            for (int i= 0; i < size; i++ ){
                JSONObject object = (JSONObject) jsonArray.get(i);
                nameList.add(object.getString("name"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return nameList;
    }

    /**
     * Load json into spinner from Shared Preferences
     */
    private void loadJsonFromSharedpreferences(){
        String strJson = sharedPref.getString(getString(R.string.campaing_list),"");
        if ((strJson != null) && (strJson != "")) {
            try {
                JSONArray jsonArray = new JSONArray(strJson);
                ArrayList nameList = loadJson(jsonArray);
                updateSpinner(nameList);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else
            updateSpinner(null);
    }

    @Override // Zeus
    public void onBackPressed() {
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(getString(R.string.quit_app))
                .setNegativeButton(getString(R.string.back), null)
                .setPositiveButton(getString(R.string.exit), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                }).create();
        d.show();
    }
}
