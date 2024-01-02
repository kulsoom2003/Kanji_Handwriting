package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.List;

import io.github.ichisadashioko.android.kanji.tflite.KanjiClassifier;
import io.github.ichisadashioko.android.kanji.tflite.Recognition;
import io.github.ichisadashioko.android.kanji.views.BitmapView;
import io.github.ichisadashioko.android.kanji.views.CanvasPoint2D;
import io.github.ichisadashioko.android.kanji.views.HandwritingCanvas;
import io.github.ichisadashioko.android.kanji.views.ResultButton;
import io.github.ichisadashioko.android.kanji.views.TouchCallback;



public class DrawCharActivity extends Activity
        implements TouchCallback {
    public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE = 0;
    public static final String KANJI_FONT_PATH = "fonts/HGKyokashotai_Medium.ttf";
    public EditText textRenderer;

    public HandwritingCanvas canvas;
    public KanjiClassifier tflite;
    public HorizontalScrollView resultListScrollView;
    public LinearLayout resultContainer;
    public int resultViewWidth;
    public boolean autoEvaluate; //flag for evaluating image while it is drawn
    public boolean autoClear;
    public Bitmap currentEvaluatingImage;
    public BitmapView bitmapView;
    public List<List<CanvasPoint2D>> currentEvaluatingWritingStrokes;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO this activity will crash if we change permission from the Settings app
        // https://stackoverflow.com/a/56765912/8364403
        super.onCreate(savedInstanceState);
        setContentView(R.layout.draw_char); //settings

        canvas = findViewById(R.id.drawCharCanvas);
        resultContainer = findViewById(R.id.result_container_draw_char); // from xml
        resultViewWidth = (int) getResources().getDimension(R.dimen.result_size);
        resultListScrollView = findViewById(R.id.result_container_scroll_view_draw_char);
        bitmapView = findViewById(R.id.preview_bitmap_draw_char);
        canvas.touchCallback = this; //might be expecting a MainActivity here

        try {
            tflite = new KanjiClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // This is a Kanji handwriting font. It looks much better than the default font.
        /*
        Typeface kanjiTypeface =
                Typeface.createFromAsset(getApplicationContext().getAssets(), KANJI_FONT_PATH);
        textRenderer.setTypeface(kanjiTypeface);
        ResultButton.LABEL_FONT = kanjiTypeface;
*/

        autoEvaluate = false;
        autoClear = true;

        //PreferenceManager.getDefaultSharedPreferences(this)
        //        .registerOnSharedPreferenceChangeListener(this);

        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int hintTextAlphaValue = 30; //the background character's opacity in evaluation
        ResultButton.HINT_TEXT_ALPHA = hintTextAlphaValue;

        int writingStrokeWidth = 5; //default values because right now focus is not to add preferences
        HandwritingCanvas.WritingStrokeWidth = writingStrokeWidth;

    }


    @Override
    public void onTouchEnd() { //starts a new thread if autoEvaluate is turned on
        System.out.println("callback!");
        /*
        if (autoEvaluate) {
            long ts = SystemClock.elapsedRealtime();
            synchronized (InferenceThread.LastCreatedThreadTimeLock) {
                InferenceThread.LastCreatedThreadTime = ts;
            }

            // System.out.println("Thread " + ts + " started!");
            (new InferenceThread(this, ts)).start();
        }
        */
    }

    public synchronized void runClassifier(View view) {
        if (canvas == null || tflite == null || resultContainer == null) {
            return;
        }

        // long startTime = SystemClock.elapsedRealtime();
        currentEvaluatingWritingStrokes = canvas.writingStrokes;
        currentEvaluatingImage =
                RenderingUtils.renderImageFromStrokes(currentEvaluatingWritingStrokes);
        this.bitmapView.setBitmap(currentEvaluatingImage);
        // System.out.println("Number of strokes: " + currentEvaluatingWritingStrokes.size());
        List<Recognition> results = tflite.recognizeImage(currentEvaluatingImage);
        // long evaluateDuration = SystemClock.elapsedRealtime() - startTime;
        // System.out.println(String.format("Inference took %d ms.", evaluateDuration));

        if (resultContainer.getChildCount() > 0) {
            resultContainer.removeAllViews();
        }

        for (Recognition result : results) {
            resultContainer.addView(
                    createButtonFromResult(
                            result, currentEvaluatingImage, currentEvaluatingWritingStrokes));
        }

        // scroll the result list to the start
        resultListScrollView.scrollTo(0, 0);
    }


    public View createButtonFromResult(
            Recognition r, Bitmap image, List<List<CanvasPoint2D>> writingStrokes) {
        ResultButton btn = new ResultButton(this, null, r.title, r.confidence);
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(
                        resultViewWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        btn.setLayoutParams(layoutParams);
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pushText(r.title);
                        if (autoClear) {
                            clearCanvas(v);
                        }
                    }
                });
        return btn;
    }


    public void pushText(String text) {
        //isTextSaved = false; //I think this is useless for now
        textRenderer.setText(textRenderer.getText() + text);
        textRenderer.setSelection(textRenderer.getText().length());
    }

    public void clearCanvas(View view) {
        canvas.clearCanvas();
        bitmapView.setBitmap(null);
        currentEvaluatingImage = null;
        currentEvaluatingWritingStrokes = null;
        if (resultContainer.getChildCount() > 0) {
            resultContainer.removeAllViews();
        }
    }

}