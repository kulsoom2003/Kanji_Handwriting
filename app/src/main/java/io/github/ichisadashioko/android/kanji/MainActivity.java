package io.github.ichisadashioko.android.kanji;
/*
* res/drawable has all the icons in it
*
*
*
*
*
* */

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.JsonWriter;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;

import io.github.ichisadashioko.android.kanji.tflite.KanjiClassifier;
import io.github.ichisadashioko.android.kanji.tflite.Recognition;
import io.github.ichisadashioko.android.kanji.views.BitmapView;
import io.github.ichisadashioko.android.kanji.views.CanvasPoint2D;
import io.github.ichisadashioko.android.kanji.views.HandwritingCanvas;
import io.github.ichisadashioko.android.kanji.views.ResultButton;
import io.github.ichisadashioko.android.kanji.views.TouchCallback;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import java.io.FileNotFoundException;  // Import this class to handle errors
import java.util.Scanner;

public class MainActivity extends Activity
        implements TouchCallback, SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     * We still have to put custom font in `assets` folder but not the `res` folder because
     * accessing font via `id` requires minimum API 26.
     */
    public static final String KANJI_FONT_PATH = "fonts/HGKyokashotai_Medium.ttf";

    /**
     * Writing data will be stored in the Downloads directory.
     *
     * <p>~/Download/handwriting_data/
     */
    public static final String SAVE_DIRECTORY_NAME = "handwriting_data";

    // ~/Download/handwriting_data/stroke_data/
    public static final String WRITING_STROKE_DATA_DIR_NAME = "stroke_data";

    // ~/Download/handwriting_data/writing_history/
    public static final String WRITING_LOG_DIR_NAME = "writing_history";
    public static final String WRITING_LOG_DIR_NAME_NEW_FILE = "learnt_characters"; //has chars, date last learnt, no. of times tested (test in priority for SRS)
    public static final String LEARNT_CHARS_FILE = "learnt_characters_file.txt";
    public static final String INVENTORY_FILE = "inventory_file.txt";
    /**
     * 5 KBs for text file.
     *
     * <p>Each Japanese character takes up around 3 bytes. Each sentence (line) is about 16
     * characters. 5 KBs will give us 100 lines for each file.
     */
    public static final int MAX_LOG_SIZE = 5 * 1024;

    //public HandwritingCanvas canvas;
    public KanjiClassifier tflite;

    /** I keep track of this view to scroll to start when we populate the result list. */
    //public HorizontalScrollView resultListScrollView;

    //public LinearLayout resultContainer;

    /**
     * This variable is used to store the pixel value converted from dp value stored in dimens.xml.
     * I use this value to set the size for the result view.
     */
    //public int resultViewWidth;

    // The EditText is used to store the input text.
    //public EditText textRenderer;

    /** flags for clearing the canvas or evaluating the image data while it's being drawn. */
    public boolean autoEvaluate;

    public boolean autoClear;

    /**
     * Sometimes, we want to store data that the model was not trained or the model give the correct
     * label to low accuracy point that the correct label does not show in the result list. We have
     * to manually type the correct label and save it ourselves for future training.
     */
    //public EditText customLabelEditText;

    /**
     * Variables to keep track of the data that we are currently seeing. I need these to store
     * custom labels that the model does not have or the model evaluates the data wrong (not showing
     * in the result list).
     */
    //public Bitmap currentEvaluatingImage;

    //public List<List<CanvasPoint2D>> currentEvaluatingWritingStrokes;

    // I set this to `true` because the text is empty.
    public boolean isTextSaved = true;

    //public BitmapView bitmapView;
    public HashMap<String, String> dict; //dictionary
    public HashMap<String, Integer> grades; //maps kanji to grade
    public HashMap<String, String> radicals; //maps kanji to grade
    public GridView kanjiGV;
    public String charDrawn;
    public HashMap<String, String>  charsLearnt; // format: character, no.tested date



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dict = new HashMap<String, String>();
        grades = new HashMap<String, Integer>();
        createEmptyInventory(SAVE_DIRECTORY_NAME, INVENTORY_FILE);

        try {
            new APICallsFromClass().execute().get(); //fill the dictionary
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // I add a TouchCallback interface because if we override the event listener,
        // the canvas is not working correctly. Our custom canvas manually handle touch
        // events, because of that add EventListener may break out canvas functionality.

        //canvas.touchCallback = this;

        try {
            tflite = new KanjiClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        // This is a Kanji handwriting font. It looks much better than the default font.
        Typeface kanjiTypeface =
                Typeface.createFromAsset(getApplicationContext().getAssets(), KANJI_FONT_PATH);
        textRenderer.setTypeface(kanjiTypeface);
        ResultButton.LABEL_FONT = kanjiTypeface;

        autoEvaluate = isAutoEvaluateEnabled();
        autoClear = isAutoClearEnabled();

         */

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int hintTextAlphaValue = -1; //the background character's opacity in evaluation
        try {
            hintTextAlphaValue =
                    Integer.parseInt(
                            sharedPreferences.getString(
                                    getString(R.string.pref_key_hint_text_type_alpha),
                                    Integer.toString(ResultButton.DEFAULT_HINT_TEXT_ALPHA)));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.err.println("Failed to get hint text alpha value!");
        }

        if (hintTextAlphaValue < 0) {
            ResultButton.HINT_TEXT_ALPHA = 0;
        } else if (hintTextAlphaValue > 255) {
            ResultButton.HINT_TEXT_ALPHA = 255;
        } else {
            ResultButton.HINT_TEXT_ALPHA = hintTextAlphaValue;
        }

        int writingStrokeWidth = -1;

        try {
            writingStrokeWidth =
                    Integer.parseInt(
                            sharedPreferences.getString(
                                    getString(R.string.pref_key_stroke_width), "5"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.err.println("Failed to get writing stroke width value from preferences!");
        }

        if (writingStrokeWidth < 1) {
            HandwritingCanvas.WritingStrokeWidth = 5;
        } else {
            HandwritingCanvas.WritingStrokeWidth = writingStrokeWidth;
        }


        /*
        updateInventory("Dango", 10);
        updateInventory("Mochi", 5);
        updateInventory("Taiyaki", 3);
        updateInventory("Happiness", 2);


        appendFile("夕 17 2024-02-10", SAVE_DIRECTORY_NAME, WRITING_LOG_DIR_NAME_NEW_FILE, LEARNT_CHARS_FILE);
        appendFile("士 15 2024-02-27", SAVE_DIRECTORY_NAME, WRITING_LOG_DIR_NAME_NEW_FILE, LEARNT_CHARS_FILE);
        appendFile("天 8 2024-03-02", SAVE_DIRECTORY_NAME, WRITING_LOG_DIR_NAME_NEW_FILE, LEARNT_CHARS_FILE);
        appendFile("入 16 2024-01-01", SAVE_DIRECTORY_NAME, WRITING_LOG_DIR_NAME_NEW_FILE, LEARNT_CHARS_FILE);
        System.out.println("saved in new file");
        */

    }

    /**
     * Check if the saving data preference is turned on and if we have permission to write to
     * external storage.
     */
    public boolean canSaveWritingData() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAllowSaving =
                sharedPreferences.getBoolean(getString(R.string.pref_key_save_data), false);
        boolean permissionGranted =
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED);
        return isAllowSaving && permissionGranted;
    }

    public boolean isAutoEvaluateEnabled() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean(
                getString(R.string.pref_key_auto_evaluate_input), false);
    }

    public boolean isAutoClearEnabled() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean(getString(R.string.pref_key_auto_clear_canvas), false);
    }

    /**
     * The location we want to use to save data has been already taken by some files/directories.
     * Rename them to back them up.
     *
     * @param path the taken file path
     * @return the available file path can be used for renaming this path
     */
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

    /**
     * Escape all invalid file name characters so that we can save the data with this file name. All
     * invalid characters are replace with underscore ('_').
     *
     * @param filename the string that may become a file name
     * @return the valid string for a file name
     */
    public String normalizeFilename(String filename) {
        return filename.replaceAll("[\\\\\\/\\.\\#\\%\\$\\!\\@\\(\\)\\[\\]\\s]+", "_");
    }

    /**
     * Save writing data to external storage for collection data later to train another model.
     *
     * @param label the text representation of the input
     * @param confidence the confidence score range from 0 to 1
     * @param timestamp the time that this data is created
     * @param image the drawing image of `label`
     * @param writingStrokes list of strokes that create the writing
     */
    public synchronized void exportWritingData( //creates a json file with each kanji drawn & other info about it
            String label,
            final float confidence,
            final long timestamp,
            Bitmap image,
            List<List<CanvasPoint2D>> writingStrokes) {
        long startTime = SystemClock.elapsedRealtime();
        // TODO create preference for save location
        try {
            // TODO the image format is ARGB, write our own encoder to encode grayscale PNG
            // Android does not support encoding grayscale PNG from Bitmap.
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
            byte[] grayscalePNGImage = byteArrayOutputStream.toByteArray();
            String base64Image = Base64.encodeToString(grayscalePNGImage, Base64.DEFAULT);
            // convert the base64 string to array of string to shorten the line length in json file.
            String[] wrappedBase64String = base64Image.split("\n");
            // System.out.println(wrappedBase64String.length);

            File downloadDirectory =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String savePath =
                    downloadDirectory.getAbsolutePath()
                            + "/"
                            + SAVE_DIRECTORY_NAME
                            + "/"
                            + WRITING_STROKE_DATA_DIR_NAME;
            // normalize path separators
            savePath = savePath.replaceAll("/+", "/");
            if (!prepareDirectory(savePath)) {
                throw new Exception(String.format("Cannot create directory: %s", savePath));
            }

            // make directory for the label
            label = (label == null || label.isEmpty()) ? "NO_LABEL" : label;
            String labelDirName = normalizeFilename(label);
            String labelDirPath = savePath + "/" + labelDirName;
            labelDirPath = labelDirPath.replaceAll("/+", "/");
            if (!prepareDirectory(labelDirPath)) {
                throw new Exception(String.format("Cannot create directory: %s", labelDirPath));
            }

            String dataFilePath = labelDirPath + "/" + Long.toString(timestamp) + ".json";
            FileOutputStream out = new FileOutputStream(dataFilePath);
            // serialize to JSON format
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("label").value(label);
            writer.name("timestamp").value(timestamp);
            writer.name("confidence").value(confidence);

            writer.name("touches");
            writer.beginArray();
            Iterator<List<CanvasPoint2D>> iterator = writingStrokes.iterator();
            while (iterator.hasNext()) {
                List<CanvasPoint2D> stroke = iterator.next();
                writer.beginArray();
                Iterator<CanvasPoint2D> strokeIterator = stroke.iterator();
                while (strokeIterator.hasNext()) {
                    CanvasPoint2D p = strokeIterator.next();
                    writer.beginObject();
                    writer.name("x").value(p.x);
                    writer.name("y").value(p.y);
                    writer.endObject();
                }
                writer.endArray();
            }
            writer.endArray();

            writer.name("image");
            writer.beginObject();
            writer.name("description")
                    .value("PNG image in ARGB format even though this is just a grayscale image.");
            writer.name("data");
            writer.beginArray();
            for (String base64Data : wrappedBase64String) {
                writer.value(base64Data);
            }
            writer.endArray();
            writer.endObject();

            writer.endObject();
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        long exportDuration = SystemClock.elapsedRealtime() - startTime;
        System.out.println("Export time: " + exportDuration);
    }

    /**
     * We want to make sure there is a directory with path equals `path`. However, there may be it
     * is not existed or it is a file. We will `mkdirs` or `rename` respectively. The `path` must be
     * valid.
     *
     * @param path the directory path we want
     */
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

    /*
    public void pushText(String text) { //sets content of first typing bar to text given
        isTextSaved = false;
        textRenderer.setText(textRenderer.getText() + text);
        textRenderer.setSelection(textRenderer.getText().length());
    }

     */

    /*
     * Create a view to show the recognition in the UI. I also setup event listener for each view in
     * order to add text if the view is clicked.
     *
     * <p>I also plan to save the image/drawing stroke to file so that I can improve the model
     * later.
     *
     * @param r the recognition result
     * @param image the image associated with the result
     * @param writingStrokes list of touch points
     * @return
     */

    /*
    public View createButtonFromResult(
            Recognition r, Bitmap image, List<List<CanvasPoint2D>> writingStrokes) {
        ResultButton btn = new ResultButton(this, null, r.title, r.confidence);
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(
                        resultViewWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        //System.out.println("layout params toString: " + layoutParams.toString());
        //System.out.println("confidence: " + btn.confidenceToString()); //printed for every kanji in result list because method is called in looping through results
        btn.setLayoutParams(layoutParams);
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        System.out.println("Result button clicked");

                        pushText(r.title); //puts the kanji clicked in the first typing bar
                        if (canSaveWritingData()) {
                            // save image and writing strokes to files
                            exportWritingData(
                                    r.title, r.confidence, r.timestamp, image, writingStrokes);
                        }
                        if (autoClear) {
                            clearCanvas(v);
                        }
                    }
                });
        return btn;
    }

     */

    /*
     * Get the image from the canvas and use the tflite model to evaluate the image. After the
     * results are returned, show them on the UI.
     *
     * @param view the View that triggers this method.
     */

    /*
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

        System.out.println("Kanji is probably: " + results.get(0).title);
        //here is where you should export/save the kanji to the file
        pushText(results.get(0).title);
        saveWritingHistory(results.get(0).title + " label: evaluated <- MainAct", SAVE_DIRECTORY_NAME, WRITING_LOG_DIR_NAME);

        // scroll the result list to the start
        resultListScrollView.scrollTo(0, 0);
    }

     */

    //make a method to add to the inventory

    public void createEmptyInventory(String saveDirectoryName, String fileName) {
        try {

            File downloadDirectory =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); //Environment imported library, directory_downloads property of environment
            String rootSavePath = downloadDirectory.getAbsolutePath() + "/" + saveDirectoryName; //parameter
            rootSavePath = rootSavePath.replaceAll("/+", "/");
            if (!prepareDirectory(rootSavePath)) { //creates directory for rootSavePath
                throw new Exception(String.format("Cannot create directory: %s", rootSavePath));
            }

            System.out.println("Created directory: " + rootSavePath);

            String saveFilePath;
            File saveFile;

            do {

                saveFilePath = rootSavePath + "/" + fileName;
                saveFilePath = saveFilePath.replace("/+", "/");
                saveFile = new File(saveFilePath);
                System.out.println("saveFilePath: " + saveFilePath);


                if (!saveFile.exists()) {
                    saveFile.createNewFile();
                } else {
                    return; //does not recreate existing inventory
                }

            } while (saveFile.length() > MAX_LOG_SIZE);

            OutputStreamWriter osw =
                    new OutputStreamWriter(new FileOutputStream(saveFile, true), "utf8");
            osw.append("Dango 0\nMochi 0\nTaiyaki 0\nHappiness 0\n"); //treats set to zero
            osw.flush();
            osw.close();
            isTextSaved = true;
        } catch (Exception ex) {
            ex.printStackTrace();
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
            replaceWith = treat + " " + (noOfTreats + increment);
            input = input.replace(lineToReplace, replaceWith);

            FileOutputStream File = new FileOutputStream("/storage/emulated/0/Download/handwriting_data/inventory_file.txt");
            File.write(input.getBytes());

        } catch (Exception e) {
            System.out.println("Problem reading file.");
        }
    }

    public void appendFile(String text, String saveDirectoryName, String redundant, String fileName) { //saves contents of first typing bar in a file. Updates the same file.
        System.out.println("text to save: " + text);

        //System.out.println("Symbol in line:" +  text.split(" ")[0]);
        //System.out.println(text.split(" ")[0].length());


        //if (isTextSaved || text.isEmpty()) {
        // check that line is in the right format
        if(text.isEmpty()) {
            System.out.println("empty text");
            return;
        } else if(text.split(" ")[0].length() != 1) { // check that line is in the right format
            System.out.println("Symbol in line:" +  text.split(" ")[0]);
            return;
        }

        /*
        if (text.isEmpty() || text.split(" ")[0].length() != 1) {
            System.out.println("isTextSaved: " + isTextSaved);
            System.out.println("Symbol in line:" +  text.split(" ")[0]);
            return;
        }

         */

        try {
            if (!canSaveWritingData()) { //checks permission from settings in the app
                return;
            }

            System.out.println("Trying to save writing history");

            File downloadDirectory =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); //Environment imported library, directory_downloads property of environment
            String rootSavePath = downloadDirectory.getAbsolutePath() + "/" + saveDirectoryName; //parameter
            rootSavePath = rootSavePath.replaceAll("/+", "/");
            if (!prepareDirectory(rootSavePath)) { //creates directory for rootSavePath
                throw new Exception(String.format("Cannot create directory: %s", rootSavePath));
            }

            /*
            String writingHistoryDirectoryPath = rootSavePath + "/" + writingLogDirName; // file path to save file in
            if (!prepareDirectory(writingHistoryDirectoryPath)) {
                throw new Exception(
                        String.format("Cannot create directory: %s", writingHistoryDirectoryPath));
            }
            */
            //above creates directory to save file in
            System.out.println("Created directory: " + rootSavePath);

            int indexCounter = 0;
            String saveFilePath;
            File saveFile;

            do {

                /*
                if(writingLogDirName.equals("learnt_characters")) {
                    saveFilePath = writingHistoryDirectoryPath + "/learnt_characters_file.txt";
                } else if {
                    saveFilePath = writingHistoryDirectoryPath + "/" + String.format("%06d.txt", indexCounter);
                }

                 */

                saveFilePath = rootSavePath + "/" + fileName;
                saveFilePath = saveFilePath.replace("/+", "/");
                saveFile = new File(saveFilePath);
                System.out.println("saveFilePath: " + saveFilePath);

                if (!saveFile.exists()) {
                    saveFile.createNewFile();
                    System.out.println("Creating new file");

                } else { //file already exists

                    if (saveFile.isDirectory()) {
                        // backup this directory to take over the file name
                        String backupPath = getBackupFilepath(saveFilePath);
                        saveFile.renameTo(new File(backupPath));
                        saveFile = new File(saveFilePath);
                        saveFile.createNewFile();
                    }
                }
                System.out.println("Incrementing index counter");
                indexCounter++; //isn't updated systematically but doesn't really matter
            } while (saveFile.length() > MAX_LOG_SIZE);

            OutputStreamWriter osw =
                    new OutputStreamWriter(new FileOutputStream(saveFile, true), "utf8"); //writes to file
            System.out.println("written to file");
            osw.append(text);
            osw.append('\n');
            //new FileOutputStream(saveFile).close();
            osw.flush();
            osw.close();
            isTextSaved = true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Copy the text showed at the UI to clipboard so that users can paste it anywhere they want.
     *
     * @param view the View that triggers this method.
     */

    /*
    public void copyTextToClipboard(View view) {
        System.out.println("copying to clipboard");

        if (textRenderer.getText().length() > 0) {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData =
                    ClipData.newPlainText(
                            "text copied from handwriting input", textRenderer.getText());
            clipboard.setPrimaryClip(clipData);
        }
        saveWritingHistory(textRenderer.getText().toString(), SAVE_DIRECTORY_NAME, WRITING_LOG_DIR_NAME);

        System.out.println("copied to clipboard");
        //setContentView(R.layout.settings); can't just do this
        Intent intent = new Intent(this, MainOptionsActivity.class);
        startActivity(intent);

    }

     */


    /** Clear text showing in the UI. */
    /*
    public void clearText(View view) {
        saveWritingHistory(textRenderer.getText().toString(), SAVE_DIRECTORY_NAME, WRITING_LOG_DIR_NAME);
        textRenderer.setText("");
    }
    */

    /**
     * This the callback method that will be called by the drawing canvas because if we attach event
     * listener to the drawing canvas, it will override the drawing logic.
     */
    @Override
    public void onTouchEnd() {
        if (autoEvaluate) {
            long ts = SystemClock.elapsedRealtime();
            synchronized (InferenceThread.LastCreatedThreadTimeLock) {
                InferenceThread.LastCreatedThreadTime = ts;
            }

            // System.out.println("Thread " + ts + " started!");
            (new InferenceThread(this, ts)).start();
        }
    }

    /**
     * Open preference settings view.
     *
     * @param view the view that triggers this method
     */
    public void openSettings(View view) { //called when the settings button is clicked
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /*
    public void clearCustomLabelText(View view) {
        customLabelEditText.setText("");
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

     */

    /*
    public void saveWritingDataWithCustomLabel(View view) { // exports writing history but with the text in second typing bar when you click icon
        String customLabel = customLabelEditText.getText().toString(); //text in the second typing bar
        System.out.println("custom label edit text: " + customLabel);

        if (currentEvaluatingImage != null
                && currentEvaluatingWritingStrokes != null
                && !customLabel.isEmpty()) {
            pushText(customLabel); //sets first typing bar to whatever is in the second

            if (canSaveWritingData()) { // if the setting is enabled by the user
                exportWritingData(
                        customLabel,
                        1f,
                        System.currentTimeMillis(),
                        currentEvaluatingImage,
                        currentEvaluatingWritingStrokes);
            }

            // After saving the data with custom label, I want to clear the current canvas, clear
            // the text from custom label input and hide my soft input keyboard. That is a lot of
            // activities to continue writing after saving custom label.
            if (autoClear) {
                // clear the canvas
                clearCanvas(view);
                // clear the label text
                customLabelEditText.setText("");
                // minimize the virtual input keyboard. Wow, this task seems pretty hard and
                // controversial.
                // https://stackoverflow.com/a/17789187/83644034
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                // I know that the view I want to hide the soft keyboard so this really help me save
                // the trouble of getting focusing view.
                // TODO I am not sure about the flags. Should I use `HIDE_IMPLICIT_ONLY`?
                imm.hideSoftInputFromWindow(customLabelEditText.getWindowToken(), 0);
            }
        }
    }

    public void lookUpMeaningWithJishoDotOrg(View view) {
        String japaneseText = this.textRenderer.getText().toString();
        if (!japaneseText.isEmpty()) {
            saveWritingHistory(japaneseText, SAVE_DIRECTORY_NAME, WRITING_LOG_DIR_NAME);

            int selectionStart = this.textRenderer.getSelectionStart();
            int selectionEnd = this.textRenderer.getSelectionEnd();

            System.out.println("selectionStart: " + selectionStart);
            System.out.println("selectionEnd: " + selectionEnd);
            if ((selectionEnd - selectionStart) > 0) {
                japaneseText = japaneseText.substring(selectionStart, selectionEnd);
            }

            System.out.println("Text to be looked up: " + japaneseText);
            try {
                String encodedText = URLEncoder.encode(japaneseText, "utf-8");
                System.out.println("Encoded text: " + encodedText);

                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK);
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(
                        this, Uri.parse("https://jisho.org/search/" + encodedText));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_key_auto_evaluate_input))) {
            this.autoEvaluate = sharedPreferences.getBoolean(key, false);
        } else if (key.equals(getString(R.string.pref_key_auto_clear_canvas))) {
            this.autoClear = sharedPreferences.getBoolean(key, false);
        } else if (key.equals(getString(R.string.pref_key_hint_text_type_alpha))) {
            int hintTextAlphaValue =
                    Integer.parseInt(
                            sharedPreferences.getString(
                                    key, Integer.toString(ResultButton.DEFAULT_HINT_TEXT_ALPHA)));

            System.out.println("hintTextAlphaValue: " + hintTextAlphaValue);
            if (hintTextAlphaValue < 0) {
                ResultButton.HINT_TEXT_ALPHA = 0; //0
            } else if (hintTextAlphaValue > 255) {
                ResultButton.HINT_TEXT_ALPHA = 255; //255
            } else {
                ResultButton.HINT_TEXT_ALPHA = hintTextAlphaValue;
            }
        } else if (key.equals(getString(R.string.pref_key_stroke_width))) {
            int writingStrokeWidth = -1;

            try {
                writingStrokeWidth =
                        Integer.parseInt(
                                sharedPreferences.getString(
                                        getString(R.string.pref_key_stroke_width), "5"));
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                System.err.println("Failed to get writing stroke width value from preferences!");
            }

            if (writingStrokeWidth < 1) {
                HandwritingCanvas.WritingStrokeWidth = 5;
            } else {
                HandwritingCanvas.WritingStrokeWidth = writingStrokeWidth;
            }
        }
    }


    ArrayList<CourseModel> processHashmap(HashMap<String, String> resultsHM) {
        //add keys to an array list

        ArrayList<CourseModel> kanjiArrayList = new ArrayList<CourseModel>();
        //print each kanji key

        //System.out.println("-----------Hash Map KEYS: ");
        int count = 0;
        for (String key: resultsHM.keySet()) {
            //System.out.println(count + ") " + key);
            //add them to array list
            kanjiArrayList.add(new CourseModel(key, R.drawable.ic_search, grades.get(key)));

            count++;
        }
        //System.out.println("--------------------------"); //shouldn't print anything because dict exists in another thread. it does.. interesting

        //create and set adapter wherever you go to the grid view page, not here
        //CourseGVAdapter adapter = new CourseGVAdapter(this, kanjiArrayList);
        //coursesGV.setAdapter(adapter);
        return kanjiArrayList;


    }
    // can process this data, taking a JSON Array to split the kanji into categories later

    private class APICallsFromClass extends AsyncTask<Void,Void,Void> {
        String data = "";
        int responseCode = -9;
        JSONArray JA;
        URL url;
        HttpURLConnection httpURLConnection;


        @Override
        protected Void doInBackground(Void... voids) {
            try {
                responseCode = -42;
                url = new URL("https://kanjialive-api.p.rapidapi.com/api/public/kanji/all");
                httpURLConnection = (HttpURLConnection) url.openConnection();

                responseCode = -50;
                httpURLConnection.setRequestProperty("X-RapidAPI-Host", "kanjialive-api.p.rapidapi.com");
                httpURLConnection.setRequestProperty("X-RapidAPI-Key", "2e7c7a14ebmsh3ea82089fdad054p1bf7b7jsn660987efe1ee");

                responseCode = -51;

                //responseCode = httpURLConnection.getResponseCode();
                //System.out.println("response code (from async): " + responseCode);

                responseCode = -52;

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                while (line != null) {
                    line = bufferedReader.readLine();
                    data = data + line;
                }

                /*
                JA = new JSONArray(data);
                for(int i = 0; i < JA.length(); i++) {
                    dict.put(JA.getJSONObject(i).optString("ka_utf"), JA.getJSONObject(i).optString("meaning"));
                    //grades.put(JA.getJSONObject(i).optString("ka_utf"), JA.getJSONObject(i).getInt("grade"));
                }

                 */

                responseCode = -55;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } //catch (JSONException e) {
            //    e.printStackTrace();
            //}
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            String meaning = "";
            String gradeString = "";

            try {
                JA = new JSONArray(data);
                for(int i = 0; i < JA.length(); i++) {
                    meaning = JA.getJSONObject(i).optString("meaning");
                    gradeString = JA.getJSONObject(i).optString("grade");

                    if(!gradeString.equals("null")) {
                        dict.put(JA.getJSONObject(i).optString("ka_utf"), meaning);
                        grades.put(JA.getJSONObject(i).optString("ka_utf"), Integer.parseInt(gradeString));
                    } else {
                        System.out.println("null grade");
                        dict.put(JA.getJSONObject(i).optString("ka_utf"), meaning);
                        grades.put(JA.getJSONObject(i).optString("ka_utf"), 30); //30 being max grade
                    }

                }
            } catch(JSONException e) {
                e.printStackTrace();
            }



            System.out.println("dict size: " + dict.size());
            System.out.println("-----------Grades Hash Map: " + grades.size());
            int count = 0;
            for (String key: grades.keySet()) {
                //System.out.println(count + ") " + key + " : " + grades.get(key));
                count++;
            }
            System.out.println("--------------------------");


            System.out.println("response code: " + responseCode);
            System.out.println("length: " + JA.length());

            ArrayList<CourseModel> kanjiChars = processHashmap(dict);

            super.onPostExecute(aVoid);
        }

    }

    public void goToLearnPage(View view) {
        //
        System.out.println("Go To Learn");
        Intent intent = new Intent(this, ChooseCharActivity.class);
        startActivity(intent);
    }

    public void goToTestPage(View view) {
        //
        System.out.println("Go To Test");

        Intent intent = new Intent(this, TestCharActivity.class);
        intent.putExtra("hashMap", dict);
        intent.putExtra("meaningToDraw", "heaven");
        //find kanji associated
        String kanjiChar = "";
        for (String key : dict.keySet()) {
            if(dict.get(key).equals("heaven")) {
                kanjiChar = key;
            }
        }

        intent.putExtra("kanjiToDraw", kanjiChar);

        startActivityForResult(intent, 2); //activity is started with request code 2
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==2)
        {
            this.charDrawn = data.getStringExtra("charDrawn");
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && (charDrawn!=null) && (!charDrawn.equals("null"))) { //null when coming from testActivity
                appendFile(charDrawn + " 0 " + LocalDate.now(), SAVE_DIRECTORY_NAME, WRITING_LOG_DIR_NAME_NEW_FILE, "learnt_characters_file.txt");
            } else {
                System.out.println("char drawn is null");
            }
        }
    }

    public void goToPlayPage(View view) {
        //
        System.out.println("Go To Play");
        Intent intent = new Intent(this, PlayActivity.class);
        startActivity(intent);
    }

    public void goToMainOptionsPage(View view) {
        //
        System.out.println("Go To Main Options");
        Intent intent = new Intent(this, MainOptionsActivity.class);
        startActivity(intent);
    }

    public void goToGridView(View view) {
        processHashmap(dict);
        Intent intent = new Intent(this, GridViewTutorial.class);
        intent.putExtra("hashMap", dict);
        intent.putExtra("gradesHashMap", grades);
        startActivityForResult(intent, 2);
    }

}

/*
* to do:
* create a new layout (page) and on clicking 'copy', open this layout
* research the Activity Life Cycle
*
* done
*
*
* */
