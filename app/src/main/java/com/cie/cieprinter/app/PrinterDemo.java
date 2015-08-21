package com.cie.cieprinter.app;


import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.cie.btp.Barcode;
import com.cie.btp.BtpConsts;
import com.cie.btp.CieBluetoothPrinter;
import com.cie.btp.PrinterWidth;
import com.cie.cieprinter.R;
import com.cie.cieprinter.bill.Bill;
import com.cie.cieprinter.loopedlabs.LlFragment;


public class PrinterDemo extends LlFragment {

    private RadioButton rbTwoInch, rbThreeInch, rbFourInch;
    private EditText etQRcode;
    private static final int BARCODE_WIDTH = 384;
    private static final int BARCODE_HEIGHT = 100;
    private TextPaint mDefaultTextPaint;
    private static int FONT_SIZE = 32;

    private CieBluetoothPrinter mPrinter = CieBluetoothPrinter.INSTANCE;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v= inflater.inflate(R.layout.printer_status, container, false);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

       rbTwoInch = (RadioButton) v.findViewById(R.id.two_inch);
        rbTwoInch.setOnClickListener(onRbClicked);
        rbThreeInch = (RadioButton) v.findViewById(R.id.three_inch);
        rbThreeInch.setOnClickListener(onRbClicked);
        rbFourInch = (RadioButton) v.findViewById(R.id.four_inch);
        rbFourInch.setOnClickListener(onRbClicked);
        printerSelection();
        etQRcode = (EditText) v.findViewById(R.id.et_qrcpde);
        Button btnClearPrefPrinter = (Button) v.findViewById(R.id.btnClearPrefPrinter);
        btnClearPrefPrinter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onAppSignal(AppConsts.CLEAR_PREFERRED_PRINTER);
            }
        });

        Button btnSetPrefPrinter = (Button) v.findViewById(R.id.btnSetPrefPrinter);
        btnSetPrefPrinter.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onClick(View v) {
                mPrinter.showDeviceList(getActivity());
            }
        });
        Button barcode = (Button) v.findViewById(R.id.barcodePrint);
        barcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = etQRcode.getText().toString();

                try {
                    int d = Integer.parseInt(txt);
                    String data = String.valueOf(d);
                    mPrinter.printBarcode(data, Barcode.CODE_128, BARCODE_WIDTH, BARCODE_HEIGHT);
                } catch (NumberFormatException nfe) {
                    Toast.makeText(getActivity(), "Enter Numeric Number to print barcode",
                            Toast.LENGTH_SHORT).show();
                }


            }
        });
        Button btnQRCode = (Button) v.findViewById(R.id.codePrint);
        btnQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = etQRcode.getText().toString();
                mPrinter.printQRcode(data);
            }
        });
        Button btnTestBill = (Button) v.findViewById(R.id.print_bill);
        btnTestBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performPrinterTask();
            }
        });
        Button btnLanguagePrint = (Button) v.findViewById(R.id.print_language);
        btnLanguagePrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt = "कम्प्यूटर, मूल रूप से, नंबरों से सम्बंध रखते हैं। ये प्रत्येक अक्षर और वर्ण के लिए एक नंबर निर्धारित करके अक्षर और वर्ण " +
                        "संग्रहित करते हैं। यूनिकोड का आविष्कार होने से पहले, ऐसे नंबर देने के लिए सैंकडों विभिन्न संकेत लिपि प्रणालियां थीं। किसी एक संकेत " +
                        "लिपि में पर्याप्त अक्षर नहीं हो सकते हैं : उदाहरण के लिए, यूरोपिय संघ को अकेले ही, अपनी सभी भाषाऒं को कवर करने के लिए अन" +
                        "ेक विभिन्न संकेत लिपियों की आवश्यकता होती है। अंग्रेजी जैसी भाषा के लिए भी, सभी अक्षरों, विरामचिन्हों और सामान्य प्रयोग के तक" +
                        "नीकी प्रतीकों हेतु एक ही संकेत लिपि पर्याप्त नहीं थी।";
                mDefaultTextPaint = new TextPaint();
                mDefaultTextPaint.setColor(Color.BLACK);
                mDefaultTextPaint.setTextSize(FONT_SIZE);
                mPrinter.printUnicodeText(txt, Layout.Alignment.ALIGN_NORMAL, mDefaultTextPaint);
            }
        });
 /*
        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(getActivity(), R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        mListener.onAppSignal(AppConsts.UPDATE_UIPREF_PRINTER);*/

     return v;
    }
    public View.OnClickListener onRbClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Is the button now checked?
            boolean checked = ((RadioButton) v).isChecked();

            // Check which radio button was clicked
            switch (v.getId()) {
                case R.id.two_inch:
                    if (checked) {
                        boolean r= mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_48MM);
                        MainActivity.mSp.edit().putInt("PRINTER_SELECTION",AppConsts.TWO_INCH).commit();
                        if(r){
                            Toast.makeText(getActivity(), "Two Inch Printer Selected",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case R.id.three_inch:
                    if (checked) {
                        boolean r = mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_72MM);
                        MainActivity.mSp.edit().putInt("PRINTER_SELECTION",AppConsts.THREE_INCH).commit();
                        if(r){
                            Toast.makeText(getActivity(), "Three Inch Printer Selected",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case R.id.four_inch:
                    if (checked) {
                        boolean r = mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_104MM);
                        MainActivity.mSp.edit().putInt("PRINTER_SELECTION",AppConsts.FOUR_INCH).commit();
                        if(r){
                            Toast.makeText(getActivity(), "Four Inch Printer Selected",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
            }
        }
    };
    private void performPrinterTask() {

        mPrinter.setPrintMode(BtpConsts.PRINT_IN_BATCH);
        mPrinter.resetPrinter();
        mPrinter.setHighIntensity();
        mPrinter.setAlignmentCenter();
        mPrinter.setBold();
        mPrinter.printTextLine("MY COMPANY BILL\n");
        mPrinter.setRegular();
        mPrinter.printTextLine("~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
        mPrinter.printLineFeed();
        // Bill Header End

        // Bill Details Start
        Bill b=new Bill();
        b.setCustomerName("Test Cust");
        b.setCustomerOrderNo("0045");

        mPrinter.setAlignmentLeft();
        mPrinter.printTextLine("Customer Name     : " + b.getCustomerName() + "\n");
        mPrinter.printTextLine("Customer Order ID : " + b.getCustomerOrderNo() + "\n");
        mPrinter.printTextLine("------------------------------\n");
        mPrinter.printTextLine("  Item      Quantity     Price\n");
        mPrinter.printTextLine("------------------------------\n");
        mPrinter.printTextLine("  Item 1          1       1.00\n");
        mPrinter.printTextLine("  Bags           10    2220.00\n");
        mPrinter.printTextLine("  Next Item     999   99999.00\n");
        mPrinter.printLineFeed();
        mPrinter.printTextLine("------------------------------\n");
        mPrinter.printTextLine("  Total              107220.00\n");
        mPrinter.printTextLine("------------------------------\n");
        mPrinter.printLineFeed();
        mPrinter.setBold();
        mPrinter.printTextLine("    Thank you ! Visit Again   \n");
        mPrinter.setRegular();
        mPrinter.printLineFeed();
        mPrinter.printTextLine("******************************\n");
        mPrinter.printLineFeed();

        mPrinter.printTextLine("~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
        mPrinter.printLineFeed();

        mPrinter.setAlignmentCenter();

        //Clearance for Paper tear
        mPrinter.printLineFeed();
        mPrinter.printLineFeed();
        mPrinter.resetPrinter();

        //print all commands
        mPrinter.batchPrint();
    }
    private void printerSelection(){
        int printer_selection = MainActivity.mSp.getInt("PRINTER_SELECTION",0);
        if (printer_selection==AppConsts.THREE_INCH) {
            rbThreeInch.setChecked(true);
            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_72MM);
        }else if (printer_selection==AppConsts.FOUR_INCH) {
            rbFourInch.setChecked(true);
            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_104MM);
        }else{
            rbTwoInch.setChecked(true);
            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_48MM);
        }
    }

    @Override
    public void onResume() {
        printerSelection();
        super.onResume();
    }
}
