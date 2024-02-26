package io.github.ichisadashioko.android.kanji.views;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;;
import java.net.MalformedURLException;


public class APICalls extends AsyncTask<Void,Void,Void> {
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

        System.out.println("data parsed: ");
        System.out.println(dataParsed);
        System.out.println("data: ");
        System.out.println(data);
        System.out.println("response code: " + responseCode);
        System.out.println("length: " + JA.length());

        System.out.println("executed!!!");
        super.onPostExecute(aVoid);
    }

}

//call every kanji, display it with its meaning
//show on screen by calling APICalls.java from ChooseCharActivity