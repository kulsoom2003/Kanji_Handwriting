package io.github.ichisadashioko.android.kanji.views;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;

public class Inventory implements Parcelable{
    /*
    * Inventory is a list of treats for virtual cat
    * Hashmap with treats and their quantity
    *
    * treats: Dango, Daifuku, Mochi
    *
    * */

    public HashMap<String, Integer> treatsAndQuantity;
    public Inventory() {
        //
        this.treatsAndQuantity = new HashMap<String, Integer>();
        this.treatsAndQuantity.put("Dango", 0);
        this.treatsAndQuantity.put("Daifuku", 0);
        this.treatsAndQuantity.put("Mochi", 0);
    }

    protected Inventory(Parcel in) {
        this.treatsAndQuantity = (HashMap<String, Integer>) in.readSerializable();
    }

    public static final Creator<Inventory> CREATOR = new Creator<Inventory>() {
        @Override
        public Inventory createFromParcel(Parcel in) {
            return new Inventory(in);
        }

        @Override
        public Inventory[] newArray(int size) {
            return new Inventory[size];
        }
    };

    public void printInventory() {
        System.out.println("Inventory Class: ");
        for (String key : treatsAndQuantity.keySet()) {
            System.out.println(key + ", " + treatsAndQuantity.get(key));
        }
    }

    public void addDango() {
        this.treatsAndQuantity.put("Dango", treatsAndQuantity.get("Dango") + 1);
    }

    public String inventoryToString() {
        String inventoryContents = "";
        for (String key : treatsAndQuantity.keySet()) {
            inventoryContents = inventoryContents + key + ", " + treatsAndQuantity.get(key) + "\n";
        }

        return inventoryContents;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(this.treatsAndQuantity);
    }
}
