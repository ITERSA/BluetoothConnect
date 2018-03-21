package iter.bluetoothconnect;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class MyPosition{
    private Marker marker;
    private Circle circle;

    public MyPosition(Marker marker, Circle circle){
        this.marker = marker;
        this.circle = circle;

    }

    public void setPosition (LatLng position, float accuracy, double altitude){
        this.marker.setPosition(position);
        this.marker.setTitle("Altitude: "+ Math.round(altitude) +" m");

        if ((accuracy > 0) && (this.circle != null)){
            this.circle.setCenter(position);
            this.circle.setRadius(accuracy);
        }
    }

    public void setRotation(float degrees){
        this.marker.setRotation(degrees);
    }

    public long getCircleRadius(){
        return Math.round(this.circle.getRadius());
    }
}