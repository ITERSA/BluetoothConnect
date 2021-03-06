package iter.bluetoothconnect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;

import android.graphics.Paint;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.androidplot.Plot;
import com.androidplot.PlotListener;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * This activity stablish a bluetooth connection with the selected device and show all data in graph
 */
//http://cursoandroidstudio.blogspot.com.es/2015/10/conexion-bluetooth-android-con-arduino.html
public class DataActivity extends AppCompatActivity  {

    private static final int K = 1; //TODO change to value...
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 112;
    private static final int REQUEST_PERMISSIONS_FINE_LOCATION = 34;
    private Spinner spinner;
    private TextView tvTemp;
    private TextView tvBat;
    private TextView tvPress, tvCurrentVal;
    private ProgressDialogFragment progressDialogFragment;

    private Handler bluetoothIn;
    private Handler writeToFileHandler;
    private Handler uploadFileToFTPHandler;

    //private StablishBluetoothConnection stablishBluetoothConnection;
    private Handler stablishBluetoothConnectionHandler;

    private final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final String FTP_IP = "1nv0lc4n.dyndns.org";
    public static final int FTP_PORT = 12221;
    public static final String FTP_USER = "movgas";
    public static final String FTP_PASS = "m0vg4s";
    private static final String WORKING_DIRECTORY = "/Portable";

    private static final String COMMAND_START = "+COMMAND:1\n";
    private static final String COMMAND_STOP = "+COMMAND:0\n";


    private ToggleButton tgRecord;
    //private StringBuilder dataToFile = new StringBuilder();
    private JSONArray jsonArray = new JSONArray();
    /***/
    private XYPlot plot;
    private PanZoom panZoom;
    private int minX;
    private int maxX;

    private ConnectedThread mConnectedThread;
    private HashMap<String, SimpleXYSeries> dataMapped;

    private String startDate;
    private GoogleApiClient mGoogleApiClient;

    //private LinearLayout llMenuSlope;
    private Button btSaveData;

    //private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    private Location mLocation;

    private String campaingName, userName;

    private boolean isDone;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   //keep screen ON
        isDone = false;
        campaingName = getIntent().getStringExtra(getString(R.string.extra_campaing_name));
        if (campaingName == null)
            campaingName = "-";
        userName = getIntent().getStringExtra(getString(R.string.extra_key_username));

        double lat = getIntent().getDoubleExtra(getString(R.string.extra_lat),0.0);
        double lng = getIntent().getDoubleExtra(getString(R.string.extra_long),0.0);

