 
package com.pavol.svancarek;
 
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
 
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
 
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
 
public class BottomSheet extends BottomSheetDialogFragment {
 
    private LatLng position;
 
    public BottomSheet(LatLng position) {
        this.position = position;
    }
 
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet, container, true);
    }
 
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
 
        TextView tw = view.findViewById(R.id.text);
        tw.setText("LAT: " + position.latitude + " - " + position.longitude);
    }
}
 