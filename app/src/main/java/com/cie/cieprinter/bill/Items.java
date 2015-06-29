package com.cie.cieprinter.bill;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Items {

    @SerializedName("product_id")
    private String productId;

    @SerializedName("id")
    private int id;

    @SerializedName("product_name")
    private String productName;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("amount")
    private int amount;

    @SerializedName("state")
    private String state;

    @SerializedName("price")
    private int price;


    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void fromString(String sJson) {
        Gson g = new Gson();
        Items i = g.fromJson(sJson, Items.class);
        setProductId(i.getProductId());
        setId(i.getId());
        setImageUrl(i.getImageUrl());
        setProductName(i.getProductName());
        setAmount(i.getAmount());
        setPrice(i.getPrice());
        setQuantity(i.getQuantity());
        setState(i.getState());
    }

    public String toString() {
        Gson g = new Gson();
        String s = g.toJson(this);
        return s;
    }
}
