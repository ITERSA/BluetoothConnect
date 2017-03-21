package iter.bluetoothconnect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aperez on 07/11/2016.
 */

public class DataManager {

    public static final String MAP_EST_GEOQUIMICAS = "Estaciones Geoquímicas";
    public static final String MAP_EST_GPS = "Estaciones de GPS";
    public static final String MAP_EST_SIS_INVOLCAN = "Estaciones Sísmicas de Involcan";
    public static final String MAP_EST_SIS_EPHESTOS = "Estaciones Sísmicas de Ephestos";
    public static final String MAP_EST_VIGILANCIA = "Torres de Vigilancia";
    public static final String MAP_CAM_DNE_2016 = "Campaña de la DNE 2016";

    public DataManager(){

    }
    public static List<String> dataToList(){
        List<String> list = new ArrayList<String>();
        list.add(MAP_EST_GEOQUIMICAS);
        list.add(MAP_EST_GPS);
        list.add(MAP_EST_SIS_INVOLCAN);
        list.add(MAP_EST_SIS_EPHESTOS);
        list.add(MAP_EST_VIGILANCIA);
        list.add(MAP_CAM_DNE_2016);
        return list;
    }

    public static String getKMLFileName(String tag){
        String ret ="";
        if (tag.equals(MAP_EST_GEOQUIMICAS)){
            ret = "estaciones_geoquimicas";
        }else if (tag.equals(MAP_EST_GPS)){
            ret = "estaciones_gps";
        }else if (tag.equals(MAP_EST_SIS_INVOLCAN)){
            ret = "estaciones_sismicas_involcan";
        }else if (tag.equals(MAP_EST_SIS_EPHESTOS)){
            ret = "estaciones_sismicas_ephestos";
        }else if (tag.equals(MAP_EST_VIGILANCIA)){
            ret = "torre_vigilancia";
        }else if (tag.equals(MAP_CAM_DNE_2016)){
            ret = "dne2016";
        }

        return ret;
    }
}
