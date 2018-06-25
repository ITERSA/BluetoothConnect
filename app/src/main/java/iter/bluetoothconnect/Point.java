package iter.bluetoothconnect;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class Point {
    protected Marker marker;
    protected String status;
    protected int altitude;
    protected long distance;

    public Point(Marker marker,  String status, float altitude) {
        this.marker = marker;
        this.status = status;
        this.altitude = Math.round(altitude);
    }

    public void setStatus(String current_status){
        status = current_status;
    }

    public boolean isDone(){
        return !status.contentEquals("");
    }

    public LatLng getLatlng(){
        return marker.getPosition();
    }

    public String getName(){
        return marker.getTitle();
    }

    public void setDistance(double distance){
        this.distance = Math.round(distance);
    }

    public long getDistance(){
        return this.distance;
    }
}
