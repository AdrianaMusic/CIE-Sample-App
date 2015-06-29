package com.cie.cieprinter.bill;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Bill {

    @SerializedName("order_state")
    private String orderState;

    @SerializedName("invoice_id")
    private String invoiceId;

    @SerializedName("line_items")
    private LineItems lineItems[];

    @SerializedName("customer_name")
    private String customerName;

    @SerializedName("customer_order_no")
    private String customerOrderNo;

    @SerializedName("time_to_rts")
    private int timeToRts;

    @SerializedName("status")
    private String status;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerOrderNo() {
        return customerOrderNo;
    }

    public void setCustomerOrderNo(String customerOrderNo) {
        this.customerOrderNo = customerOrderNo;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public LineItems[] getLineItems() {
        return lineItems;
    }

    public void setLineItems(LineItems[] lineItems) {
        this.lineItems = lineItems;
    }

    public String getOrderState() {
        return orderState;
    }

    public void setOrderState(String orderState) {
        this.orderState = orderState;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTimeToRts() {
        return timeToRts;
    }

    public void setTimeToRts(int timeToRts) {
        this.timeToRts = timeToRts;
    }

    public void fromString(String sJson) {
        Gson g = new Gson();
        Bill b = g.fromJson(sJson, Bill.class);
        setOrderState(b.getOrderState());
        setInvoiceId(b.getInvoiceId());
        setLineItems(b.getLineItems());
        setCustomerName(b.getCustomerName());
        setCustomerOrderNo(b.getCustomerOrderNo());
        setTimeToRts(b.getTimeToRts());
        setStatus(b.getStatus());
    }

    public String toString() {
        Gson g = new Gson();
        String s = g.toJson(this);
        return s;
    }
}
