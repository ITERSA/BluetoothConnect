package iter.bluetoothconnect;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static iter.bluetoothconnect.DataActivity.FILE_LIST;

/**
 * Created by jfernandez on 17/11/2016.
 */

public class FtpFileUpload extends AsyncTask<Void,String,Set<String>> {

    private Set<String> set;
    private Context context;
    private ProgressDialog pd;

    public FtpFileUpload(Set<String> _set, Context ctx){
        set = _set;
        context = ctx;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pd = new ProgressDialog(context);
        pd.setTitle("Subiendo datos al FTP");
        pd.setMessage("Espere...");
        pd.setIndeterminate(false);
        pd.setMax(set.size());
        pd.setProgress(0);
        pd.setCancelable(false);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.show();
    }

    @Override
    protected Set<String> doInBackground(Void... params) {

        HashSet<String> failsSet = new HashSet<String>();

        FTPClient con = null;
        try {
            con = new FTPClient();
            con.connect(DataActivity.FTP_IP,DataActivity.FTP_PORT);
            if (con.login(DataActivity.FTP_USER,DataActivity.FTP_PASS)){
                con.enterLocalPassiveMode();
                con.setFileType(FTP.BINARY_FILE_TYPE);
                con.changeWorkingDirectory("/appflux");
                /**/
                for (String path : set){
                    boolean result = false;
                    String dir = Environment.getExternalStorageDirectory() + File.separator  + "stationData"+ File.separator+ path;
                    File f = new File(dir);
                    publishProgress(path);  //update progress
                    if (f.exists()){
                        BufferedInputStream buffIn = new BufferedInputStream(new FileInputStream(f));
                        try {
                            result = con.storeFile(f.getName(),buffIn);
                        }catch (IOException e){
                            e.printStackTrace();
                        }finally {
                            buffIn.close();
                        }
                    }else
                        result = true;
                    if (!result) {
                        Log.v("ftp", "Upload success");
                        failsSet.add(path);
                    }
                }
                ////
                con.logout();
                con.disconnect();
            }else
                return  set;
        }catch (IOException e){
            e.getCause().printStackTrace();
            Log.e("ftp", "Upload fails!!");
            return set;
        }
        return failsSet;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        pd.setProgress(pd.getProgress() + 1);
        pd.setMessage(values[0]);

    }

    @Override
    protected void onPostExecute(Set<String> set) {
        super.onPostExecute(set);
        SharedPreferences sharedPref = context.getSharedPreferences("Configs",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(FILE_LIST);
        editor.commit();
        editor.putStringSet(FILE_LIST, set);
        editor.apply();
        pd.dismiss();
        String text = "Envio de ficheros completados con exito";
        if (set.size() > 0){
            text = set.size()+" ficheros fallaron en el envio";
        }
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }
}
