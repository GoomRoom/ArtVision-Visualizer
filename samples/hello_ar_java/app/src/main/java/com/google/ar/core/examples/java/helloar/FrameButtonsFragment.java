package com.google.ar.core.examples.java.helloar;

import androidx.fragment.app.Fragment;
import android.os.Bundle; // Added
import android.view.LayoutInflater; // Added
import android.view.View; // Added
import android.view.ViewGroup; // Added
import android.widget.LinearLayout; // Added
import android.widget.ImageButton; // Added

public class FrameButtonsFragment extends Fragment {

    public FrameButtonsFragment() {
        // Required empty public constructor
    }

    public static FrameButtonsFragment newInstance() {
        return new FrameButtonsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_frame_buttons, container, false);

        ImageButton frameButton1 = layout.findViewById(R.id.frame_button_1);
        //ImageButton frameButton2 = layout.findViewById(R.id.frame_button_2);
       // ImageButton frameButton3 = layout.findViewById(R.id.frame_button_3);
       // ImageButton frameButton4 = layout.findViewById(R.id.frame_button_4);

        View.OnClickListener frameButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HelloArActivity activity = (HelloArActivity) getActivity();
                if (activity != null) {
                    switch (v.getId()) {
                        case R.id.frame_button_1:
                            activity.setFrameModel(0);
                            break;
                       // case R.id.frame_button_2:
                            //activity.setFrameModel(1);
                            //break;
                        //case R.id.frame_button_3:
                            //activity.setFrameModel(2);
                           // break;
                       // case R.id.frame_button_4:
                            //activity.setFrameModel(3);
                            //break;
                    }
                }
            }
        };

        frameButton1.setOnClickListener(frameButtonClickListener);
        //frameButton2.setOnClickListener(frameButtonClickListener);
        //frameButton3.setOnClickListener(frameButtonClickListener);
        //frameButton4.setOnClickListener(frameButtonClickListener);

        return layout;
    }
}

