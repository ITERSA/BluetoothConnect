package iter.bluetoothconnect;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

public class CampaignActivity extends AppCompatActivity {

    private ListView listView;
    private TextView textView;
    private String currentMap = "";
    List<String> mApps = new ArrayList<String>();
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campaign);
        textView = (TextView) findViewById(R.id.campaign_title);

        //retrieveFileFromResource();
        mApps.addAll(DataManager.dataToList());

        listView = (ListView) findViewById(R.id.campaign_list);
        ListAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mApps);
        listView.setAdapter(adapter);

        listView.setTextFilterEnabled(true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try{

                    Object obj = listView.getItemAtPosition(position);
                    currentMap = DataManager.getKMLFileName(obj.toString());
                    showMessage(currentMap);

                } catch (Exception e){

                }
            }
        });

    }

    private void showMessage(String mapa) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(CampaignActivity.this);

        // Setting Dialog Title
        alertDialog.setTitle("Mapa de Campaña: ");

        // Setting Dialog Message
        alertDialog.setMessage("¿Desea cargar el mapa '" + mapa + "'?");

        // Setting Icon to Dialog
        alertDialog.setIcon(R.drawable.ic_action_place);



        // Setting Negative "NO" Button
        alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Write your code here to invoke NO event
                /*Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT).show();
                dialog.cancel();*/
            }
        });

        // Setting Positive "Yes" Button
        alertDialog.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                // Write your code here to invoke YES event
                Toast.makeText(getApplicationContext(), "Cargando mapa: " + currentMap, Toast.LENGTH_LONG).show();
                //finish();

                if(currentMap.length()>0){
                    Intent output = new Intent();
                    output.putExtra("currentMap", currentMap);
                    setResult(RESULT_OK, output);
                }

                finish();

            }
        });

        // Showing Alert Message
        alertDialog.show();
    }
}
