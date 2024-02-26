package io.github.ichisadashioko.android.kanji;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;

public class CourseGVAdapter extends ArrayAdapter<CourseModel> {

    public CourseGVAdapter(@NonNull Context context, ArrayList<CourseModel> courseModelArrayList) {
        super(context, 0, courseModelArrayList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listitemView = convertView;
        if (listitemView == null) {
            // Layout Inflater inflates each item to be displayed in GridView.
            listitemView = LayoutInflater.from(getContext()).inflate(R.layout.card_item, parent, false);
        }

        CourseModel courseModel = getItem(position);
        //TextView courseTV = listitemView.findViewById(R.id.idTVCourse);
        Button kanjiButton = listitemView.findViewById(R.id.idButtonCourse);
        //ImageView courseIV = listitemView.findViewById(R.id.idIVcourse);

        kanjiButton.setText(courseModel.getKanjiChar());
        //courseIV.setImageResource(courseModel.getImgid());
        return listitemView;
    }
}



// pass LOADED dictionary to goToGridView.. can you do that in postExecute????