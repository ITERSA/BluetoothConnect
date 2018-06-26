package iter.bluetoothconnect;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class AdapterPoint extends ArrayAdapter<Point> {

    private Context ctx;
    private ArrayList<Point> points;
    private static LayoutInflater inflater = null;

    public AdapterPoint(@NonNull Context context, int resource, ArrayList<Point> list) {
        super(context, resource, list);
        try {
            this.ctx = context;
            this.points = list;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }catch (Exception e){

        }
    }

    @Override
    public int getCount() {
        return points.size();
    }

    @Nullable
    @Override
    public Point getItem(int position) {
        return points.get(position);
    }

    public static class ViewHolder {
        public TextView display_name;
        public TextView display_distance;
    }

    public ArrayList<Point> getPoints() {
        return points;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        final ViewHolder holder;
        try{
            if (view == null){
                view = inflater.inflate(android.R.layout.two_line_list_item, null);
                holder = new ViewHolder();
                holder.display_name = (TextView) view.findViewById(android.R.id.text1);
                holder.display_distance = (TextView) view.findViewById(android.R.id.text2);
                view.setTag(holder);

            }else{
                holder = (ViewHolder) view.getTag();
            }

            holder.display_name.setText(points.get(position).getName());
            long distance = points.get(position).getDistance();
            String distanceText = "" + distance + " m";
            if (distance > 1000){
                double d = distance / 1000;
                DecimalFormat df = new DecimalFormat("#.#");
                df.setRoundingMode(RoundingMode.CEILING);
                distanceText = df.format(d) + " km";
            }
            holder.display_distance.setText(distanceText);
            int color = Color.RED;
            if (points.get(position).isDone())
                color = Color.GREEN;
            view.setBackgroundColor(color);
        }catch (Exception e){

        }
        //view.setTag(position);
        return view;
    }
}