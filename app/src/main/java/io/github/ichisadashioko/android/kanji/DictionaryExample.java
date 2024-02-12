package io.github.ichisadashioko.android.kanji;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

public class DictionaryExample {
    private Dictionary<String, String> dict;

    public DictionaryExample() {
        dict = new Hashtable<>();
    }

    public void loadDict() {
        dict.put("one", "一");
        dict.put("two", "二");
        dict.put("five", "五");

    }

    public String getFromKey(String key) {
        return dict.get(key);
    }

}
