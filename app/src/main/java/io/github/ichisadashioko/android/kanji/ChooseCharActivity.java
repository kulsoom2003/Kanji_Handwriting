package io.github.ichisadashioko.android.kanji;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import org.chromium.net.CronetEngine;
//tried to import HttpRequest

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;


//////////////////////
import android.os.AsyncTask;
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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;


import io.github.ichisadashioko.android.kanji.views.APICalls;
import io.github.ichisadashioko.android.kanji.views.Inventory;
import io.github.ichisadashioko.android.kanji.views.ResultButton;

////////////////////////////



public class ChooseCharActivity extends AppCompatActivity {
    public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_CODE = 0;

    public String dataFromAPI = "";
    public HashMap<String, String> dict;
    //public Inventory inventory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO this activity will crash if we change permission from the Settings app
        // https://stackoverflow.com/a/56765912/8364403
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_char); //settings

        //
        dict = new HashMap<String, String>();
        //inventory = getIntent().getParcelableExtra("inventory");

        dict.put("one", "一");
        dict.put("two", "二");
        dict.put("five", "五"); //doesn't add these values. it's okay though


        new APICallsFromClass().execute();

        System.out.println("Made a call!"); //executes at the same time as async task
    }




    public void goToDrawCharPage(View view) {
        System.out.println("Go To Draw Char");
        Intent intent = new Intent(this, DrawCharActivity.class);
        intent.putExtra("hashMap", dict);
        intent.putExtra("kanjiToDraw", "(character)");
        startActivity(intent);
    }

    public void goToMainPage(View view) {
        System.out.println("Go To Main");

        Intent intent = new Intent();
        //intent.putExtra("inventory", inventory);
        setResult(2, intent);
        finish();//finishing activity
    }


    void processValue(String resultsFromAPICall) {
        System.out.println("Data from API: ");
        System.out.println(resultsFromAPICall);
    }
    // can process this data, taking a JSON Array to split the kanji into categories later

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
                //System.out.println("connecting to API");

                //URL url = new URL("https://kanjialive-api.p.rapidapi.com/api/public/search/advanced/");

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

                //System.out.println(data);


                JA = new JSONArray(data);
                for (int i = 0; i < JA.length(); i++) {
                    JSONObject JO = (JSONObject) JA.get(i);
                    singleParsed = i + ") kanji:" + JO.get("kanji") + "\n";
                    //dataParsed = dataParsed + singleParsed;
                }

                for(int i = 0; i < JA.length(); i++) {
                    singleMeaning = i + ") " + JA.getJSONObject(i).optString("ka_utf") + " :  ";
                    singleMeaning = singleMeaning + JA.getJSONObject(i).optString("meaning") + "\n";
                    dataParsed = dataParsed + singleMeaning;
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
            dataFromAPI = dataParsed;

            System.out.println("-----------Hash Map: ");
            int count = 0;
            for (String key: dict.keySet()) {
                System.out.println(count + ") " + key + " : " + dict.get(key));
                count++;
            }
            System.out.println("--------------------------");

            System.out.println("response code: " + responseCode);
            System.out.println("length: " + JA.length());

            //processValue(dataParsed);

            System.out.println("executed from within Choose Character!!!");
            super.onPostExecute(aVoid);
        }

    }

}
