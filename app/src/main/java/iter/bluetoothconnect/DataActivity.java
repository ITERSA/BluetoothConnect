package iter.bluetoothconnect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;

import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * This activity stablish a bluetooth connection with the selected device and show all data in graph
 */
//http://cursoandroidstudio.blogspot.com.es/2015/10/conexion-bluetooth-android-con-arduino.html
public class DataActivity extends AppCompatActivity  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private Spinner spinner;
    private TextView tvTemp;
    private TextView tvBat;
    private ValuesFragment dialogFragment;
    private ProgressDialogFragment progressDialogFragment;

    private Handler bluetoothIn;
    private Handler writeToFileHandler;
    private Handler uploadFileToFTPHandler;
    private Handler stablishBluetoothConnectionHandler;

    private final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final String FTP_IP = "ftp.iter.es";
    private static final int FTP_PORT = 21;
    private static final String FTP_USER = "jbarrancos";
    private static final String FTP_PASS = "bebacafebe";

    private ToggleButton tgRecord;
    private StringBuilder dataToFile = new StringBuilder();
    /***/
    private XYPlot plot;
   // private SimpleXYSeries series1;

    private ConnectedThread mConnectedThread;
    private HashMap<String, SimpleXYSeries> dataMapped;

    private String startDate;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        Log.v("DataActivity","onCreate");

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        dialogFragment = new ValuesFragment();
        progressDialogFragment = new ProgressDialogFragment();
        tgRecord = (ToggleButton)findViewById(R.id.toggleRecording);
        /*Toggle button ON/OFF reading data*/
        tgRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Log.v("DataActivity", "Recording...");
                    dataToFile.setLength(0);
                    clearAllSeries();   //Inicializa series
                    startDate = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy").format(new Date());

                    launchBluetoothReaderThread();
                    //plot.clear();   //Limpia grafica
                }else {
                    //Save to disk
                    if (dataToFile.length() > 0){
                        WriteToFileThread writeToFileThread = new WriteToFileThread(dataToFile.toString());
                        writeToFileThread.start();
                    }

                    stopBluetoothReader();
                    if (mConnectedThread != null)
                        mConnectedThread.interrupt();
                    Log.v("DataActivity", "Stop recording");
                }
            }
        });

        /*List of items*/
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               // Log.v("DataActivity", "tap"+position);
                //plot.setTitle(parent.getItemAtPosition(position).toString());
                String text = spinner.getSelectedItem().toString();
                plot.clear();
                addSerieToPlot(text);
                plot.redraw();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        plot = (XYPlot) findViewById(R.id.plot);
        tvTemp = (TextView) findViewById(R.id.tvTemp);
        tvBat = (TextView)findViewById(R.id.tvBat);
        initSeries();
       // plot.setDomainBoundaries(0, POINTS_IN_PLOT, BoundaryMode.AUTO);
        plot.setRangeBoundaries(null,null, BoundaryMode.AUTO);
        plot.setDomainStepValue(3);
        plot.setLinesPerRangeLabel(5);
        //plot.setRangeBottomMin(-10);
        /*plot.setRangeTopMax(400);
        plot.setRangeTopMin(1);*/
        //plot.setRangeBottomMax(0);

        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.##"));
        plot.setTitle(spinner.getSelectedItem().toString());

        /*Create a instance of Google Api Client*/
        if (mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        /**** Handlers ****/
        bluetoothIn = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == handlerState){
                    String readMessage = (String)msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.lastIndexOf("]");
                    int initOfLineIndex =  recDataString.lastIndexOf("[");
                    if (endOfLineIndex > 0) {
                        //String dataInPrint = recDataString.substring(0, endOfLineIndex);
                        //Quitamos corchetes de inicio y final
                        String dataInPrint = recDataString.substring(initOfLineIndex + 1, endOfLineIndex);
                        String[] values = dataInPrint.split(",");

                        // if toggle button is checked, then add data parsed to series for showing in Plot
                        if (tgRecord.isChecked()) {

                            //Insert values into map
                            for (int i = 0; i < values.length; i++) {
                                String[] items = values[i].split(":");
                                if (!items[0].contentEquals("LIC")) {
                                    SimpleXYSeries serie = dataMapped.get(items[0]);
                                    if (serie != null) {
                                        if (!items[1].contains("NAN")){
                                            try {
                                                Number n = Double.parseDouble(items[1]);
                                                if (n != null )
                                                    serie.addLast(null, n);
                                            }catch (NumberFormatException e){
                                                serie.addLast(null, 0);
                                            }catch (NullPointerException e){
                                                serie.addLast(null, 0);
                                            }
                                        }else
                                            serie.addLast(null, 0);
                                    }
                                } else {    //Parse "LIC" xml
                                    String wellFormedXML = "<xml>" + items[1] + "</xml>";
                                    ArrayList<String> xmlItems = parseXml(wellFormedXML);
                                    if (xmlItems != null){
                                        for (int j = 0; j < xmlItems.size(); j++){
                                            String[] xmlItem = xmlItems.get(j).split(":");
                                            SimpleXYSeries serie = dataMapped.get(xmlItem[0].toUpperCase());
                                            if (serie != null) {
                                                Number n = Double.parseDouble(xmlItem[1]);
                                                if (n != null ){
                                                    serie.addLast(null, n);
                                                }else{
                                                    serie.addLast(null, 0);
                                                }
                                                //parse LIC <co2>394.76</co2><tem>51.2</tem><pre>101383</pre><h2o>394.76</h2o><bat>12.4</bat>
                                            }
                                            //Show info in dialog if it is shown
                                            if (xmlItem[0].toLowerCase().equals("bat")){
                                                updateBattery(xmlItem[1]);
                                                if ((dialogFragment != null) && (dialogFragment.shown)){
                                                    dialogFragment.updateTV1("Bateria \n"+xmlItem[1]+"%");
                                                }
                                            }
                                            if (xmlItem[0].toLowerCase().equals("tem")){
                                                updateTemp(xmlItem[1]);
                                                if ((dialogFragment != null) && (dialogFragment.shown)){
                                                    dialogFragment.updateTV2("Temperatura \n"+xmlItem[1]+"º");
                                                }
                                            }
                                            if (xmlItem[0].toLowerCase().equals("pre")){
                                                if ((dialogFragment != null) && (dialogFragment.shown)){
                                                    dialogFragment.updateTV3("Presion \n"+xmlItem[1]+"-");
                                                }
                                            }
                                            if (xmlItem[0].toLowerCase().equals("flu")){
                                                if ((dialogFragment != null) && (dialogFragment.shown)){
                                                    dialogFragment.updateTV3("Flujo \n"+xmlItem[1]+"-");
                                                }
                                            }

                                        }
                                    }
                                }
                                plot.redraw();
                                dataToFile.append(dataInPrint + "\n");
                                Log.v("DataActivity", dataInPrint);
                            }
                            recDataString.setLength(0); //clear buffer
                        }
                    }
                }
            }
        };

        /*handle FTP upload*/
        uploadFileToFTPHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1)
                Toast.makeText(getApplicationContext(),"Fichero subido con exito "+ (String)msg.obj, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getApplicationContext(),"Error subiendo el fichero "+ (String)msg.obj +" al FTP!", Toast.LENGTH_LONG).show();
            }
        };

        /*handle file writer*/
        writeToFileHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final String currentPath = (String)msg.obj;
            if (msg.what == 1){
                AlertDialog d = new AlertDialog.Builder(DataActivity.this)
                        .setTitle(R.string.app_name)
                        .setIcon(R.mipmap.ic_launcher)
                        .setMessage("Datos salvados en el fichero "+ currentPath)
                        .setPositiveButton("Guardar y Enviar al servidor", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                UploadFileToFTPThread uploadFileToFTPThread = new UploadFileToFTPThread(currentPath);
                                uploadFileToFTPThread.start();
                            }
                        })
                        .setNegativeButton("Cancelar y Descartar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File f = new File(currentPath);
                                f.delete();
                            }
                        })

                        .setNeutralButton("Abrir con", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openWith(currentPath);
                            }
                        }).create();

                d.show();
            }else{
                Toast.makeText(getApplicationContext(),"Error generando el fichero "+ (String)msg.obj, Toast.LENGTH_LONG).show();
            }

            }
        };

        /*Handle stablish bluetooth connection*/
       stablishBluetoothConnectionHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                progressDialogFragment.dismiss();
                if (msg.what == 1){
                    if ((btSocket != null) && (btSocket.isConnected())){
                        mConnectedThread = new ConnectedThread(btSocket);
                        mConnectedThread.write("1");
                        mConnectedThread.start();
                    }
                }
            }
        };
    }

    /*Start bluetooth connection */
   private void launchBluetoothReaderThread(){
       Intent intent = getIntent();
       String address = intent.getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);   //get current MAC
       StablishBluetoothConnection stablishBluetoothConnection = new StablishBluetoothConnection(address);
       stablishBluetoothConnection.start();
       /*  Log.v("DataActivity", "Starting bluetooth");
        if ((btSocket == null) || (!btSocket.isConnected())){
            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            try {
                btSocket = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
            }catch (IOException e){
                Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
                finish();
            }
            try {
                btSocket.connect();
            }catch (IOException e){
                try {
                    btSocket.close();
                }catch (IOException e2){

                }
                Toast.makeText(getBaseContext(), "No se pudo establecer conexion", Toast.LENGTH_LONG).show();
                finish();
            }
            if (btSocket.isConnected()){
                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.write("1");
                mConnectedThread.start();
            }
            Log.v("DataActivity", "Finishing bluetooth");
          //  loadingdialog.dismiss();
        }*/
    }

    /*Stop bluetooth reader*/
    private void stopBluetoothReader(){
        if (mConnectedThread != null)
            mConnectedThread.write("2");    //Turn off led
        if ((btSocket != null) && (btSocket.isConnected())){
            try {
                btSocket.close();   //Close bluetooth socket
            }catch (IOException e){

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnectedThread != null) {
            mConnectedThread.write("2");
            mConnectedThread.interrupt();
        }
        stopBluetoothReader();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /*Launch openWith activity*/
    private void openWith(String path){
        if (path != null){
            File f= new File(path);
            if (f != null){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(f),"text/plain");
                Intent chooser = Intent.createChooser(intent, "Abrir con...");
                startActivity(chooser);
            }
        }
    }

    /*Initialize all series*/
    private void initSeries(){
        dataMapped = new HashMap<String, SimpleXYSeries>();
        String[] spinnerItems = getResources().getStringArray(R.array.items_name); //leemos valores de string
        for (int i = 0; i < spinnerItems.length; i++){
            ArrayList<Number> values = new ArrayList<Number>();
            dataMapped.put(spinnerItems[i],new SimpleXYSeries(values, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, spinnerItems[i]));
        }
    }

    /*Clear all series*/
    private void clearAllSeries(){

        if (dataMapped != null){

            for (SimpleXYSeries serie : dataMapped.values()){
                //remove items in plot
                while (serie.size() > 0) {
                    serie.removeLast();
                }
            }
        }
    }

    /**
     * Add serie called serieName to plot
     * @param serieName Name of the serie
     */
    private void addSerieToPlot(String serieName){

        SimpleXYSeries serie = dataMapped.get(serieName);
        if (serie != null){
            //LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, Color.RED, null, null);
            LineAndPointFormatter series1Format = new LineAndPointFormatter(
                    Color.rgb(200, 0, 0), null, null, null);
            series1Format.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
            series1Format.getLinePaint().setStrokeWidth(7);
            plot.addSeries(serie, series1Format);
        }
    }

    /**
     * Parse a XML
     * @param s
     * @return ArrayList of items (name:value)
     */
    private ArrayList<String> parseXml(String s){
       // Log.v("Parsed", s);
        XMLReader xmlReader = null;
        try {
            xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
        // create a SAXXMLHandler
        SaxXMLHandler saxHandler = new SaxXMLHandler();
        // store handler in XMLReader
        xmlReader.setContentHandler(saxHandler);
        // the process starts
        try {

            xmlReader.parse(new InputSource(new StringReader(s)));

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        }
        return saxHandler.items;
    }

    /*Update fields*/
    private void updateTemp(String n){
        tvTemp.setText(n + "º");
    }
    private void updateBattery(String n){
        tvBat.setText(n+"%");
    }
    public void showExtraInfo(View v){
        dialogFragment.show(getSupportFragmentManager(), "Sample Fragment");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /************************* THREADS *************************/

    /**
     * Thread that write a file to SDCard
     */
    private class WriteToFileThread extends Thread{

        private String data;
        private File file;

        public WriteToFileThread(String _data){

            Location loc = null;
            if ((mGoogleApiClient != null) && (mGoogleApiClient.isConnected())){
                try {
                  loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                }catch (SecurityException e){
                    Log.e("DataActivity","Error getting activity");
                }
            }

            data = _data;
            if (loc != null){
                data = startDate + "\n Location: (" + loc.getLatitude() +  ", " + loc.getLongitude() + ")\n" + data;
            }else
                data = startDate + "\n"+ data;

            String path = Environment.getExternalStorageDirectory() + File.separator  + "data";
            // Create the folder.
            File folder = new File(path);
            folder.mkdirs();
            // Create the file.
            String date = new SimpleDateFormat("HH-mm-ss-dd-MM-yyyy").format(new Date());
            file = new File(folder, "data_"+ date+".txt");
            data += date;
            //TODO adjuntar temperatura y demas valores
        }

        @Override
        public void run(){
            // Save your stream, don't forget to flush() it before closing it.
            int status = 0;
            try{
                FileWriter writer = new FileWriter(file);
                writer.append(data);
                writer.flush();
                writer.close();
                Log.v("DataActivity","File saved to "+file.getAbsolutePath());
                //Log.v("DataActivity", data);
                status = 1;
            }catch (IOException e){
                Log.e("Exception", "File write failed: " + e.toString());
            }
            Message msg = new Message();
            msg.what = status;
            msg.obj = file.getAbsolutePath();
            if (writeToFileHandler != null)
                writeToFileHandler.sendMessage(msg);
        }
    }

    /**
     * Thread that upload a file to FTP
     */
    private class UploadFileToFTPThread extends Thread{

        private String path;
        public UploadFileToFTPThread(String _path){
            path = _path;
        }

        @Override
        public void run() {
            FTPClient con = null;
            boolean result = false;
            File f = new File(path);
            try {
                con = new FTPClient();
                con.connect(FTP_IP,FTP_PORT);
                if (con.login(FTP_USER,FTP_PASS)){
                    con.enterLocalPassiveMode();
                    con.setFileType(FTP.BINARY_FILE_TYPE);
                    if (f != null) {
                        FileInputStream in = new FileInputStream(f);
                        //con.changeWorkingDirectory("/appflux/");
                        result = con.storeFile("/appflux/"+f.getName(),in);
                        if (result)
                            Log.v("DataActivity", "Upload success");
                    }
                    con.logout();
                    con.disconnect();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
            Message msg = new Message();
            if (result)
                msg.what = 1;
            else
                msg.what = 0;
            msg.obj = f.getName();
            if (uploadFileToFTPHandler != null)
                uploadFileToFTPHandler.sendMessage(msg);
        }
    }

    /**
     * Thread that stablish a bluetooth connection with a socket
     */
    private class StablishBluetoothConnection extends Thread{

        private String address;
        public StablishBluetoothConnection(String _address) {
            address = _address;
            progressDialogFragment.show(getSupportFragmentManager(), "progressDialog");
            if ((btSocket == null) || (!btSocket.isConnected())) {
                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                try {
                    btSocket = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }

        @Override
        public void run() {
            boolean result = false;
            if ((btSocket != null) || (!btSocket.isConnected())){
                try {
                    btSocket.connect();
                    result = true;
                }catch (IOException e){
                    try {
                        btSocket.close();
                    }catch (IOException e2){

                    }
                    Toast.makeText(getApplicationContext(), "No se pudo establecer conexion", Toast.LENGTH_LONG).show();
                    //
                    result = false;
                }
                //Log.v("DataActivity", "Finishing bluetooth");
                }
            Message msg = new Message();
            if (result)
                msg.what = 1;
            else
                msg.what = 0;
            if (stablishBluetoothConnectionHandler != null)
                stablishBluetoothConnectionHandler.sendMessage(msg);
        }
    }

    /**
     * Thread that contiuosly read data from a bluetooth socket
     */
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }catch (IOException e){}
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run(){
            //int bytes;
            while (true)
                try{
                    if (this.isInterrupted())
                        break;
                    int bytesAvailable = mmInStream.available();
                    if(bytesAvailable > 0){
                        byte[] buffer = new byte[bytesAvailable];
                        int bytes = mmInStream.read(buffer);
                        String readMessage = new String(buffer, 0, bytes);
                        if (!readMessage.isEmpty()){
                            if (bluetoothIn != null)
                                bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                        }
                    }
                }catch (IOException e){
                    break;
                }
        }

        public void write(String input){
            byte[] bytes = input.getBytes();
            try {
                mmOutStream.write(bytes);
            }catch (IOException e){}
        }

        public void cancel(){
            try{
                mmSocket.close();
            }catch (IOException e){

            }
        }
    }
}