        if ((lat != 0.0) && (lng != 0.0)){
            mLocation = new Location("MapActivity");
            mLocation.setLatitude(lat);
            mLocation.setLongitude(lng);
            mLocation.setAccuracy(getIntent().getFloatExtra(getString(R.string.extra_accuracy),0));
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        minX = -1;
        maxX = 1;

        progressDialogFragment = new ProgressDialogFragment();
        tgRecord = (ToggleButton)findViewById(R.id.toggleRecording);
        /*Toggle button ON/OFF reading data*/
        tgRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showCurrentValue(isChecked);
            if (isChecked){
                Log.v("DataActivity", "Recording...");
                offOptionMenu();
               // plot.setTitle("");
                panZoom.setEnabled(false);
                jsonArray = new JSONArray();
                clearAllSeries();   //Inicializa series
                plot.setDomainBoundaries(0,1, BoundaryMode.AUTO);
                //change left drawable
                int imgResource = android.R.drawable.ic_media_pause;
                tgRecord.setCompoundDrawablesWithIntrinsicBounds(imgResource, 0, 0, 0);
                startDate = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss").format(new Date());
                readBtData();
            }else {
                //change left drawable
                int imgResource = android.R.drawable.ic_media_play;
                tgRecord.setCompoundDrawablesWithIntrinsicBounds(imgResource, 0, 0, 0);
                //stopBluetoothReader();

                /*if (mConnectedThread != null)
                    mConnectedThread.interrupt();*/
                pauseReadBtData();
                //Log.v("DataActivity", "Stop recording");
                panZoom.setEnabled(true);
                forceUpdateSlopeText();
                activeOptionMenu();
            }

            }
        });

        /*List of items*/
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               // Log.v("DataActivity", "tap"+position)
                String text = spinner.getSelectedItem().toString();
                plot.clear();
                Number lastValue = addSerieToPlot(text);
                updateTextView(tvCurrentVal, lastValue, "");
                plot.redraw();

                /*Update slope*/
                if (!tgRecord.isChecked())
                    forceUpdateSlopeText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        plot = (XYPlot) findViewById(R.id.plot);
        tvTemp = (TextView) findViewById(R.id.tvTemp);
        tvBat = (TextView)findViewById(R.id.tvBat);
        tvPress = (TextView)findViewById(R.id.tvPress);
        tvCurrentVal = (TextView)findViewById(R.id.tvCurrentVal);
        initSeries();
       // plot.setDomainBoundaries(0, POINTS_IN_PLOT, BoundaryMode.AUTO);
        plot.setRangeBoundaries(0,0, BoundaryMode.AUTO);
        plot.setDomainBoundaries(0,1, BoundaryMode.AUTO);
        plot.setDomainStepValue(5);
        plot.setLinesPerRangeLabel(5);
        panZoom = PanZoom.attach(plot, getApplicationContext());

        panZoom.setZoom(PanZoom.Zoom.STRETCH_HORIZONTAL);
        panZoom.setPan(PanZoom.Pan.HORIZONTAL);
        panZoom.setEnabled(false);
        plot.getOuterLimits().set(0,1, 0, 50000);
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("#####.##"));
       // plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new DecimalFormat("#####.#"));
        plot.setBorderStyle(Plot.BorderStyle.NONE, null, null);
        plot.setPlotMargins(0, 0, 0, 0);
        plot.setPlotPadding(0, 0, 0, 0);
        plot.getLayoutManager().remove(plot.getRangeTitle());
        plot.getGraph().setPaddingLeft(50);     //left marging labels cut off

        //plot.setRangeBottomMin(-10);
        /*plot.setRangeTopMax(400);
        plot.setRangeTopMin(1);*/
        //plot.setRangeBottomMax(0);
        //plot.setDomainStep(StepMode.INCREMENT_BY_VAL, 0);

        plot.addListener(new PlotListener() {
            @Override
            public void onBeforeDraw(Plot source, Canvas canvas) {
               // Log.v("BeforeDraw", "OK");
            }

            @Override
            public void onAfterDraw(Plot source, Canvas canvas) {
                int domainMin = (int)(Math.ceil(plot.getBounds().getMinX().doubleValue()));
                int domainMax = (int)(Math.floor(plot.getBounds().getMaxX().doubleValue()));
                if ((domainMin != minX) || (domainMax != maxX)){
                    minX = domainMin;
                    maxX = domainMax;
                    //if (!tgRecord.isChecked()){
                        String currentItem = spinner.getSelectedItem().toString();
                        double slope = calculateSlope(currentItem, minX, maxX);
                        double correlation = calculatePearsonCorrelation(currentItem, minX, maxX);
                        if (currentItem.contentEquals("co2"))
                            plot.setTitle(String.format( "Pte(*K): %.5f \n R2: %.5f", slope, correlation*correlation));
                        else
                            plot.setTitle(String.format( "Pte: %.5f \n R2: %.5f", slope, correlation*correlation));
                  //  }
                }
            }
        });

        //llMenuSlope = (LinearLayout)findViewById(R.id.llMenuSlope);
        btSaveData = (Button) findViewById(R.id.btOptions);
        btSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
            showSaveDialog();
            }
        });

        /**** Handlers ****/
        bluetoothIn = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == handlerState){
                    String readMessage = (String)msg.obj;
                    Log.v("current_data","---***-----");
                    Log.v("current_data","chunk: " + readMessage);
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.lastIndexOf("\n");
                    //int endOfLineIndex2 = recDataString.lastIndexOf("\r");
                    Log.v("current_data","raw data: " + recDataString);
                    if (endOfLineIndex > 0) {
                        String current_data = recDataString.toString();

                        evaluateChunk(current_data);
                        //Quitamos corchetes de inicio([) y final (]/r/n)
//                        int initOfLineIndex =  recDataString.indexOf("[");
//                        if (initOfLineIndex > -1) {
//                            //FIXME in some cases it brokes
//                            String dataInPrint = recDataString.substring(initOfLineIndex + 1, endOfLineIndex - 2);
//                           //
//
//                            // if toggle button is checked, then add data parsed to series for showing in Plot
//                            if (tgRecord.isChecked()) {
//
//                                customParser(",", ":", dataInPrint);
//                                /*Fixed bug: Update zoomable range to current max X (visible)*/
//                                plot.redraw();
//                                //TODO update current value widget
//                                Number n = maxX + 1;
//                                //Number number = plot.getBounds().getMaxY();
//                                plot.getOuterLimits().set(0, n, 0, 50000);
//                                String locationText = "";
//                                if (mLocation != null)
//                                    locationText = String.format("(Lat: %f - Long: %f - Alt:%.1f) Error: %.1f", mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude(), mLocation.getAccuracy());
//                                locationText = locationText.replace(",",".");
//                                dataToFile.append(new SimpleDateFormat("HH:mm:ss").format(new Date())+ " - " + dataInPrint + " "+ locationText +"\n");
//                                updateInfoWidget();
//                                Log.v("DataActivity", dataInPrint);
//                                /***/
//
//                                recDataString.setLength(0); //clear buffer
//                            }
//                        }
                    }
                }
            }
        };

        /*handle FTP upload*/
        uploadFileToFTPHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String fileName = (String)msg.obj;
            isDone = true;
            if (msg.what == 1)
                Toast.makeText(getApplicationContext(),fileName + getResources().getString(R.string.up_success), Toast.LENGTH_LONG).show();
            else{
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.up_error) + fileName + getResources().getString(R.string.to_ftp), Toast.LENGTH_LONG).show();
                updateFileList(fileName);
            }
                }
        };

        /*handle file writer*/
        writeToFileHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                isDone = true;
            super.handleMessage(msg);
            final String currentPath = (String)msg.obj;
            if (msg.what == 1){
                UploadFileToFTPThread uploadFileToFTPThread = new UploadFileToFTPThread(currentPath);
                uploadFileToFTPThread.start();
                offOptionMenu();
            }else{
                Toast.makeText(getApplicationContext(),getString(R.string.error_file) + " " + (String)msg.obj, Toast.LENGTH_LONG).show();
            }
            }
        };

        /*Handle stablish bluetooth connection*/
       stablishBluetoothConnectionHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                progressDialogFragment.dismiss();
               if (msg.what == 0){
                   Toast.makeText(getApplicationContext(), getString(R.string.error_bt_connection), Toast.LENGTH_LONG).show();
                   finish();
                    /*if ((btSocket != null) && (btSocket.isConnected())){
                        mConnectedThread = new ConnectedThread(btSocket);
                        //mConnectedThread.write("+COMMAND:2\n");
                        writeSocket(COMMAND_START);
                        //mConnectedThread.write("0");
                        mConnectedThread.start();
                        Log.v("Connect", "OK!");
                        //mConnectedThread.write("+COMMAND:0\n");     //TODO turn on led  replace -> "+COMMAND:0"
                    }*/
                }
            }
        };
    }

    private void readBtData(){
        if ((btSocket != null) && (btSocket.isConnected())){
            if ((mConnectedThread != null) && mConnectedThread.isAlive()) {
                mConnectedThread.setReadStatus(false);
                mConnectedThread.interrupt();
            }
            mConnectedThread = new ConnectedThread(btSocket);

            //mConnectedThread.write("+COMMAND:2\n");
            writeSocket(COMMAND_START);

            mConnectedThread.start();

            //mConnectedThread.write("0");
            Log.v("Connect", "OK!");
            //mConnectedThread.write("+COMMAND:0\n");     //TODO turn on led  replace -> "+COMMAND:0"
        }
    }

    private void pauseReadBtData(){
        if ((btSocket != null) && (btSocket.isConnected())) {
            if ((mConnectedThread != null) && mConnectedThread.isAlive()) {
                mConnectedThread.setReadStatus(false);
                mConnectedThread.interrupt();

            }
            writeSocket(COMMAND_STOP);
        }
    }

    private void writeSocket(String command){
        if ((btSocket != null) && btSocket.isConnected()){
            try {
                OutputStream tmpOut = btSocket.getOutputStream();
                tmpOut.write(command.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*Start bluetooth connection */
   private void launchBluetoothReaderThread(){
       if ((btSocket == null) || (!btSocket.isConnected())){
           Intent intent = getIntent();
           String address = intent.getStringExtra(getString(R.string.extra_device_address));   //get current MAC
           if (address!= null && !address.isEmpty()){
               StablishBluetoothConnection stablishBluetoothConnection = new StablishBluetoothConnection(address);
               stablishBluetoothConnection.start();
           }
       }
    }

    /*Stop bluetooth reader*/
    private void stopBluetoothReader(){
       /* if (mConnectedThread != null)
            //mConnectedThread.write("+COMMAND:1\n");    //TODO Turn off led  replace -> "+COMMAND:0"
            mConnectedThread.write("+COMMAND:3\n");*/
        writeSocket(COMMAND_STOP);
          //  mConnectedThread.write("1");    //TODO Turn off led  replace -> "+COMMAND:0"
        if ((btSocket != null) && (btSocket.isConnected())){
            try {
                btSocket.close();   //Close bluetooth socket
            }catch (IOException e){
                Log.e("Socket", "ERROR: "+e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        if ((mConnectedThread != null) && mConnectedThread.isAlive()){
          //  mConnectedThread.write("+COMMAND:1\n");  //TODO Turn off led  replace -> "+COMMAND:1"
            //mConnectedThread.write(COMMAND_STOP);
            //mConnectedThread.write("1");  //TODO Turn off led  replace -> "+COMMAND:1"
            mConnectedThread.setReadStatus(false);
            mConnectedThread.interrupt();
        }
        stopBluetoothReader();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mLocation == null){
            if (!checkPermissions()) {
                startLocationPermissionRequest();
            } else {
                getLastLocation();
            }
        }
        launchBluetoothReaderThread();
    }

  /*  @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (plot != null){
            plot.calculateMinMaxVals();
            plot.redraw();
        }
    }*/

    @Override
    public void finish() {

        if (isDone){
            String pointName =  getIntent().getStringExtra(getString(R.string.extra_point_name));
            if (pointName == null)
                pointName = "-";
            Intent intent = new Intent();
            intent.putExtra(getString(R.string.extra_point_name), pointName);
            setResult(Activity.RESULT_OK, intent);

        }else
            setResult(Activity.RESULT_CANCELED);
        super.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_STORAGE:
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //Log.i(TAG, "Permission has been denied by user");
                } else {
                    //Log.i(TAG, "Permission has been granted by user");
                    showDialog();
                }
                break;
            case REQUEST_PERMISSIONS_FINE_LOCATION:
                if (grantResults.length <= 0) {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i("DataActivity", "User interaction was cancelled.");
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted.
                    getLastLocation();
                }
                break;
        }
    }

     /*Initialize all series*/
    private void initSeries(){
        dataMapped = new HashMap<String, SimpleXYSeries>();
        String[] spinnerItems = getResources().getStringArray(R.array.items_name); //leemos valores de string
        for (int i = 0; i < spinnerItems.length; i++){
            ArrayList<Number> values = new ArrayList<Number>();
            //SimpleXYSeries xy = new SimpleXYSeries(values, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, spinnerItems[i]);
            SimpleXYSeries xy = new SimpleXYSeries(new ArrayList<Number>(), new ArrayList<Number>(), spinnerItems[i]);
            //xy.useImplicitXVals();

            dataMapped.put(spinnerItems[i],xy);
            //dataMapped.put(spinnerItems[i],new SimpleXYSeries(new ArrayList<Number>(),new ArrayList<Number>(), spinnerItems[i]));
        }
    }

    private void initSerie(String serieName){
        if (dataMapped == null){
            dataMapped = new HashMap<String, SimpleXYSeries>();
        }
        //ArrayList<Number> values = new ArrayList<Number>();
        //
        //SimpleXYSeries xy = new SimpleXYSeries(new ArrayList<Number>(), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, serieName);
        SimpleXYSeries xy = new SimpleXYSeries(new ArrayList<Number>(),new ArrayList<Number>(), serieName);
        //xy.useImplicitXVals();
        dataMapped.put(serieName, xy);

       // dataMapped.put(serieName, new SimpleXYSeries(new ArrayList<Number>(),new ArrayList<Number>() , serieName));
    }

    /**
     *Prevent show raw, co2abs, cellpress, celltemp
     *
     */
    private void updateSpinner(String name) {

            SpinnerAdapter spinnerAdapter = spinner.getAdapter();
            int size = spinnerAdapter.getCount();
            ArrayList<String> listSpinner = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                //Prevent show raw, co2abs, cellpress, celltemp
                listSpinner.add(spinnerAdapter.getItem(i).toString());
            }
            listSpinner.add(name);
            //ArrayAdapter<String> spinnerAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listSpinner);
            ArrayAdapter<String> spinnerAdapter2 = new ArrayAdapter<String>(this, R.layout.spinner_item, listSpinner);
            //spinnerAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerAdapter2.setDropDownViewResource(R.layout.spinner_item);
            spinner.setAdapter(spinnerAdapter2);
            spinnerAdapter2.notifyDataSetChanged();
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
    private Number addSerieToPlot(String serieName){
        Number lastValue = 0;
        SimpleXYSeries serie = dataMapped.get(serieName);
        if ((serie != null) &&  (serie.size() > 0)){
            //LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, Color.RED, null, null);
            //LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.rgb(200, 0, 0), null, null, null);
            //LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.rgb(150, 0, 0), null, Color.argb(125, 100, 0, 0), null);

            /*PointLabelFormatter  plf = new PointLabelFormatter();
            plf.getTextPaint().setTextSize(18);
            plf.getTextPaint().setColor(Color.BLACK);*/

            LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.rgb(220, 20, 60), Color.rgb(0, 0, 0), null, null);
            series1Format.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
            series1Format.getLinePaint().setStrokeWidth(4);
            series1Format.getVertexPaint().setStrokeWidth(8);
            //series1Format.setPointLabelFormatter(plf);
            plot.addSeries(serie, series1Format);
            lastValue = serie.getY(serie.size() - 1);
        }
        return lastValue;
    }

    private void forceUpdateSlopeText(){
        /*Update slope*/
        minX = (int)(Math.ceil(plot.getBounds().getMinX().doubleValue()));
        maxX = (int)(Math.floor(plot.getBounds().getMaxX().doubleValue()));
        String item = spinner.getSelectedItem().toString();
        double slope = calculateSlope(item, minX, maxX);
        double correlation = calculatePearsonCorrelation(item, minX, maxX);
        if (item.contentEquals("co2"))
            plot.setTitle(String.format( "Pte(*K): %.5f \n R2: %.5f", slope, correlation * correlation));
        else
            plot.setTitle(String.format( "Pte: %.5f \n R2: %.5f", slope, correlation * correlation));
    }

    /**
     * Calculate slope
     * @param serieName String
     * @param domainMin int min value X axis
     * @param domainMax int max value X axis
     * @return double slope value
     */
    private double calculateSlope(String serieName, int domainMin, int domainMax){

        double result = 0.0;
        SimpleXYSeries serie = dataMapped.get(serieName);
        if ((serie.size() > 2) && (domainMin < domainMax)){
            int size = domainMax;
            if (domainMax > serie.size())
                size = serie.size() - 1;
            SimpleRegression simpleRegression = new SimpleRegression(true);
            for (int i = domainMin; i < size; i++){
                //TODO revise range
                simpleRegression.addData(serie.getX(i).doubleValue(), serie.getY(i).doubleValue());
            }
            result = simpleRegression.getSlope();
            if (serieName.contentEquals("co2"))
                result = result * K;

            //Log.v("Slope",""+domainMin + " , "+domainMax + "  Slope: "+ result);
        }
        //Toast.makeText(DataActivity.this, "Slope range"+domainMin + " - "+domainMax ,Toast.LENGTH_LONG).show();
        return result;
    }

    private double calculatePearsonCorrelation(String serieName, int domainMin, int domainMax){
        double result = 0.0;
        SimpleXYSeries serie = dataMapped.get(serieName);
        if ((serie.size() > 2) && (domainMin < domainMax)) {
            int rangeTop = domainMax;
            int serieSize =  serie.size();
            if (rangeTop > serieSize)
                rangeTop = serieSize - 1;

            int sizeVector = rangeTop - domainMin + 1;
            double[] x = new double[sizeVector];
            double[] y = new double[sizeVector];

            int cont = 0;
            for (int i = domainMin; i < rangeTop; i++){
                double dx = serie.getX(i).doubleValue();
                x[cont] = dx;
                double dy = serie.getY(i).doubleValue();
                y[cont] = dy;
                cont++;
            }
            result = new PearsonsCorrelation().correlation(x, y);
        }
        return result;
    }

    /**
     *
     */
    private void showCurrentValue(boolean checking){
        if (checking){
            tvCurrentVal.setVisibility(View.VISIBLE);
        }else
            tvCurrentVal.setVisibility(View.INVISIBLE);
    }
    /**
     *
     */
    private void activeOptionMenu(){
      //  llMenuSlope.setVisibility(View.VISIBLE);
        if (!isDone) {
            btSaveData.clearAnimation();
            final Animation flashAnimation = new AlphaAnimation(1, 0);
            flashAnimation.setDuration(500);
            flashAnimation.setInterpolator(new LinearInterpolator());
            flashAnimation.setRepeatCount(3);
            flashAnimation.setRepeatMode(Animation.RESTART);
            btSaveData.startAnimation(flashAnimation);
        }
        btSaveData.setBackgroundResource(R.drawable.my_button_active);
        btSaveData.setEnabled(true);
    }

    private void offOptionMenu(){
        btSaveData.clearAnimation();
        btSaveData.setBackgroundResource(R.drawable.my_button);
        btSaveData.setEnabled(false);
      //  llMenuSlope.setVisibility(View.GONE);
    }

   /* private boolean isOptionsMenuActive(){
        boolean result = true;
        if (btSaveData.isActivated())
        //    result = false;
     return result;
    }*/

    private void updateInfoWidget(){

      updateTextView(tvTemp, getItemValue("celltemp"), "ºC");
      //updateTextView(tvBat, getItemValue("ivolt"), "V");
      String item = spinner.getSelectedItem().toString();
      updateTextView(tvCurrentVal,getItemValue(spinner.getSelectedItem().toString()),"");
      double bat_v =  getItemValue("bat_v").doubleValue();
      Number n_bat = bat_v;
      updateTextView(tvBat, n_bat, "V");
      double pressValue =  getItemValue("cellpres").doubleValue();
      pressValue = pressValue /1013.25;
      Number n = pressValue;
      updateTextView(tvPress, n, "a");
  }

  private Number getItemValue(String item){
      Number n;
      SimpleXYSeries serie = dataMapped.get(item);
      if (serie == null){
          n = 0;
      }else{
          if (serie.size() > 0) {
              n = serie.getY(serie.size() - 1);
              if (n == null)
                  n = 0;
          }else
              n = 0;
      }
      return n;
  }

  private void showDialog(){
      AlertDialog d = new AlertDialog.Builder(DataActivity.this)
              .setTitle(R.string.app_name)
              //.setIcon(android.R.drawable.ic_menu_save)
              .setIcon(R.mipmap.ic_launcher)
              .setMessage(R.string.ftp)
              .setPositiveButton(getString(R.string.send), new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      //if (dataToFile.length() > 0) {
                      if (jsonArray.length() > 0){
                          //WriteToFileThread writeToFileThread = new WriteToFileThread(dataToFile.toString());
                          WriteToFileThread writeToFileThread = new WriteToFileThread(jsonArray);
                          writeToFileThread.start();
                          dialog.dismiss();
                      }
                  }
              })
              .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      dialog.dismiss();
                  }
              }).create();
      d.show();
  }

    private void showSaveDialog(){
       // if (btSaveData.isActivated()){

            int permission = ContextCompat.checkSelfPermission(DataActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                showDialog();
            }else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ActivityCompat.requestPermissions(DataActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                }
            }
      //  }
    }



    private void updateTextView(TextView tv, Number n,  String unit){
        String value = "%.1f";
        if (unit.contentEquals("a"))
            value = "%.2f";
        tv.setText(String.format( value, n.doubleValue() ) + unit);
    }
   /* public void showExtraInfo(View v){
        dialogFragment.show(getSupportFragmentManager(), "InfoFragment");
    }*/

    /**
     * List containing file names
     * @param file String- File path
     */
    private void updateFileList(String file){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.key_configs),Context.MODE_PRIVATE);
        Set<String> set = sharedPref.getStringSet(getString(R.string.key_filelist), new HashSet<String>());
        if (!set.contains(file)){
            set.add(file);
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(getString(R.string.key_filelist));
        editor.commit();
        editor.putStringSet(getString(R.string.key_filelist), set);
        editor.apply();
    }

    /**
     * Insert key and value into a global hashMap dataMapped
     * @param key String with
     * @param value String with value
     */
    private void insertIntoMap(String key, String value, Number time) {

        if (!key.contentEquals("co2abs") && !key.contentEquals("model")
                && !key.contentEquals("cellpress") /*&& !key.contentEquals("celltemp")*/
                && !key.contentEquals("lat") && !key.contentEquals("lon") && !key.contentEquals("alt")
                && !key.contentEquals("date") && !key.contentEquals("gpsTime") && !key.contentEquals("time")) {

            SimpleXYSeries serie = dataMapped.get(key);

            if (serie == null) {
                initSerie(key);
                serie = dataMapped.get(key);
                updateSpinner(key);
            }
            if ((!value.contains("NAN")) && (value.length() > 0)) {
                try {
                    Number n = Double.parseDouble(value);
                    if (n != null) {
                        Number n1 = 0;
                        if ((n == n1) && (serie.size() > 0))     //
                            n = serie.getX(serie.size() - 1);
                        serie.addLast(time, n);

                    }
                } catch (NumberFormatException e) {
                    serie.addLast(time, 0);
                } catch (NullPointerException e) {
                    serie.addLast(time, 0);
                }
            } else {
                Number n1 = 0;
                if (serie.size() > 0) {
                    n1 = serie.getX(serie.size() - 1);
                }
                serie.addLast(0, n1);
            }
        }
    }

    //[TAM:25.00,HAM:35.00,DIS:184,ANA:[A00|2.23_A01|1.85_A02|1.70_A03|1.50],LIC:[celltemp|5.1704649e1_cellpres|1.0111982e2_co2|4.1958174e2_co2abs|6.6353826e-2_ivolt|1.2219238e1_raw|3780083.3641255]]
    //initial divider1 = ',' divider2 = ':'
   /* private void customParser(String divider1, String divider2, String data){
        Log.v("current_data", data);
        if (data != null && data.length() >  0){
            String[] dataList = data.split(divider1);
            if (dataList != null){
                for (String pair : dataList) {
                    String[] key_val = pair.split(divider2);
                    if (key_val != null){
                        if (key_val.length == 2){
                            if ((key_val[1] != null) && (key_val[1].startsWith("["))){
                                //if start with '[' , remove brackets and parse _ and |
                                if (key_val[1].length() > 2){
                                    String valueList = key_val[1].substring(1, key_val[1].length() - 1);
                                    customParser("_", "\\|", valueList);
                                }
                            }else {
                                //Insert key and value
                                if ((key_val[0] != null) && key_val[1] != null)
                                    insertIntoMap(key_val[0], key_val[1]);
                            }
                        }
                    }
                }
            }
        }
    }*/


    private void jsonParser(JSONObject jObject, Number time){
       // if (data != null && data.length() >  0) {
            try {
                //JSONObject jObject = new JSONObject(data);
                Iterator<String> keys = jObject.keys();
                //JSONObject licorJson = jObject.getJSONObject("licor");
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (jObject.get(key) instanceof JSONObject) {
                        // do something with jsonObject here
                        JSONObject object = jObject.getJSONObject(key);
                        jsonParser(object, time);
                    } else {
                        String value = jObject.getString(key);
                        insertIntoMap(key, value, time);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
      // }
    }

    private void evaluateChunk(String current_data){

        String[] chunk = current_data.split("\n");
        int cursor = 0;

        for (int i = 0 ; i < chunk.length; i++){
            //Log.v("current_data", "initial: " + chunk[i]);
            JSONObject jObject = null;
            //String dataInPrint = chunk[i];
            try {
                 jObject = new JSONObject(chunk[i]);
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }
            Double time = 0.0;
            Double lat_json = 0.0;
            Double lng_json = 0.0;
            Double alt_json = 0.0;
            try {
                time = jObject.getDouble("time");
                lat_json = jObject.getDouble("lat");
                lng_json = jObject.getDouble("lon");
                alt_json = jObject.getDouble("alt");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            /*int initOfLineIndex =  chunk[i].indexOf("[");
            int endOfLineIndex = chunk[i].lastIndexOf("\r");*/
            //if ((initOfLineIndex == 0) && (endOfLineIndex == chunk[i].length() - 1)){
              // Log.v("current_data", "Well formed");

               // String dataInPrint = chunk[i].substring(initOfLineIndex + 1, endOfLineIndex - 1);
                // if toggle button is checked, then add data parsed to series for showing in Plot
                if (tgRecord.isChecked()) {

                    //customParser(",", ":", dataInPrint);
                                /*Fixed bug: Update zoomable range to current max X (visible)*/

                    jsonParser(jObject, time);
                    //plot.calculateMinMaxVals();
                    plot.redraw();
                    //TODO update current value widget
                    Number n = maxX + 1;

                    //Number number = plot.getBounds().getMaxX();

                    plot.getOuterLimits().set(0, n, 0, 50000);
                   // String locationText = "";

                    if (mLocation != null) {
                        if ((lat_json == 0) || (lng_json == 0) || (alt_json == 0)) {
                            try {
                                jObject.put("lat", mLocation.getLatitude());
                                jObject.put("lon", mLocation.getLongitude());
                                jObject.put("alt", mLocation.getAltitude());
                                jObject.put("acc", mLocation.getAccuracy());
                            }catch (JSONException e){
                                e.printStackTrace();
                            }

                            //String latitude = "" + mLocation.getLatitude();
                            //latitude = latitude.replace(",", ".");
                        }
                        try {
                            jObject.put("hour", new SimpleDateFormat("HH:mm:ss").format(new Date()));
                        }catch (JSONException e){
                            e.printStackTrace();
                        }

                       /* String longitude = "" + mLocation.getLongitude();
                        longitude = longitude.replace(",", ".");
                        String altitude = (String) String.format("%.2f",  mLocation.getAltitude());
                        altitude = altitude.replace(",", ".");
                        String accuracy = (String) String.format("%.2f",  mLocation.getAccuracy());
                        accuracy = accuracy.replace(",", ".");
                        locationText = String.format("Lat:%s,Long:%s,Alt:%s,Error:%s", latitude, longitude,altitude, accuracy);*/
                    }
                   // locationText = locationText.replace(",",".");
                   // dataToFile.append(new SimpleDateFormat("HH:mm:ss").format(new Date())+ " " + dataInPrint + /*","+ locationText +*/"\n");
                    jsonArray.put(jObject);
                    //dataToFile.append(jObject.toString());
                    updateInfoWidget();
                //    Log.v("DataActivity", dataInPrint);
                }
            /*}else{
                cursor = current_data.length() - chunk.length;
                //char x = current_data.charAt(cursor);
            }*/
        }
        int endOfLineIndex = current_data.lastIndexOf("\n");
        if (endOfLineIndex == (current_data.length() - 1)){ //if current data doesnt end with \n
            recDataString.setLength(0); //clear buffer
        }else{
            StringBuilder sb = new StringBuilder();
            String leftText = recDataString.substring(cursor, recDataString.length() -1);
            sb.append(leftText);
            recDataString = sb;
        }
    }


    /************************* THREADS *************************/

    /**
     * Thread that write a file to SDCard
     */
    private class WriteToFileThread extends Thread{

        private JSONObject main_json;
        //private String data;
        private File file;

        public WriteToFileThread(JSONArray array){

            main_json = new JSONObject();
            String pointName = getIntent().getStringExtra(getString(R.string.extra_point_name));
            if (pointName == null)
                pointName = "-";

            String item = spinner.getSelectedItem().toString();
            double slope = calculateSlope(item, minX, maxX);
            double correlation = calculatePearsonCorrelation(item, minX, maxX);
           /* String footer = "";
            if (item.contentEquals("co2"))
                footer = String.format("\nRango_%s:(%d - %d) | Pendiente(*K):%.5f | Coeficiente_Correlacion(R2):%.5f", item, minX, maxX, slope, correlation * correlation);
            else
                footer = String.format("\nRango_%s:(%d - %d) | Pendiente: %.5f | Coeficiente_Correlacion(R2):%.5f", item, minX, maxX, slope, correlation * correlation);*/

            try{
                main_json.put("campaing", campaingName);
                main_json.put("point", pointName);
                main_json.put("user", userName);
                main_json.put("starDate", startDate);
                main_json.put("samples", array);
                main_json.put("minX",minX);
                main_json.put("maxX",maxX);
                main_json.put("item", item);
                main_json.put("slope", slope);
                main_json.put("correlation", correlation * correlation);
            }catch (JSONException e){
                e.printStackTrace();
            }
            //data = _data;
            //data = campaingName + " - " + data;


          /*  if (mLocation != null){
                String locationText = String.format("Lat:%f | Long:%f | Alt:%.1f | Error:%.1f m", mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude(), mLocation.getAccuracy());
                locationText = locationText.replace(",",".");
                data = data.replace("|", ":");
               // data = data.replace("_", ",");

                data = campaingName + " | " + pointName + " | " + userName + " | " + startDate + "\n" + locationText+"\n" + data;
            }else*/
                //data = campaingName + " | " +  pointName + " | " + userName + " | " + startDate + "\n"+ data;

            String path = Environment.getExternalStorageDirectory() + File.separator  + "stationData";
            // Create the folder.
            File folder = new File(path);
            folder.mkdirs();
            // Create the file.
            //String date = new SimpleDateFormat("HH-mm-ss_dd-MM-yyyy").format(new Date());
            String fileName = startDate.replace(":","-");
            /**/

            //data = data + footer;
            /*double correlation2 = calculatePearsonCorrelation("A03", minX, maxX);

            String footer2 =  String.format("\nRango_%s:(%d - %d) | Pendiente: %.5f | Coeficiente_Correlacion(R2):%.5f", "A03", minX, maxX, calculateSlope("A03", minX, maxX), correlation2 * correlation2);
            data = data + footer2;
*           */
            String filePrefix = campaingName.replaceAll("\\s+",""); //Remove white spaces
            filePrefix = filePrefix + "_" + pointName + "_" + userName;
            file = new File(folder, filePrefix + "_" + fileName+".json");
        }

        @Override
        public void run(){
            // Save your stream, don't forget to flush() it before closing it.
            int status = 0;
            try{
                FileWriter writer = new FileWriter(file);
                writer.append(main_json.toString());
                writer.flush();
                writer.close();
                Log.d("DataActivity","File saved to "+file.getAbsolutePath());
                //Log.v("DataActivity", data);
                status = 1;
            }catch (IOException e){
                Log.e("Exception", "File write failed: " + e.toString());
            }
            Message msg = new Message();
            msg.what = status;
            msg.obj = file.getAbsolutePath();
            Log.d("File", file.getAbsolutePath());
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
            if (f.exists()){
                try {
                    con = new FTPClient();
                    con.connect(FTP_IP,FTP_PORT);
                    if (con.login(FTP_USER,FTP_PASS)){
                        con.enterLocalPassiveMode();
                        con.setFileType(FTP.BINARY_FILE_TYPE);
                        BufferedInputStream buffIn=null;
                        buffIn=new BufferedInputStream(new FileInputStream(f));
                       // FileInputStream in = new FileInputStream(f);
                        con.changeWorkingDirectory(WORKING_DIRECTORY);
                        result = con.storeFile(f.getName(),buffIn);
                        buffIn.close();
                        if (result)
                            Log.v("ftp", "Upload success");
                        con.logout();
                        con.disconnect();
                    }else
                        Log.e("ftp", "login error");
                }catch (IOException e){
                    //e.getCause().printStackTrace();
                    Log.e("ftp", e.getMessage());
                }
            }else
                Log.d("ftp","File not exists");
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

        private final String address;
        public StablishBluetoothConnection(String _address) {
            address = _address;
            progressDialogFragment.show(getSupportFragmentManager(), "progressDialog");
            if ((btSocket == null) || (!btSocket.isConnected())) {
                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                try {
                    btSocket = device.createInsecureRfcommSocketToServiceRecord(BTMODULEUUID);
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.error_bt_connection), Toast.LENGTH_LONG).show();
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
                        Log.e("Connect", "Error " + e2.getMessage());
                    }
                    finally {
                        //Toast.makeText(getApplicationContext(), "No se pudo establecer conexion", Toast.LENGTH_LONG).show();
                        //
                        result = false;
                    }
                }
                //Log.v("DataActivity", "Finishing bluetooth");
                }
            Message msg = new Message();
            if (result)
                msg.what = 1;
            else
                msg.what = 0;
            if (stablishBluetoothConnectionHandler != null)
                //stablishBluetoothConnectionHandler.sendMessageDelayed(msg,2000);
                stablishBluetoothConnectionHandler.sendMessage(msg);
        }

        public void flushSocket(){
            if (btSocket != null){
                try {
                    btSocket.getOutputStream().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    /**
     * Thread that contiuosly read data from a bluetooth socket
     */
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private boolean control_loop;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            control_loop = true;
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
            while (control_loop)
                try{
                    if (this.isInterrupted())
                        break;
                    int bytesAvailable = mmInStream.available();
                    if(bytesAvailable > 0){
                        byte[] buffer = new byte[bytesAvailable];
                        int bytes = mmInStream.read(buffer);
                        if (bytes != -1){
                            String readMessage = new String(buffer, 0, bytes);
                            if (!readMessage.isEmpty()){
                                if (bluetoothIn != null)
                                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                            }
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
                mmOutStream.flush();
                Log.v("LED",input);
            }catch (IOException e){
                Log.e("LED", "Error: " + e.getMessage());
            }
        }

        public void cancel(){
            try{
                control_loop = false;
                mmSocket.close();
            }catch (IOException e){

            }
        }

        public void setReadStatus(boolean status){
            this.control_loop = status;
        }
    }

    /*************  GPS methods -****************************/
    /**
     * Return the current state of the permissions needed.
     */

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(DataActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_FINE_LOCATION);
    }


    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mLocation = task.getResult();
                          /*  Log.v("Location", "" + mLocation.getLatitude() + " " + mLocation.getLongitude());
                            Toast.makeText(getApplicationContext(), "Location" + mLocation.getLatitude() + " " + mLocation.getLongitude() , Toast.LENGTH_LONG).show();*/
                        }
                    }
                });
    }

    @Override // Zeus
    public void onBackPressed() {
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage(getString(R.string.quit_data))
                .setNegativeButton(getString(R.string.No), null)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DataActivity.super.onBackPressed();
                    }
                }).create();
        d.show();
    }
}
