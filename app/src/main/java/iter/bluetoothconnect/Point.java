package iter.bluetoothconnect;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class Point {
    protected Marker marker;
    protected Circle circle;
    protected String status;
    protected int altitude;
    protected long distance;

    public Point(Marker marker, Circle circle, String status, float altitude) {
        this.marker = marker;
        this.circle = circle;
        this.status = status;
        this.altitude = Math.round(altitude);
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

    public void showCirle(boolean state){
        if (circle != null){
            circle.setVisible(state);
        }
    }

    private void changeCircleColor(){

    }

    public long getCircleRadius(){
        return Math.round(this.circle.getRadius());
    }

    public void setDistance(double distance){
        this.distance = Math.round(distance);
    }

    public long getDistance(){
        return this.distance;
    }

}
