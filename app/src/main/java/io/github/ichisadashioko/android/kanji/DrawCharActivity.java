package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


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
import io.github.ichisadashioko.android.kanji.views.Inventory;
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
    public TextView charInfo;
    public DictionaryExample dictExample;

    public HashMap<String, String> dict;
    public Inventory inventory;
    public String characterToDraw;
    public String charToSave = "";
    public String LABEL_FILE_PATH = "etlcb_9b_labels.txt"; //temporary, used to filter out kanji that don't match api call


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO this activity will crash if we change permission from the Settings app
        // https://stackoverflow.com/a/56765912/8364403
        super.onCreate(savedInstanceState);
        setContentView(R.layout.draw_char); //settings

        Intent intent = getIntent();
        dict = (HashMap<String, String>) intent.getSerializableExtra("hashMap");
        inventory = getIntent().getParcelableExtra("inventory");
        characterToDraw = intent.getStringExtra("kanjiToDraw");
        System.out.println("extra data: ");
        System.out.println("-----------Hash Map from Draw Char: ");
        int countElements = 0;
        for (String key: dict.keySet()) {
            //System.out.println(countElements + ") " + key + " : " + dict.get(key)); //countElements only used for printing
            countElements++;
        }
        System.out.println("--------------------------");
        System.out.println("to draw: " + characterToDraw);
        TextView kanjiButton = findViewById(R.id.charToDraw);

        kanjiButton.setText("Draw the character for: '" + dict.get(characterToDraw) + "' " + characterToDraw + " (6) times");


        ArrayList<String> databaseChars;

        try {
            databaseChars = loadLabelList(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for(int i = 0; i < databaseChars.size(); i++) {
            //System.out.println("label: " + databaseChars.get(i));
        }
        ArrayList<String> APIChars = new ArrayList<String>();
        for (String key: dict.keySet()) {
            APIChars.add(key);
        }
        System.out.println("Database size: " + databaseChars.size());
        System.out.println("APICalls size: " + APIChars.size());

        int numberOfMatchingKanji = 0;
        for(int i = 0; i < APIChars.size(); i++) {
            if(databaseChars.contains(APIChars.get(i))) {
                numberOfMatchingKanji++;
            } else {
                System.out.println("Missing: " + APIChars.get(i));
            }
        }
        System.out.println("Number of matching Kanji: " + numberOfMatchingKanji);


        charInfo = findViewById(R.id.countButton);
        charInfo.setText("Earn treats by drawing the character " + characterToDraw);

        canvas = findViewById(R.id.drawCharCanvas);
        resultContainer = findViewById(R.id.result_container); // from xml
        resultViewWidth = (int) getResources().getDimension(R.dimen.result_size);
        resultListScrollView = findViewById(R.id.result_container_draw_char);
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

        dictExample = new DictionaryExample(); //NOT the same as APICall dict
        dictExample.loadDict();
        System.out.println("dictionary return: " + dictExample.getFromKey("two"));

    }


    @Override
    public void onTouchEnd() { //starts a new thread if autoEvaluate is turned on
        //System.out.println("callback!");
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

    public synchronized void runClassifier(View view) throws IOException {
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
        //if(results.get(0).title.equals( dictExample.getFromKey("one"))) {

        System.out.println(results.get(0));
        System.out.println(characterToDraw);

        if(results.get(0).title.equals(characterToDraw)) { //aa i don't know if all the kanji from API are in the database from wrapper app
            count++; //increase count of number of evaluations so can count until 20 character draws
            System.out.println("Correct!!!!");
            inventory.addDango();
            inventory.printInventory();
            charInfo = findViewById(R.id.countButton);
            charInfo.setText("Correct!"); //reset count button to show increased number

            TextView kanjiButton = findViewById(R.id.charToDraw);
            kanjiButton.setText("Draw the character for: '" + dict.get(characterToDraw) + "' " + characterToDraw + " (" + (6 - count) + ") times");
        } else {
            charInfo.setText("This looks like " + results.get(0).title + " meaning '" + dict.get(results.get(0).title) + "'");
        }

        if(count == 6) { //temporarily, for demo
            count = 0;
            charToSave = characterToDraw; //character is learnt, will be saved in file
            charInfo = findViewById(R.id.countButton);
            charInfo.setText("Character " + characterToDraw +" meaning '" + dict.get(characterToDraw) + "' learnt!"); //reset count button to show increased number
            inventory.addMochi();
            //ADD TO CHARS TO TEST!!!

            //finish activity
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    Intent intent = new Intent();
                    intent.putExtra("inventory", inventory);
                    intent.putExtra("charDrawn", characterToDraw);
                    setResult(2, intent);
                    finish();//finishing activity
                }
            }, 3000);
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

    public ArrayList<String> loadLabelList(Activity activity) throws IOException {
        ArrayList<String> labels = new ArrayList<>();
        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(activity.getAssets().open(LABEL_FILE_PATH)));
        String line = reader.readLine();
        while (line != null) {
            labels.add(line);
            line = reader.readLine();
        }
        return labels;
    }

    public void goToMainPage(View view) {
        //
        System.out.println("Go To Main");

        Intent intent = new Intent();
        intent.putExtra("inventory", inventory);
        intent.putExtra("charDrawn", charToSave);
        System.out.println("charDrawn (from DrawChar): " + charToSave);
        System.out.println("inventory from draw char, being passed to GV");
        inventory.printInventory();

        setResult(2, intent);
        finish();//finishing activity

    }

}


//have deleted the isTextSaved boolean because isn't necessary when there is no auto evaluation