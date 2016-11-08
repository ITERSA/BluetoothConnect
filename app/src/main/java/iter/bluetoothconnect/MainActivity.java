package iter.bluetoothconnect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private  BluetoothAdapter mBluetoothAdapter;

    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private List<String> list;
    public static final String EXTRA_DEVICE_ADDRESS = "extra_device_address";

    private Toast globalToast;
    private ProgressBar  progressBar;

    private static final int REQUEST_ENABLE_BT = 1000;
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
                    mBluetoothAdapter.startDiscovery();
                    progressBar.setVisibility(View.VISIBLE);
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
                Intent i = new Intent(MainActivity.this, DataActivity.class);
                i.putExtra(EXTRA_DEVICE_ADDRESS, address);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_ENABLE_BT)&& (resultCode == RESULT_OK))
            //Toast.makeText(getApplicationContext(),"Bluetooth activado",Toast.LENGTH_SHORT).show();
            showGlobalToast("Bluetooth activado");
        super.onActivityResult(requestCode, resultCode, data);
    }

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

}
