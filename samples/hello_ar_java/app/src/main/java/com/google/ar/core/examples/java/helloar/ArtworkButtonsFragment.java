package com.google.ar.core.examples.java.helloar;

import androidx.fragment.app.Fragment;
import android.os.Bundle; // Added
import android.view.LayoutInflater; // Added
import android.view.View; // Added
import android.view.ViewGroup; // Added
import android.widget.LinearLayout; // Added
import android.widget.ImageButton; // Added

public class ArtworkButtonsFragment extends Fragment {

    public ArtworkButtonsFragment() {
        // Required empty public constructor
    }

    public static ArtworkButtonsFragment newInstance() {
        return new ArtworkButtonsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_artwork_buttons, container, false);

        ImageButton artworkButton1 = layout.findViewById(R.id.artwork_button_1);
        ImageButton artworkButton2 = layout.findViewById(R.id.artwork_button_2);
        ImageButton artworkButton3 = layout.findViewById(R.id.artwork_button_3);
        ImageButton artworkButton4 = layout.findViewById(R.id.artwork_button_4);

        View.OnClickListener artworkButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HelloArActivity activity = (HelloArActivity) getActivity();
                if (activity != null) {
                    switch (v.getId()) {
                        case R.id.artwork_button_1:
                            activity.setArtworkTexture(0);
                            break;
                        case R.id.artwork_button_2:
                            activity.setArtworkTexture(1);
                            break;
                        case R.id.artwork_button_3:
                            activity.setArtworkTexture(2);
                            break;
                        case R.id.artwork_button_4:
                            activity.setArtworkTexture(3);
                            break;
                    }
                }
            }
        };

        artworkButton1.setOnClickListener(artworkButtonClickListener);
        artworkButton2.setOnClickListener(artworkButtonClickListener);
        artworkButton3.setOnClickListener(artworkButtonClickListener);
        artworkButton4.setOnClickListener(artworkButtonClickListener);

        return layout;
    }
}

