package com.cie.cieprinter.app;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.cie.btp.CieBluetoothPrinter;
import com.cie.btp.DebugLog;
import com.cie.cieprinter.BuildConfig;
import com.cie.cieprinter.R;
import com.cie.cieprinter.loopedlabs.FragmentMessageListener;

import static com.cie.btp.BtpConsts.ACTIVITY_BOUND_TO_SERVICE;
import static com.cie.btp.BtpConsts.STATUS_MSG;


public class MainActivity extends AppCompatActivity implements  TabListener,FragmentMessageListener {

    private BluetoothAdapter mAdapter;
    private TextView statusMsg;
    private ViewPager viewPager;
    private TabsPagerAdapter pAdapter;
    private android.support.v7.app.ActionBar actionBar;
    public static CieBluetoothPrinter mBtp = CieBluetoothPrinter.INSTANCE;
    // Tab titles
    private String[] tabs = {"Printer Demo","Image Print", };

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mBtp.setDebugService(true);
        try {
            mBtp.initService(MainActivity.this, mMessenger);
        } catch (Exception e) {
            e.printStackTrace();
        }
        statusMsg = (TextView) findViewById(R.id.status_msg);
        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getSupportActionBar();
        pAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(pAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(getSupportActionBar().NAVIGATION_MODE_TABS);

        // Adding Tabs
        for (String tab_name : tabs) {
            actionBar.addTab(actionBar.newTab().setText(tab_name)
                    .setTabListener(this));
        }
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }
            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
        DebugLog.setDebugMode(BuildConfig.DEBUG);

        DebugLog.logTrace();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    /**
     * Called when system activity to enable bluetooth exits, giving you the
     * requestCode you started it with, the resultCode it returned, and any
     * additional data from it. The resultCode will be RESULT_CANCELED if the
     * activity explicitly returned that, didn't return any result, or crashed
     * during its operation.
     */


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        DebugLog.logTrace();
        super.onActivityResult(requestCode, resultCode, data);
        mBtp.onActivityResult(requestCode, resultCode,this);
    }
    @Override
    protected void onResume() {
        DebugLog.logTrace();
        mBtp.onActivityResume();
//        iPrinterTask = AppConsts.NO_PRINT_TASK;
        super.onResume();
    }

    @Override
    protected void onPause() {
        DebugLog.logTrace();
        mBtp.onActivityPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        DebugLog.logTrace();
        mBtp.onActivityDestroy();
        super.onDestroy();
    }


    final Messenger mMessenger = new Messenger(new PrintSrvMsgHandler());
    private String mConnectedDeviceName = "";
    public static final String title_connecting = "connecting...";
    public static final String title_connected_to = "connected: ";
    public static final String title_not_connected = "not connected";

    @Override
    public void onTabSelected(android.support.v7.app.ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(android.support.v7.app.ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(android.support.v7.app.ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
    }

    @Override
    public void onAppSignal(int iAppSignal) {
        switch (iAppSignal) {
            case AppConsts.STATUS:
                break;
            case AppConsts.START_PRINT_SERVICE:
                mBtp.startPrintService();
                mBtp.connectToPrinter();
                break;
            case AppConsts.DISCONNECT_FROM_PRINTER:
                mBtp.disconnectFromDevice();
                break;
            case AppConsts.CLEAR_PREFERRED_PRINTER:
                mBtp.clearPreferredPrinter();
                try {
                    PrinterDemo.tbPrinter.setText("OFF");
                    PrinterDemo.tbPrinter.setChecked(false);
                }catch (NullPointerException n){
                    DebugLog.logTrace("Fragment creating");
                }
                break;
            case AppConsts.CONNECT_TO_DEVICE:
                mBtp.connectToPrinter();
                break;
        }
    }

    @Override
    public void onAppSignal(int iAppSignal, String data) {
    }

    @Override
    public void onAppSignal(int iAppSignal, boolean data) {}

    @Override
    public void onAppSignal(int iAppSignal, byte[] data) {

    }

    class PrintSrvMsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            DebugLog.logTrace();
            switch (msg.what) {
                case STATUS_MSG:
                    switch (msg.arg1) {
                        case ACTIVITY_BOUND_TO_SERVICE:
                            setStatusMsg(R.string.starting_print_service);
                            mBtp.connectToPrinter();
                            break;

                    }
                    break;
                // Direct from the Bluetooth Printer
                case CieBluetoothPrinter.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case CieBluetoothPrinter.STATE_CONNECTED:
                            setStatusMsg(title_connected_to + mConnectedDeviceName);
                            PrinterDemo.tbPrinter.setText("ON");
                            PrinterDemo.tbPrinter.setChecked(true);

                            break;
                        case CieBluetoothPrinter.STATE_CONNECTING:
                            setStatusMsg(title_connecting);
                            try {
                                PrinterDemo.tbPrinter.setText("...");
                                PrinterDemo.tbPrinter.setChecked(false);
                            }catch (NullPointerException e){
                                DebugLog.logTrace("Fragment creating");
                            }
                            break;
                        case CieBluetoothPrinter.STATE_LISTEN:
                            setStatusMsg(title_connected_to + mConnectedDeviceName);
                        case CieBluetoothPrinter.STATE_NONE:
                            setStatusMsg(title_not_connected);
                            try {
                                PrinterDemo.tbPrinter.setText("OFF");
                                PrinterDemo.tbPrinter.setChecked(false);
                            }catch (NullPointerException n){
                                DebugLog.logTrace("Fragment creating");
                            }
                            break;
                        case CieBluetoothPrinter.START_PRINT_SERVICE:
                            mBtp.startPrintService();
                            mBtp.connectToPrinter();
                            break;
                    }
                    break;
                case CieBluetoothPrinter.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(
                            CieBluetoothPrinter.DEVICE_NAME);
                    break;
                case CieBluetoothPrinter.MESSAGE_STATUS:
                    DebugLog.logTrace("Message Status Received");
                    setStatusMsg(msg.getData().getString(
                            CieBluetoothPrinter.STATUS_TEXT));
                    break;
                case CieBluetoothPrinter.PRINT_COMPLETE:
                    setStatusMsg("PRINT OK");
                    break;
                case CieBluetoothPrinter.PRINTER_CONNECTION_CLOSED:
                    setStatusMsg("Printer Connection closed");
                    break;

                default:
                    DebugLog.logTrace("Some un handled message : " + msg.what);
                    super.handleMessage(msg);
            }
        }
    }
    public void setStatusMsg(String msg) {
        DebugLog.logTrace(msg);
        statusMsg.setText(msg);
    }
    private void setStatusMsg(int resId) {
        DebugLog.logTrace();
//        tvMsg.setText(getResources().getString(resId));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        DebugLog.logTrace();
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.print_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DebugLog.logTrace();

        int id = item.getItemId();

        switch (id) {
            case R.id.action_about:
                AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
                aboutBuilder.setTitle(R.string.app_name);
                aboutBuilder
                        .setMessage("App Version : " + BuildConfig.VERSION_CODE + "\nCoiNel Technology Solution LLP\nhttp://www.coineltech.com")
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog aboutDialog = aboutBuilder.create();
                aboutDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
