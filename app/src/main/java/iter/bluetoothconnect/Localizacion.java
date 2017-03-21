package iter.bluetoothconnect;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * Created by aperez on 26/10/2016.
 */

/* Aqui empieza la Clase Localizacion */
public class Localizacion implements LocationListener {
    MapsActivity mainActivity;

    public MapsActivity getMainActivity() {
        return mainActivity;
    }

    public void setMainActivity(MapsActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    /*private void getLocation(){
        if (networkOn){
            Location l = locationManager.getLastKnownLocation(proveedor);
            if(l != null){
                StringBuilder builder =  new StringBuilder();
                builder.append("Mi ubicacion es: <Altitud:")
                        .append(l.getAltitude())
                        .append(",Longitud:")
                        .append(l.getLongitude())
                        .append(">");
                this.mainActivity.status.setText(builder.toString());

            }
        }
    }*/

    @Override
    public void onLocationChanged(Location loc) {
        // Este metodo se ejecuta cada vez que el GPS recibe nuevas coordenadas
        // debido a la deteccion de un cambio de ubicacion
        if(loc != null){
            loc.getLatitude();
            loc.getLongitude();
            StringBuilder builder =  new StringBuilder();
            builder.append("Mi ubicacion es: <Alti:")
                    .append(loc.getAltitude())
                    .append(",Long:")
                    .append(loc.getLongitude())
                    .append(",Alti:")
                    .append(loc.getAltitude())
                    .append(">");
            //this.mainActivity.status.setText(builder.toString());
            //this.mainActivity.setLocationOnMap(loc);


        }
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Este metodo se ejecuta cada vez que se detecta un cambio en el
        // status del proveedor de localizacion (GPS)
        // Los diferentes Status son:
        // OUT_OF_SERVICE -> Si el proveedor esta fuera de servicio
        // TEMPORARILY_UNAVAILABLE -> Temporalmente no disponible pero se
        // espera que este disponible en breve
        // AVAILABLE -> Disponible

    }

    @Override
    public void onProviderEnabled(String provider) {
        // Este metodo se ejecuta cuando el GPS es activado
        //this.mainActivity.status.setText("GPS Activado");
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Este metodo se ejecuta cuando el GPS es desactivado
        //this.mainActivity.status.setText("GPS Desactivado");
    }

}/* Fin de la clase localizacion */
