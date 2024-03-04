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

import io.github.ichisadashioko.android.kanji.views.Inventory;

public class PlayActivity extends AppCompatActivity {
    public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE = 0;
    public Button dangoCount, mochiCount, taiyakiCount;
    public Inventory inventory;
    public ImageView catSprite;
    public ProgressBar happiness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("In Play :D");
        // TODO this activity will crash if we change permission from the Settings app
        // https://stackoverflow.com/a/56765912/8364403
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play); //settings

        inventory = getIntent().getParcelableExtra("inventory");
        catSprite = findViewById(R.id.catSprite);
        System.out.println("inventory from Play");
        inventory.printInventory();

        dangoCount = findViewById(R.id.dangoCount);
        mochiCount = findViewById(R.id.mochiCount);
        taiyakiCount = findViewById(R.id.taiyakiCount);
        happiness = findViewById(R.id.progressBar);
        happiness.setProgress(0);

        dangoCount.setText("Dango x" + Integer.toString(inventory.getDango()));
        mochiCount.setText("Mochi x" + Integer.toString(inventory.getMochi()));
        taiyakiCount.setText("Taiyaki x" + Integer.toString(inventory.getTaiyaki()));
        //inventoryText.setText(inventory.inventoryToString()); //reset count button to show increased number
        System.out.println("set all counts");

        checkSprite();


    }

    public void goToMainPage(View view) {
        //
        System.out.println("Go To Main");
        inventory.printInventory();

        Intent intent = new Intent();
        intent.putExtra("inventory", inventory);
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

    public void feedDango(View view) {

        if (inventory.getDango() > 0) {
            dangoCount.setText("Dango x" + (inventory.getDango() - 1));
            inventory.minusDango();
            happiness.setProgress(happiness.getProgress() + 1);
            checkSprite();
        }
    }

    public void feedMochi(View view) {

        if (inventory.getMochi() > 0) {
            mochiCount.setText("Mochi x" + (inventory.getMochi() - 1));
            inventory.minusMochi();
            happiness.setProgress(happiness.getProgress() + 1);
            checkSprite();
        }
    }

    public void feedTaiyaki(View view) {

        if (inventory.getTaiyaki() > 0) {
            taiyakiCount.setText("Taiyaki x" + (inventory.getTaiyaki() - 1));
            inventory.minusTaiyaki();
            happiness.setProgress(happiness.getProgress() + 1);
            checkSprite();
        }
    }



}

//inventory passed correctly from Test
