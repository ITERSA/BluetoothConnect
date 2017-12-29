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
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
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
    public static final String EXTRA_DEVICE_ADDRESS = "extra_device_address";
    public static final String EXTRA_CAMPAING_NAME = "extra_campaing_name";

    private Toast globalToast;
    private ProgressBar  progressBar;

    private static final int REQUEST_ENABLE_BT = 1000;

    private Button btUploadFiles;
    private ImageButton btmap;
    private Spinner spinner;

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
                String notif = "No se han detectado dispositivos";
                if (count > 0)
                    notif = ""+count +" dispositivos encontrados";
                //Toast.makeText(getApplicationContext(), notif, Toast.LENGTH_SHORT).show();
                showGlobalToast(notif);
                progressBar.setVisibility(View.GONE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwitchCompat switchCompat = (SwitchCompat)findViewById(R.id.swichBluetooth);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    if (!mBluetoothAdapter.isEnabled()) {
                        //tgb.setChecked(true);
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }else{
                        //Toast.makeText(getApplicationContext(),"Bluetooth ya activado", Toast.LENGTH_LONG).show();
                        showGlobalToast("Bluetooth ya activado");
                    }
                }else{

                }
            }
        });
        globalToast = Toast.makeText(getApplicationContext(), null, Toast.LENGTH_LONG);
        spinner = (Spinner)findViewById(R.id.spinnerMap);

        btUploadFiles = (Button)findViewById(R.id.btSendData);
        btUploadFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = getSharedPreferences("Configs",Context.MODE_PRIVATE);
                Set<String> set = sharedPref.getStringSet(DataActivity.FILE_LIST, new HashSet<String>());
                if (set.size() > 0){
                    FtpFileUpload uploader = new FtpFileUpload(set, MainActivity.this);
                    uploader.execute();
                    //TODO if files uploaded, turn button disable
                }else{
                    btUploadFiles.setEnabled(false);
                    showGlobalToast("No hay ficheros para enviar");
                }
            }
        });
        Button btDiscover = (Button) findViewById(R.id.button);

        btDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter.isEnabled()){
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                        //Toast.makeText(getApplicationContext(),"Proceso reiniciado",Toast.LENGTH_SHORT).show();
                        showGlobalToast("Proceso reiniciado");
                    }
                    list.clear();
                    mNewDevicesArrayAdapter.notifyDataSetChanged();
                   /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
                        switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                            case PackageManager.PERMISSION_DENIED:
                                ((TextView) new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Runtime Permissions up ahead")
                                        .setMessage(Html.fromHtml("<p>To find nearby bluetooth devices please click \"Allow\" on the runtime permissions popup.</p>" +
                                                "<p>For more info see <a href=\"http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id\">here</a>.</p>"))
                                        .setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                                    ActivityCompat.requestPermissions(MainActivity.this,
                                                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                            1);
                                                }
                                            }
                                        })
                                        .show()
                                         .findViewById(android.R.id.message))
                                        .setMovementMethod(LinkMovementMethod.getInstance());       // Make the link clickable. Needs to be called after show(), in order to generate hyperlinks
                                break;
                            case PackageManager.PERMISSION_GRANTED:
                                break;
                        }
                    }*/
                   /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    }
                    mBluetoothAdapter.startDiscovery();
                    progressBar.setVisibility(View.VISIBLE);*/
                   askPermissions();
                }else{
                   // Toast.makeText(getApplicationContext(),"Bluetooth desactivado",Toast.LENGTH_SHORT).show();
                    showGlobalToast("Bluetooth desactivado");
                }

            }
        });
       // TextView title = (TextView) findViewById(R.id.topTitle);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        ListView listItems = (ListView)findViewById(R.id.leads_list);

        list = new ArrayList<String>();

        mNewDevicesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1, list);
        listItems.setAdapter(mNewDevicesArrayAdapter);

        listItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                mBluetoothAdapter.cancelDiscovery();
                progressBar.setVisibility(View.GONE);
                // Get the device MAC address, which is the last 17 chars in the View
                String info = tv.getText().toString();
                String address = info.substring(info.length() - 17);
                //Toast.makeText(getApplicationContext(),address,Toast.LENGTH_LONG).show();
                // Make an intent to start next activity while taking an extra which is the MAC address.
                EditText etName = (EditText)findViewById(R.id.campo_texto);
                String campaingName = etName.getText().toString();
                if (campaingName.length() <= 1)
                    campaingName = "data";
                Intent i = new Intent(MainActivity.this, DataActivity.class);
                i.putExtra(EXTRA_CAMPAING_NAME, campaingName);
                i.putExtra(EXTRA_DEVICE_ADDRESS, address);
                i.putExtra(MainActivityMap.PARAMETER_KML_NAME, spinner.getSelectedItem().toString());
                startActivity(i);
            }
        });

       mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            switchCompat.setEnabled(false);
            switchCompat.setChecked(false);
            btDiscover.setEnabled(false);
            /*title.setText("Bluetooth no disponible!");
            title.setTextColor(Color.RED);*/
        }else
            switchCompat.setChecked(mBluetoothAdapter.isEnabled());

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        updateSpinner();
        btmap = (ImageButton)findViewById(R.id.btMap);
        btmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, MainActivityMap.class);
                i.putExtra(MainActivityMap.PARAMETER_KML_NAME, spinner.getSelectedItem().toString());
                startActivity(i);
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
        checkIfUploadFiles();
    }

    @Override
    protected void onStart() {
        super.onStart();
        final LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            //showGlobalToast("GPS OFF!");
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

            // Setting Dialog Title
            alertDialog.setTitle("Configuración GPS");

            // Setting Dialog Message
            alertDialog.setMessage("GPS no está habilitado, ¿Deseas habilitarlo?");

            // Setting Icon to Dialog
            //alertDialog.setIcon(android.R.drawable.stat_sys_gps_on);

            // On pressing Settings button
            alertDialog.setPositiveButton("Habilitar GPS", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    dialog.dismiss();
                    startActivity(intent);
                }
            });

            // on pressing cancel button
            alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alertDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_ENABLE_BT)&& (resultCode == RESULT_OK))
            //Toast.makeText(getApplicationContext(),"Bluetooth activado",Toast.LENGTH_SHORT).show();
            showGlobalToast("Bluetooth activado");
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

    private void showGlobalToast(String text){
        if (globalToast != null) {
            globalToast.setText(text);
            globalToast.setDuration(Toast.LENGTH_LONG);
            //globalToast.show();
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

    private void checkIfUploadFiles(){
        SharedPreferences sharedPref = getSharedPreferences("Configs",Context.MODE_PRIVATE);
        Set<String> set = sharedPref.getStringSet(DataActivity.FILE_LIST, new HashSet<String>());
        if (set.size() > 0){
            btUploadFiles.setEnabled(true);
        }
        else
            btUploadFiles.setEnabled(false);
    }


    private void updateSpinner(){
        ArrayList<String> listSpinner = listRaw();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listSpinner);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();
    }

    private ArrayList<String> listRaw(){

        ArrayList<String> listSpinner = new ArrayList<>();
        Field[] fields=R.raw.class.getFields();
        for(int count=0; count < fields.length; count++){
            String file = fields[count].getName();
            if (file.startsWith("map"))
                listSpinner.add(file);
            //Log.i("Raw Asset: ", fields[count].getName());
        }
        return listSpinner;
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

   /* private ArrayList<String> listAssets(){

        String[] listFiles = null;
        try {
            listFiles = getResources().getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> listSpinner = new ArrayList<>(Arrays.asList(listFiles));
        return listSpinner;
    }*/
}
