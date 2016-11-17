package com.cie.cieprinter.app;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.cie.btp.CieBluetoothPrinter;
import com.cie.btp.DebugLog;
import com.cie.btp.PrinterWidth;
import com.cie.cieprinter.BuildConfig;
import com.cie.cieprinter.R;
import com.loopedlabs.FragmentMessageListener;


public class MainActivity extends AppCompatActivity implements  TabListener,FragmentMessageListener {

    private TextView statusMsg;
    private ViewPager viewPager;
    private ToggleButton tbPrinter;
    private android.support.v7.app.ActionBar actionBar;
    public static SharedPreferences mSp;
    public static CieBluetoothPrinter mPrinter = CieBluetoothPrinter.INSTANCE;
    // Tab titles
    private String[] tabs = {"Printer Demo","Image Print", };

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mSp = PreferenceManager.getDefaultSharedPreferences(this);
        statusMsg = (TextView) findViewById(R.id.status_msg);
        tbPrinter = (ToggleButton) findViewById(R.id.tbPrinter);
        tbPrinter.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             if (tbPrinter.isChecked()) {
                                                 mPrinter.connectToPrinter();
                                             } else {
                                                 mPrinter.disconnectFromPrinter();
                                             }
                                         }
                                     }
        );
        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getSupportActionBar();
        TabsPagerAdapter pAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(pAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

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

        //uncomment when you want to debug.
        //DebugLog.setDebugMode(BuildConfig.DEBUG);
        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        //un comment the line below to debug the print service
        //mPrinter.setDebugService(BuildConfig.DEBUG);
        try {
            mPrinter.initService(MainActivity.this, mMessenger);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPrinter.onActivityResult(requestCode, resultCode, this);
    }
    @Override
    protected void onResume() {
        mPrinter.onActivityResume();
        printerSelection();
        super.onResume();
    }


    @Override
    protected void onPause() {
        mPrinter.onActivityPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
       mPrinter.onActivityDestroy();
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
            case AppConsts.CLEAR_PREFERRED_PRINTER:
                mPrinter.clearPreferredPrinter();
                   tbPrinter.setText("OFF");
                    tbPrinter.setChecked(false);
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
            switch (msg.what) {
                case CieBluetoothPrinter.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case CieBluetoothPrinter.STATE_CONNECTED:
                            setStatusMsg(title_connected_to + mConnectedDeviceName);
                            tbPrinter.setText("ON");
                           tbPrinter.setChecked(true);
                            break;
                        case CieBluetoothPrinter.STATE_CONNECTING:
                            setStatusMsg(title_connecting);
                            try {
                             tbPrinter.setText("...");
                                tbPrinter.setChecked(false);
                            }catch (NullPointerException e){
                                DebugLog.logTrace("Fragment creating");
                            }
                            break;
                        case CieBluetoothPrinter.STATE_LISTEN:
                            setStatusMsg(title_connected_to + mConnectedDeviceName);
                        case CieBluetoothPrinter.STATE_NONE:
                            setStatusMsg(title_not_connected);
                            try {
                                tbPrinter.setText("OFF");
                                tbPrinter.setChecked(false);
                            }catch (NullPointerException n){
                                DebugLog.logTrace("Fragment creating");
                            }
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
                case CieBluetoothPrinter.PRINTER_DISCONNECTED:
                    setStatusMsg("Printer Connection failed");
                    break;
                default:
                    DebugLog.logTrace("Some un handled message : " + msg.what);
                    super.handleMessage(msg);
            }
        }
    }
    public void setStatusMsg(String msg) {
       statusMsg.setText(msg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.print_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_about:
                AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
                aboutBuilder.setTitle(R.string.app_name);
                aboutBuilder
                        .setMessage("App Version : " + BuildConfig.VERSION_NAME + "\nCoiNel Technology Solution LLP\nhttp://www.coineltech.com")
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
    public static void printerSelection(){
        int printer_selection = mSp.getInt("PRINTER_SELECTION", 0);
        if (printer_selection==AppConsts.THREE_INCH) {
            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_72MM);
        }else if(printer_selection==AppConsts.FOUR_INCH) {
            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_104MM);
        }else {
            mPrinter.setPrinterWidth(PrinterWidth.PRINT_WIDTH_48MM);
        }
    }
}
