package io.github.ichisadashioko.android.kanji;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

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
        Button meaningButton = listitemView.findViewById(R.id.meaning);
        int backgroundColor;


        kanjiButton.setText(courseModel.getKanjiChar());
        meaningButton.setText(courseModel.getKanjiChar() + " - ENG - " + courseModel.getGrade());

        switch (courseModel.getGrade()) {
            case(1) : backgroundColor = Color.parseColor("#FFF4E7E7"); break;
            case(2) : backgroundColor = Color.parseColor("#FFD89797"); break;
            case(3) : backgroundColor = Color.parseColor("#FFA84949"); break;
            case(4) : backgroundColor = Color.parseColor("#FF952929"); break;
            case(5) : backgroundColor = Color.parseColor("#FF800303"); break;
            case(6) : backgroundColor = Color.parseColor("#FF5C0101"); break;
            default: backgroundColor = Color.parseColor("#FF5C0101");
        }

        CardView card = listitemView.findViewById(R.id.card);
        card.setCardBackgroundColor(backgroundColor);



        //courseIV.setImageResource(courseModel.getImgid());
        return listitemView;
    }
}



// pass LOADED dictionary to goToGridView.. can you do that in postExecute????