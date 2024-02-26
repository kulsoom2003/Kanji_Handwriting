package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.github.ichisadashioko.android.kanji.views.Inventory;

public class PlayActivity extends AppCompatActivity {
    public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE = 0;
    public TextView inventoryText;
    public Inventory inventory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("In Play :D");
        // TODO this activity will crash if we change permission from the Settings app
        // https://stackoverflow.com/a/56765912/8364403
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play); //settings

        inventory = getIntent().getParcelableExtra("inventory");

        inventoryText = findViewById(R.id.inventoryTextView);
        Button playButton = findViewById(R.id.playButton);
        inventoryText.setText(inventory.inventoryToString()); //reset count button to show increased number
        playButton.setBackgroundColor(Color.parseColor("#831B1B"));

        System.out.println("In Play");
        inventory.printInventory();

    }

    public void goToMainPage(View view) {
        //
        System.out.println("Go To Main");

        Intent intent = new Intent();
        intent.putExtra("inventory", inventory);
        setResult(2, intent);
        finish();//finishing activity

    }



}

//inventory passed correctly from Test
