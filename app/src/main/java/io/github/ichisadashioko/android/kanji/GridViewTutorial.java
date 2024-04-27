package io.github.ichisadashioko.android.kanji;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

//import io.github.ichisadashioko.android.kanji.views.Inventory;

public class GridViewTutorial extends AppCompatActivity { //activity
    public HashMap<String, String> dict;
    public HashMap<String, Integer> grades;
    //public Inventory inventory;
    GridView kanjiGV;

    public String characterToDraw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_char);

        Intent intent = getIntent();
        dict = (HashMap<String, String>) intent.getSerializableExtra("hashMap");
        grades = (HashMap<String, Integer>) intent.getSerializableExtra("gradesHashMap");
        processDict();

        System.out.println("extra data: ");
        System.out.println("-----------Hash Map given from MainAct to GridTutorial: ");
        int countElements = 0;
        for (String key: dict.keySet()) {
            //System.out.println(countElements + ") " + key + " : " + dict.get(key)); //countElements only used for printing
            countElements++;
        }
        System.out.println("--------------------------");

        /*
        coursesGV = findViewById(R.id.idGVcourses);

        //change this to show each kanji (all of them)
        dict = new HashMap<String, String>();
        new APICallsFromClass().execute();

         */

    }

    public void goToLearnPage(View view) {
        //
        System.out.println("Go To Learn");
        Intent intent = new Intent(this, ChooseCharActivity.class);
        startActivity(intent);
    }

    public void goToDrawCharPage(View view) {
        //
        System.out.println("Go To Draw Char x20");
        Button b = (Button)view;
        String buttonText = b.getText().toString();
        System.out.println("button text: " + buttonText);

        Intent intent = new Intent(this, DrawCharActivity.class); //pass dict to DrawCharActivity?
        //intent.putExtra("message_key", "im going to ACE this project!!!");
        intent.putExtra("hashMap", dict);
        intent.putExtra("kanjiToDraw", buttonText); //put the element that was clicked here
        //intent.putExtra("inventory", inventory);
        startActivityForResult(intent, 2);
    }

    public void showMeaning(View view) {


        //Button kanjiButton = findViewById(R.id.idGVcourses).findViewById(R.id.idButtonCourse);
        //String buttonText = kanjiButton.getText().toString();

        Button meaningButton = (Button)view;
        System.out.println("text: " + meaningButton.getText().toString().substring(0,1));
        //System.out.println(dict.get("栄"));
        String meaningText = dict.get(meaningButton.getText().toString().substring(0,1));
        meaningButton.setText(meaningText);

        System.out.println("show:" + meaningButton.getText().toString().substring(0,1) + ", " + meaningText);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2
        if(requestCode==2)
        {
            //inventory = data.getParcelableExtra("inventory");
            characterToDraw = data.getStringExtra("charDrawn");

            System.out.println("inventory from grid view----");
            //inventory.printInventory();

            Intent intent = new Intent();
            //intent.putExtra("inventory", inventory);
            intent.putExtra("charDrawn", characterToDraw);
            System.out.println("character drawn (from GV): " + characterToDraw);
            setResult(2, intent);
            finish();

        }
    }


    public void goToMainPage(View view) {
        System.out.println("GV Tutorial: Go To Main");
        //inventory.printInventory();

        Intent intent = new Intent();
        //intent.putExtra("inventory", inventory);
        //setResult(2, intent);
        finish();//finishing activity
    }





    /////////////// API CALL


    void processDict() { // adds keys from dict to arrayList and sets adapter for the Grid View class
        //add keys to an array list

        ArrayList<CourseModel> kanjiArrayList = new ArrayList<CourseModel>();
        //print each kanji key

        //System.out.println("-----------Hash Map KEYS: ");
        int count = 0;
        for (String key: dict.keySet()) {
            //System.out.println(count + ") " + key);
            //add them to array list
            kanjiArrayList.add(new CourseModel(key, R.drawable.ic_search, grades.get(key)));

            count++;
        }
        //System.out.println("--------------------------"); //shouldn't print anything because dict exists in another thread. it does.. interesting

        kanjiGV = findViewById(R.id.idGVcourses);
        CourseGVAdapter adapter = new CourseGVAdapter(this, kanjiArrayList);
        kanjiGV.setAdapter(adapter);

    }
    // can process this data, taking a JSON Array to split the kanji into categories later


    /*
    private class APICallsFromClass extends AsyncTask<Void,Void,Void> {
        String data = "";
        String dataParsed = "";
        String singleParsed = "";
        String singleMeaning = "";
        int responseCode;
        JSONArray JA;

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                URL url = new URL("https://kanjialive-api.p.rapidapi.com/api/public/kanji/all");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setRequestProperty("X-RapidAPI-Host", "kanjialive-api.p.rapidapi.com");
                httpURLConnection.setRequestProperty("X-RapidAPI-Key", "2e7c7a14ebmsh3ea82089fdad054p1bf7b7jsn660987efe1ee");

                responseCode = httpURLConnection.getResponseCode();

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                while (line != null) {
                    line = bufferedReader.readLine();
                    data = data + line;
                }

                JA = new JSONArray(data);
                for(int i = 0; i < JA.length(); i++) {
                    dict.put(JA.getJSONObject(i).optString("ka_utf"), JA.getJSONObject(i).optString("meaning"));
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            /*
            System.out.println("-----------Hash Map: ");
            int count = 0;
            for (String key: dict.keySet()) {
                System.out.println(count + ") " + key + " : " + dict.get(key));
                count++;
            }
            System.out.println("--------------------------");
            */

    /*
            System.out.println("response code: " + responseCode);
            System.out.println("length: " + JA.length());

            processHashmap(dict);
            super.onPostExecute(aVoid);
        }

    }

     */
}