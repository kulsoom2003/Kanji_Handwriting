package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import io.github.ichisadashioko.android.kanji.tflite.KanjiClassifier;
import io.github.ichisadashioko.android.kanji.tflite.Recognition;
import io.github.ichisadashioko.android.kanji.views.BitmapView;
import io.github.ichisadashioko.android.kanji.views.CanvasPoint2D;
import io.github.ichisadashioko.android.kanji.views.HandwritingCanvas;
import io.github.ichisadashioko.android.kanji.views.Inventory;
import io.github.ichisadashioko.android.kanji.views.ResultButton;
import io.github.ichisadashioko.android.kanji.views.TouchCallback;



public class TestCharActivity extends Activity
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
    public DictionaryExample dictExample;

    public HashMap<String, String> dict;
    public Inventory inventory;
    public String meaning;
    public String characterToDraw;
    public HashMap<String, String> charsLearnt;
    public HashMap<String, Integer> charsToTest; // character, reps
    public Iterator<HashMap.Entry<String, Integer>> iterator;
    public HashMap.Entry<String, Integer> charsToTestEntry;
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
        //inventory = (HashMap<String, Integer>) intent.getSerializableExtra("inventory");
        //meaning = intent.getStringExtra("meaningToDraw");

        characterToDraw = intent.getStringExtra("kanjiToDraw");
        //randomise characterToDraw

        charsLearnt = new HashMap<String, String>();
        charsToTest = new HashMap<String, Integer>();

        System.out.println("Reading file");
        //String charLearntKey;
        //String charLearntNoTestedAndDate;
        try {
            System.out.println("locating file for LearntChars");
            File myObj = new File("/storage/emulated/0/Download/handwriting_data/learnt_characters/000000.txt");
            System.out.println("located file for LearntChars");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                System.out.println(data);
                // add to charsLearnt HashMap
                String charLearntKey = data.substring(0,1);
                String charLearntNoTestedAndDate = data.substring(2); //start from 2nd index (after char and space)
                //System.out.println(charLearntKey + ", " + charLearntNoTestedAndDate);
                //charsLearnt.put(charLearntKey, charLearntNoTestedAndDate);
                charsLearnt.put(charLearntKey, charLearntNoTestedAndDate);

            }
            myReader.close();
            System.out.println("Done reading file");
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        //print charsLearnt hash map
        System.out.println("CharsLearnt HashMap: ");
        for (String key : charsLearnt.keySet()) {
            System.out.println(key + ", " + charsLearnt.get(key));
        }

        int noOfTimesTested;
        int daysUntilNextTest;
        LocalDate dueDate = null;
        LocalDate dateTemp = null; //to initialise them
        int reps;
        //above for SR algorithm

        System.out.println("Chars Learnt Information");
        for (String key : charsLearnt.keySet()) {
            //System.out.println(key + ", " + charsLearnt.get(key));
            noOfTimesTested = Integer.parseInt(charsLearnt.get(key).split(" ")[0]);
            daysUntilNextTest = noOfTimesTested;
            reps = 20 - noOfTimesTested;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dateTemp = LocalDate.parse(charsLearnt.get(key).split(" ")[1]);
                dueDate = dateTemp.plusDays(daysUntilNextTest);

                //print info
                System.out.println(key + ") " + dueDate + ", reps: " + reps);

                if(dueDate.isBefore(LocalDate.now()) || dueDate.equals(LocalDate.now())) { //shouldn't equal, but for testing purposes will keep this for now
                    charsToTest.put(key, reps);
                } else {
                    System.out.println(key + " not due yet");
                }
            }
        }

        System.out.println("Chars to Test: ");
        if(charsToTest.isEmpty()) {
            System.out.println("no chars to test"); //pick random from learnt
        }
        for (String key : charsToTest.keySet()) {
            System.out.println(key + ", " + charsToTest.get(key));
        }

        iterator = charsToTest.entrySet().iterator();
        charsToTestEntry = iterator.next();


        System.out.println("extra data: ");
        System.out.println("-----------Hash Map from Draw Char: ");
        int countElements = 0;
        for (String key: dict.keySet()) {
            //System.out.println(countElements + ") " + key + " : " + dict.get(key)); //countElements only used for printing
            countElements++;
        }
        System.out.println("--------------------------");


        //randomise char to draw

        // find first key in charToTest


        characterToDraw = charsToTestEntry.getKey();
        meaning = dict.get(characterToDraw);

        System.out.println("to draw: " + meaning + " ( " + characterToDraw + ")");
        Button kanjiButton = findViewById(R.id.charToDraw);
        kanjiButton.setText("Draw the character for: " + meaning + " (" + characterToDraw + ") " + charsToTestEntry.getValue() + " times");

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


        button = findViewById(R.id.countButton);
        button.setText(Integer.toString(count));

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

        //updateFileTestedChar("夕");


    }

    public void updateFileTestedChar(String charTested) {
        //updating file
        try {
            System.out.println("opening file to update");
            // input the file content to the String "input"
            BufferedReader file = new BufferedReader(new FileReader("/storage/emulated/0/Download/handwriting_data/learnt_characters/000000.txt"));
            String line;
            String input = "";

            while ((line = file.readLine()) != null) {
                input += line + '\n';
            }

            System.out.println("input: " + input); // check that it's inputted right

            //input = input.replace("夕 17 2024-02-10", "夕 18 2024-02-10");
            String[] lines = input.split("\n");
            String lineToReplace = "";
            for(String element : lines) {
                if(Character.compare(element.charAt(0), charTested.charAt(0)) == 0) {
                    lineToReplace = element;
                }
            }
            //String lineToReplace = input.substring(input.indexOf(charTested), input.indexOf('\n'));
            //System.out.println("line to replace: " + lineToReplace);
            String replaceWith = "";

            int oldTimesTested = Integer.parseInt(lineToReplace.split(" ")[1]);
            LocalDate oldDate = null;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                oldDate = LocalDate.parse(lineToReplace.split(" ")[2]);
                replaceWith = charTested + " " + (oldTimesTested + 1) + " " + LocalDate.now();
            }

            System.out.println("line to replace: " + lineToReplace);
            System.out.println("replace with: " + replaceWith);


            input = input.replace(lineToReplace, replaceWith);
            System.out.println("new input: " + input);

            // write the new String with the replaced line OVER the same file
            FileOutputStream File = new FileOutputStream("/storage/emulated/0/Download/handwriting_data/learnt_characters/000000.txt");
            File.write(input.getBytes());

        } catch (Exception e) {
            System.out.println("Problem reading file.");
        }
    }

    public void goToMainPage(View view) {
        System.out.println("Go To Main");

        Intent intent = new Intent();
        intent.putExtra("inventory", inventory);
        setResult(2, intent);
        finish();//finishing activity
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


        if(results.get(0).title.equals(characterToDraw)) { // meaning
            count++; //increase count of number of evaluations so can count until 20 character draws
            inventory.addDango();
            inventory.printInventory();
            ////////////////////////////////
            System.out.println("Correct!!!!");
            button = findViewById(R.id.countButton);
            button.setText(Integer.toString(count)); //reset count button to show increased number

            Button kanjiButton = findViewById(R.id.charToDraw);
            kanjiButton.setText("Draw the character for: " + meaning + " (" + characterToDraw + ") " + (charsToTestEntry.getValue() - count) + " times");
        }

        if(count == charsToTestEntry.getValue()) { //done reps

            //update file here with new due date & reps.
            updateFileTestedChar(characterToDraw);
            //display correct
            button.setText("Well done! Will add to inventory");
            //display that treats were added to the inventory

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    Intent intent = new Intent();
                    intent.putExtra("inventory", inventory);
                    setResult(2, intent);
                    finish();//finishing activity
                }
            }, 3000);

            /*
            Intent intent = new Intent();
            intent.putExtra("inventory", inventory);
            setResult(2, intent);
            finish();//finishing activity

             */

            //count = 0;
            //button = findViewById(R.id.countButton);
            //button.setText(Integer.toString(count)); //reset count button to show increased number
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

}

