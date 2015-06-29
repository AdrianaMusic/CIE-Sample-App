package com.cie.cieprinter.bill;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class LineItems {

    @SerializedName("category")
    private String category;

    @SerializedName("items")
    private Items items[];

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Items[] getItems() {
        return items;
    }

    public void setItems(Items[] items) {
        this.items = items;
    }

    public void fromString(String sJson) {
        Gson g = new Gson();
        LineItems li = g.fromJson(sJson, LineItems.class);
        setCategory(li.getCategory());
        setItems(li.getItems());
    }

    public String toString() {
        Gson g = new Gson();
        String s = g.toJson(this);
        return s;
    }
}
