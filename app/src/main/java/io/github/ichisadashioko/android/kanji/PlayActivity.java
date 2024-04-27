package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;

public class PlayActivity extends AppCompatActivity {
    public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE = 0;
    public int maxHappiness = 20;
    public Button dangoCount, mochiCount, taiyakiCount;
    public ImageView catSprite;
    public TextView catSpeech;
    public ProgressBar happiness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("In Play :D");
        // TODO this activity will crash if we change permission from the Settings app
        // https://stackoverflow.com/a/56765912/8364403
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play); //settings

        catSprite = findViewById(R.id.catSprite);
        dangoCount = findViewById(R.id.dangoCount);
        mochiCount = findViewById(R.id.mochiCount);
        catSpeech = findViewById(R.id.catSpeech);
        taiyakiCount = findViewById(R.id.taiyakiCount);
        happiness = findViewById(R.id.progressBar);
        happiness.setProgress(getInventoryTreat("Happiness"));

        dangoCount.setText("Dango x" + Integer.toString(getInventoryTreat("Dango")));
        mochiCount.setText("Mochi x" + Integer.toString(getInventoryTreat("Mochi")));
        taiyakiCount.setText("Taiyaki x" + Integer.toString(getInventoryTreat("Taiyaki")));
        System.out.println("set all counts");

        checkSprite();

    }

    public void goToMainPage(View view) {
        //
        System.out.println("Go To Main");
        Intent intent = new Intent();
        setResult(2, intent);
        finish();//finishing activity

    }

    public void checkSprite() {
        if(happiness.getProgress() > 5 && happiness.getProgress() <= 10) {
            catSprite.setImageResource(R.drawable.cat_happy);
        } else if(happiness.getProgress() > 10 && happiness.getProgress() <= 15) {
            catSprite.setImageResource(R.drawable.cat_v_happy);
        } else if(happiness.getProgress() > 15) {
            catSprite.setImageResource(R.drawable.cat_vv_happy);
        }
    }

    public void updateInventory(String treat, int increment) { //treat may be a treat or happiness
        try {
            BufferedReader file = new BufferedReader(new FileReader("/storage/emulated/0/Download/handwriting_data/inventory_file.txt"));
            String line;
            String input = ""; //string to store file contents

            while ((line = file.readLine()) != null) {
                input += line + '\n'; //add each line of the file to input string
            }

            String[] lines = input.split("\n");
            String lineToReplace = "";
            for(String element : lines) {
                if(element.split(" ")[0].equals(treat)) { //locates line with matching parameter
                    lineToReplace = element;
                }
            }

            String replaceWith = "";
            int noOfTreats = Integer.parseInt(lineToReplace.split(" ")[1]); //locates current number of treats/happiness
            if(noOfTreats == 0 && increment < 0) {
                return;
            }

            replaceWith = treat + " " + (noOfTreats + increment);
            input = input.replace(lineToReplace, replaceWith);

            FileOutputStream File = new FileOutputStream("/storage/emulated/0/Download/handwriting_data/inventory_file.txt");
            File.write(input.getBytes());

        } catch (Exception e) {
            System.out.println("Problem reading file.");
        }
    }
    //only different to the method in the play activity is that a zero check is performed. it's okay before since treats are only added in other activities, not subtracted

    public int getInventoryTreat(String treat) {
        try {
            BufferedReader file = new BufferedReader(new FileReader("/storage/emulated/0/Download/handwriting_data/inventory_file.txt"));
            String line;
            String input = ""; //string to store file contents

            while ((line = file.readLine()) != null) {
                input += line + '\n'; //add each line of the file to input string
            }

            String[] lines = input.split("\n");
            String lineToReturn = "";
            for(String element : lines) {
                if(element.split(" ")[0].equals(treat)) { //locates line with matching parameter
                    lineToReturn = element;
                }
            }
            int noOfTreats = Integer.parseInt(lineToReturn.split(" ")[1]); //locates current number of treats/happiness
            return noOfTreats;

        } catch (Exception e) {
            System.out.println("Problem reading file.");
        }
        return -1; // if an exception is caught
    }
    public void feedDango(View view) {
        if(getInventoryTreat("Dango") > 0 && getInventoryTreat("Happiness") < maxHappiness) {
            updateInventory("Dango", -1);
            dangoCount.setText("Dango x" + (getInventoryTreat("Dango")));
            updateInventory("Happiness", 1);
            happiness.setProgress(getInventoryTreat("Happiness"));
            checkSprite();
        } else if (getInventoryTreat("Happiness") >= maxHappiness) {
            catSpeech.setText("I'm full");
        }
    }

    public void feedMochi(View view) {
        if(getInventoryTreat("Mochi") > 0 && getInventoryTreat("Happiness") <= maxHappiness) {
            updateInventory("Mochi", -1);
            mochiCount.setText("Mochi x" + (getInventoryTreat("Mochi")));
            updateInventory("Happiness", 1);
            happiness.setProgress(getInventoryTreat("Happiness"));
            checkSprite();
            System.out.println("fed from inventory file");
        } else if (getInventoryTreat("Happiness") >= maxHappiness) {
            catSpeech.setText("I'm full");
        }
    }

    public void feedTaiyaki(View view) {
        if(getInventoryTreat("Taiyaki") > 0 && getInventoryTreat("Happiness") <= maxHappiness) {
            updateInventory("Taiyaki", -1);
            taiyakiCount.setText("Taiyaki x" + (getInventoryTreat("Taiyaki")));
            updateInventory("Happiness", 1);
            happiness.setProgress(getInventoryTreat("Happiness"));
            checkSprite();
        } else if (getInventoryTreat("Happiness") >= maxHappiness) {
            catSpeech.setText("I'm full");
        }
    }
}

//inventory passed correctly from Test
