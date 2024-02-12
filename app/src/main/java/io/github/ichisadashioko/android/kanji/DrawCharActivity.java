package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Button;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
    public static final String SAVE_DIRECTORY_NAME = "handwriting_data";
    public static final String WRITING_LOG_DIR_NAME = "writing_history";
    public EditText textRenderer;
    public static final int MAX_LOG_SIZE = 5 * 1024;
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

    public int count = 0;
    public boolean isTextSaved = true;
    public Button button;
    public DictionaryExample dict;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO this activity will crash if we change permission from the Settings app
        // https://stackoverflow.com/a/56765912/8364403
        super.onCreate(savedInstanceState);
        setContentView(R.layout.draw_char); //settings
        button = findViewById(R.id.countButton);
        button.setText(Integer.toString(count));

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

        dict = new DictionaryExample();
        dict.loadDict();
        System.out.println("dictionary return: " + dict.getFromKey("two"));

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

        //if the character drawn was 一 then increase the counter
        if(results.get(0).title.equals( dict.getFromKey("one"))) {
            count++; //increase count of number of evaluations so can count until 20 character draws
            button = findViewById(R.id.countButton);
            button.setText(Integer.toString(count)); //reset count button to show increased number
        }

        if(count == 21) {
            count = 0;
            button = findViewById(R.id.countButton);
            button.setText(Integer.toString(count)); //reset count button to show increased number
        } //can replace this with a mod function. also make a 'resetCount' function


        System.out.println("draw char, Kanji is probably: " + results.get(0).title);
        //here is where you should export/save the kanji to the file
        //pushText(results.get(0).title);
        saveWritingHistory(results.get(0).title + " label: evaluated <- drawChar");

        clearCanvas(view); //will keep this afterwards, it's useful to see results for now

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
                        if (autoClear) {
                            clearCanvas(v);
                        }
                    }
                });
        return btn;
    }

    public void saveWritingHistory(final String text) { //saves contents of first typing bar in a file. Updates the same file.
        System.out.println("text to save: " + text);

        if (text.isEmpty()) {
            return;
        }

        try {


            System.out.println("Trying to save writing history");

            File downloadDirectory =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String rootSavePath = downloadDirectory.getAbsolutePath() + "/" + SAVE_DIRECTORY_NAME;
            rootSavePath = rootSavePath.replaceAll("/+", "/");
            if (!prepareDirectory(rootSavePath)) {
                throw new Exception(String.format("Cannot create directory: %s", rootSavePath));
            }

            String writingHistoryDirectoryPath = rootSavePath + "/" + WRITING_LOG_DIR_NAME;
            if (!prepareDirectory(writingHistoryDirectoryPath)) {
                throw new Exception(
                        String.format("Cannot create directory: %s", writingHistoryDirectoryPath));
            }

            int indexCounter = 0;
            String saveFilePath;
            File saveFile;

            do {
                saveFilePath =
                        writingHistoryDirectoryPath + "/" + String.format("%06d.txt", indexCounter);
                saveFilePath = saveFilePath.replace("/+", "/");
                saveFile = new File(saveFilePath);

                if (!saveFile.exists()) {
                    saveFile.createNewFile();
                } else {
                    if (saveFile.isDirectory()) {
                        // backup this directory to take over the file name
                        String backupPath = getBackupFilepath(saveFilePath);
                        saveFile.renameTo(new File(backupPath));
                        saveFile = new File(saveFilePath);
                        saveFile.createNewFile();
                    }
                }

                indexCounter++;
            } while (saveFile.length() > MAX_LOG_SIZE);

            OutputStreamWriter osw =
                    new OutputStreamWriter(new FileOutputStream(saveFile, true), "utf8");
            osw.append(text);
            osw.append('\n');
            osw.flush();
            osw.close();
            isTextSaved = true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean prepareDirectory(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.isFile()) {
                String backupFilePath = getBackupFilepath(path);
                file.renameTo(new File(backupFilePath));
                file = new File(path);
            } else {
                return true;
            }
        }

        return file.mkdirs();
    }

    public String getBackupFilepath(String path) {
        int counter = 0;
        String backupFilepath = path + "_" + counter;
        File backupFile = new File(path);
        while (backupFile.exists()) {
            backupFilepath = path + "_" + counter;
            backupFile = new File(backupFilepath);
            counter++;
        }
        return backupFilepath;
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


//have deleted the isTextSaved boolean because isn't necessary when there is no auto evaluation