package iter.bluetoothconnect;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by jfernandez on 03/11/2016.
 * Show values fields in a dialog
 */

public class ValuesFragment extends DialogFragment {

    private TextView tv1,tv2,tv3,tv4;
    public boolean shown = false;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.values_dialog, container, false);
        getDialog().setTitle("Valores");
        Button dismiss = (Button) rootView.findViewById(R.id.dismiss);
        tv1 = (TextView) rootView.findViewById(R.id.title1);
        tv2 = (TextView) rootView.findViewById(R.id.title2);
        tv3 = (TextView) rootView.findViewById(R.id.title3);
        tv4 = (TextView) rootView.findViewById(R.id.title4);
        dismiss.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return rootView;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        if (shown) return;

        super.show(manager, tag);
        shown = true;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        shown = false;
        super.onDismiss(dialog);
    }

    public void updateTV1(String st1){
        tv1.setText(st1);
    }
    public void updateTV2(String st2){
        tv2.setText(st2);
    }
    public void updateTV3(String st3){
        tv3.setText(st3);
    }
    public void updateTV4(String st4){
        tv4.setText(st4);
    }
}
