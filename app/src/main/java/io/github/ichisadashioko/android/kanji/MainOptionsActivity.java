package io.github.ichisadashioko.android.kanji;

        import android.Manifest;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.os.Bundle;
        import android.view.View;

        import androidx.appcompat.app.AppCompatActivity;
        import androidx.core.app.ActivityCompat;
        import androidx.core.content.ContextCompat;

public class MainOptionsActivity extends AppCompatActivity {
    public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO this activity will crash if we change permission from the Settings app
        // https://stackoverflow.com/a/56765912/8364403
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_options); //settings

    }

    public void goToLearn(View view) {
        System.out.println("Clicked Learn");
    }

    public void goToTest(View view) {
        System.out.println("Clicked Test");
    }

    public void goToPlay(View view) {
        System.out.println("Clicked Play");
    }



}
