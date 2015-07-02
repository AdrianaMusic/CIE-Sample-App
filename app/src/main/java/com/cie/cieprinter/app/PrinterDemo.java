package com.cie.cieprinter.app;


import android.annotation.TargetApi;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.cie.btp.Barcode;
import com.cie.btp.BtpPrintService;
import com.cie.btp.CieBluetoothPrinter;
import com.cie.btp.DebugLog;
import com.cie.cieprinter.R;
import com.cie.cieprinter.bill.Bill;
import com.cie.cieprinter.loopedlabs.LlFragment;

import static com.cie.btp.BtpConsts.BTP_DEVICE_MAC_ID;


public class PrinterDemo extends LlFragment {
    private static android.app.FragmentManager fragMgr;
    private Fragment mContent = null;
    public static ToggleButton tbPrinter;
    private Messenger mService = null;
    private BluetoothAdapter mAdapter;
    private String mBtpDevice = "";
    private SharedPreferences mSp;
    private EditText etQRcode;
    private static final int BARCODE_WIDTH = 384;
    private static final int BARCODE_HEIGHT = 100;

    private CieBluetoothPrinter mBtp = CieBluetoothPrinter.INSTANCE;
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v= inflater.inflate(R.layout.printer_status, container, false);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        Bill b = new Bill();
        tbPrinter = (ToggleButton) v.findViewById(R.id.tbPrinter);
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
                mBtp.showDeviceList(getActivity());
            }
        });
        Button barcode = (Button) v.findViewById(R.id.barcodePrint);
        barcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DebugLog.logTrace("clicked");
                String txt = etQRcode.getText().toString();

                try {
                    int d = Integer.parseInt(txt);
                    String data = String.valueOf(d);
                    mBtp.printBarcode(data, Barcode.CODE_128, BARCODE_WIDTH, BARCODE_HEIGHT);
                }catch (NumberFormatException nfe){
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
                mBtp.printQRcode(data);
            }
        });
        Button btnTestBill = (Button) v.findViewById(R.id.print_bill);
        btnTestBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               performPrinterTask();
            }
        });

        mListener.onAppSignal(AppConsts.START_PRINT_SERVICE);

        tbPrinter.setOnClickListener(new View.OnClickListener() {
                                         @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                                         @Override
                                         public void onClick(View v) {
                                                 if (!BtpPrintService.isRunning()) {
                                                     Toast.makeText(getActivity(), "Print Service is not running please Restart app again",
                                                             Toast.LENGTH_SHORT).show();
                                                     tbPrinter.setChecked(false);
                                                 } else {
                                                     mListener.onAppSignal(AppConsts.CONNECT_TO_DEVICE, true);
                                                 }
                                                 if(!tbPrinter.isChecked()){

                                                     mListener.onAppSignal(AppConsts.DISCONNECT_FROM_PRINTER);
                                                 }
                                         }
                                     }
        );
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(getActivity(), R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        mSp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mBtpDevice = mSp.getString(BTP_DEVICE_MAC_ID, "");
        mListener.onAppSignal(AppConsts.UPDATE_UIPREF_PRINTER);
     return v;

    }

    private void performPrinterTask() {

        mBtp.setPrintMode(AppConsts.PRINT_IN_BATCH);

        mBtp.setHighIntensity();
        mBtp.setAlignmentCenter();
        mBtp.printLineFeed("MY COMPANY BILL\n");
        mBtp.printLineFeed("~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
        mBtp.printLineFeed();
        // Bill Header End

        // Bill Details Start
        Bill b=new Bill();
        b.setCustomerName("Test Cust");
        b.setCustomerOrderNo("0045");

        mBtp.setAlignmentLeft();
        mBtp.printTextLine("Customer Name     : " + b.getCustomerName() + "\n");
        mBtp.printTextLine("Customer Order ID : " + b.getCustomerOrderNo() + "\n");
        mBtp.printTextLine("------------------------------\n");
        mBtp.printTextLine("  Item      Quantity     Price\n");
        mBtp.printTextLine("------------------------------\n");
        mBtp.printTextLine("  Item 1          1       1.00\n");
        mBtp.printTextLine("  Bags           10    2220.00\n");
        mBtp.printTextLine("  Next Item     999   99999.00\n");
        mBtp.printLineFeed();
        mBtp.printTextLine("------------------------------\n");
        mBtp.printTextLine("  Total              107220.00\n");
        mBtp.printTextLine("------------------------------\n");
        mBtp.printLineFeed();
        mBtp.printTextLine("    Thank you ! Visit Again   \n");
        mBtp.printLineFeed();
        mBtp.printTextLine("******************************\n");
        mBtp.printLineFeed();

        mBtp.printTextLine("~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
        mBtp.printLineFeed();

        mBtp.setAlignmentCenter();

        //Clearance for Paper tear
        mBtp.printLineFeed();
        mBtp.printLineFeed();
        mBtp.resetPrinter();

        //print all commands
        mBtp.batchPrint();
    }

}
