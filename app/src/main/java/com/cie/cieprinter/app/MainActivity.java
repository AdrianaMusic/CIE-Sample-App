package com.cie.cieprinter.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.cie.btp.BluetoothPrinter;
import com.cie.btp.BtpConsts;
import com.cie.btp.BtpPrintService;
import com.cie.btp.DebugLog;
import com.cie.btp.PrintCmds;
import com.cie.cieprinter.BuildConfig;
import com.cie.cieprinter.R;
import com.cie.cieprinter.bill.Bill;
import com.cie.cieprinter.loopedlabs.FragmentMessageListener;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

import static com.cie.btp.BtpConsts.ACTIVITY_BOUND_TO_SERVICE;
import static com.cie.btp.BtpConsts.BTP_DEVICE_MAC_ID;
import static com.cie.btp.BtpConsts.BTP_MSG_DATA;
import static com.cie.btp.BtpConsts.DISCONNECT_FROM_PRINTER;
import static com.cie.btp.BtpConsts.GET_PRINTER_STATUS;
import static com.cie.btp.BtpConsts.REGISTER_CLIENT;
import static com.cie.btp.BtpConsts.STATUS_MSG;
import static com.cie.btp.BtpConsts.UNREGISTER_CLIENT;
import static com.cie.btp.BtpConsts.WRITE_TO_PRINTER;


public class MainActivity extends AppCompatActivity implements IConnectToPrinter, TabListener,FragmentMessageListener {

    private BluetoothAdapter mAdapter;
    private String mBtpDevice = "";
    private SharedPreferences mSp;
    private Fragment mContent = null;
    private TextView statusMsg;
    private ViewPager viewPager;
    private TabsPagerAdapter pAdapter;
    private Bill b = new Bill();
    private int iPrinterTask = AppConsts.NO_PRINT_TASK;
    private android.support.v7.app.ActionBar actionBar;
    public static BluetoothPrinter mBtp = BluetoothPrinter.INSTANCE;
    // Tab titles
    private String[] tabs = {"Print Demo", "Image print" };

    private static final int REQUEST_ENABLE_BT = 2;
    private com.cie.cieprinter.app.BluetoothDeviceList mDialogLayout = com.cie.cieprinter.app.BluetoothDeviceList.INSTANCE;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
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
        mSp = PreferenceManager.getDefaultSharedPreferences(this);
        mBtpDevice = mSp.getString(BTP_DEVICE_MAC_ID, "");
        updateUiPrefPrinter();

    }


    /**
     * Called when system activity to enable bluetooth exits, giving you the
     * requestCode you started it with, the resultCode it returned, and any
     * additional data from it. The resultCode will be RESULT_CANCELED if the
     * activity explicitly returned that, didn't return any result, or crashed
     * during its operation.
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        DebugLog.logTrace();
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    setStatusMsg(R.string.bt_switched_on);
                    startPrintService();
                } else {
                    setStatusMsg(R.string.bt_denied);
                }
                break;
        }
    }
    /*  @Override
      protected void onSaveInstanceState(Bundle outState) {
          getSupportFragmentManager().putFragment(outState, "mContent", mContent);
          super.onSaveInstanceState(outState);
      }*/
    String TabFragmentB;
    public void setTabFragmentB(String t){
        TabFragmentB = t;
    }
    public String getTabFragmentB(){
        return TabFragmentB;
    }
    private boolean mBoundToService = false;
    private Messenger mService = null;
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
                DebugLog.logTrace("status printing");
                statusMsg.setText("Printing..");
                break;
            case AppConsts.START_PRINT_SERVICE:
                startPrintService();
                break;
            case AppConsts.CONNECT_TO_PRINTER:
                sendMessageToPrintService(BtpConsts.CONNECT_TO_PRINTER, mBtpDevice);
                break;
            case AppConsts.DO_UNBIND_SERVICE:
                statusMsg.setText(R.string.stopping_print_service);
                doUnbindService();
                break;
            case AppConsts.DISCONNECT_FROM_PRINTER:
                disconnectFromDevice();
                break;
            case AppConsts.UPDATE_UIPREF_PRINTER:
                updateUiPrefPrinter();
                break;
            case AppConsts.CLEAR_PREFERRED_PRINTER:
                clearPreferredPrinter();
                break;
            case AppConsts.PRINT_BILL:
                performPrinterTask();
        }
    }

    @Override
    public void onAppSignal(int iAppSignal, String data) {
        switch (iAppSignal) {
            case AppConsts.PRINTS:
                sendMessageToPrintService(WRITE_TO_PRINTER, data);
                break;

        }
    }

    @Override
    public void onAppSignal(int iAppSignal, boolean data) {
        switch (iAppSignal) {
            case AppConsts.CONNECT_TO_DEVICE:
                connectToDevice(data);
                break;
        }
    }

    @Override
    public void onAppSignal(int iAppSignal, byte[] data) {
        switch (iAppSignal) {
            case AppConsts.PRINT:
                sendMessageToPrintService(WRITE_TO_PRINTER, data);
                break;
        }

    }

    private void performPrinterTask() {

        PrintCmds cmds = new PrintCmds();
        // Bill Header Start
        cmds.setHighIntensity();
        cmds.setAlignmentCenter();
        cmds.printLineFeed("MY COMPANY BILL");
        cmds.printLineFeed("~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        cmds.printLineFeed();
        // Bill Header End

        // Bill Details Start
        cmds.setAlignmentLeft();
        cmds.printTextLine("Customer Name     : " + b.getCustomerName() + "\n");
        cmds.printTextLine("Customer Order ID : " + b.getCustomerOrderNo() + "\n");
        cmds.printTextLine("Customer name : " + b.getCustomerName() + "\n");
        cmds.printTextLine("Customer order no  : " + b.getCustomerOrderNo() + "\n");
        cmds.printTextLine("------------------------------\n");
        cmds.printTextLine("  Item      Quantity     Price\n");
        cmds.printTextLine("------------------------------\n");
        cmds.printTextLine("  Item 1          1       1.00\n");
        cmds.printTextLine("  Some big item   10   7890.00\n");
        cmds.printTextLine("  Next Item       999 10000.00\n");
        cmds.printLineFeed();
        cmds.printTextLine("------------------------------\n");
        cmds.printTextLine("  Total               17891.00\n");
        cmds.printTextLine("------------------------------\n");
        cmds.printLineFeed();
        cmds.printTextLine("    Thank you ! Visit Again   \n");
        cmds.printLineFeed();
        cmds.printTextLine("******************************\n");
        cmds.printLineFeed();




        cmds.printTextLine("~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
        cmds.printLineFeed();

        DebugLog.logTrace("order_state :" + b.getOrderState());
        DebugLog.logTrace("invoice_id : " + b.getInvoiceId());
        DebugLog.logTrace("customer_name : " + b.getCustomerName());
        DebugLog.logTrace("customer_order_no : " + b.getCustomerOrderNo());
        DebugLog.logTrace("time_to_rts : " + b.getTimeToRts());
        DebugLog.logTrace("status : " + b.getStatus());

            cmds.setAlignmentCenter();
            cmds.printTextLine("THANK YOU");

           //Clearance for Paper tear
            cmds.printLineFeed();
            cmds.printLineFeed();
            cmds.resetPrinter();
            byte[] d = cmds.PrintCmd();
            sendMessageToPrintService(WRITE_TO_PRINTER, d);
        
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
                            connectToDevice(true);
                            break;
                        case WRITE_TO_PRINTER:
                            break;
                        case DISCONNECT_FROM_PRINTER:
                            break;
                        case GET_PRINTER_STATUS:

                            break;
                    }
                    break;
                // Direct from the Bluetooth Printer
                case BluetoothPrinter.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothPrinter.STATE_CONNECTED:
                            setStatusMsg(title_connected_to + mConnectedDeviceName);
                            PrinterDemo.tbPrinter.setText("ON");
                            PrinterDemo.tbPrinter.setChecked(true);
                            break;
                        case BluetoothPrinter.STATE_CONNECTING:
                            setStatusMsg(title_connecting);
                            PrinterDemo.tbPrinter.setText("...");
                            break;
                        case BluetoothPrinter.STATE_LISTEN:
                        case BluetoothPrinter.STATE_NONE:
                            PrinterDemo.tbPrinter.setText("OFF");
                            PrinterDemo.tbPrinter.setChecked(false);
                            break;
                    }
                    break;
                case BluetoothPrinter.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(
                            BluetoothPrinter.DEVICE_NAME);
                    break;
                case BluetoothPrinter.MESSAGE_STATUS:
                    DebugLog.logTrace("Message Status Received");
                    setStatusMsg(msg.getData().getString(
                            BluetoothPrinter.STATUS_TEXT));
                    break;
                case BluetoothPrinter.PRINT_COMPLETE:
                    setStatusMsg("PRINT OK");
                    break;
                case BluetoothPrinter.PRINTER_CONNECTION_CLOSED:
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

    private ServiceConnection mSrvConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);

            sendMessageToPrintService(REGISTER_CLIENT);
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
        }
    };

    private void doBindService() {
        DebugLog.logTrace();
        bindService(new Intent(this, BtpPrintService.class), mSrvConn, Context.BIND_AUTO_CREATE);
        mBoundToService = true;
        DebugLog.logTrace("checkingg");
    }

    private void doUnbindService() {
        DebugLog.logTrace();
        if (mBoundToService) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            sendMessageToPrintService(UNREGISTER_CLIENT);
            // Detach our existing connection.
            unbindService(mSrvConn);
            mBoundToService = false;
        }
    }

    private void sendMessageToPrintService(int what) {
        DebugLog.logTrace("INT what : " + what);
        if (mService != null) {
            try {
                Message msg = Message.obtain(null, what);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
        }
    }

    private void sendMessageToPrintService(int what, String data) {
        DebugLog.logTrace("INT what : " + what + " : String : " + data);
        if (mService != null) {
            try {
                Message msg = Message.obtain(null, what);
                msg.replyTo = mMessenger;
                if (data.length() > 0) {
                    Bundle b = new Bundle();
                    b.putString(BTP_MSG_DATA, data);
                    msg.setData(b);
                }
                mService.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
        }
    }

    public void sendMessageToPrintService(int what, byte[] data) {
        DebugLog.logTrace("INT what : " + what + " Bytes : " + DebugLog.bytesToHex(data));
        if (mService != null) {
            try {

                Message msg = Message.obtain(null, what);
                msg.replyTo = mMessenger;
                if (data.length > 0) {
                    Bundle b = new Bundle();
                    b.putByteArray(BTP_MSG_DATA, data);
                    msg.setData(b);
                }
                mService.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service has crashed.
            }
        }else{
            DebugLog.logTrace("out");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (com.cie.cieprinter.app.BluetoothDeviceList.bDialogShown) {
            mDialogLayout.onDestroyAlertDialog();
        }

        doUnbindService();
    }

    public void connectToDevice(boolean bPaired) {
        DebugLog.logTrace();
        if (bPaired) {
            getPreferredDevice();
        }
        if (mBtpDevice.length() == 0) {
            mDialogLayout.createDialog1(this);
        } else {
            sendMessageToPrintService(BtpConsts.CONNECT_TO_PRINTER, mBtpDevice);
        }
    }

    public void connectToPairedPrinter(String address) {
        DebugLog.logTrace();
        if (address.length() > 0) {
            savePrefPrinter(address);
            connectToDevice(true);
        }
    }

    public void connectToUnPairedPrinter(String address) {
        DebugLog.logTrace();
        DebugLog.logTrace("Mac address main : " + address);
        if (address.length() > 0) {
            savePrefPrinter(address);
            connectToDevice(false);
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void savePrefPrinter(String address) {
        if (address.length() > 0) {
            mBtpDevice = address;
            mSp.edit().putString(BTP_DEVICE_MAC_ID, address).apply();
            updateUiPrefPrinter();
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void updateUiPrefPrinter() {
        if (mBtpDevice.isEmpty()) {
            return;
        }

        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                if (device.getAddress().equalsIgnoreCase(mBtpDevice)) {
//                    tvPreferredPrinter.setText(device.getName() + "\n" + device.getAddress());
                    break;
                }
            }
        }
    }

    public void disconnectFromDevice() {
        DebugLog.logTrace();
        sendMessageToPrintService(BtpConsts.DISCONNECT_FROM_PRINTER);
    }

    private void getPreferredDevice() {
        DebugLog.logTrace();
        if (mBtpDevice.length() == 0) {
            mBtpDevice = mSp.getString(BTP_DEVICE_MAC_ID, "");
            if (mBtpDevice.length() == 0) {
                return;
            }
        }
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevicesSet = mAdapter
                .getBondedDevices();

        for (BluetoothDevice pairedDevice : pairedDevicesSet) {
            String d = pairedDevice.getAddress();
            if (d.equalsIgnoreCase(mBtpDevice)) {
                // still paired, so ok
                return;
            }
        }

        //If we are here then it means that preferred device was removed. or
        //Some one has unpaired the devices, so forget the preferred device.
        clearPreferredPrinter();
    }

    /**
     * erase the preferred printer details.
     */
    public void clearPreferredPrinter() {
        DebugLog.logTrace();
        mBtpDevice = "";
        mSp.edit().putString(BTP_DEVICE_MAC_ID, mBtpDevice).commit();
        statusMsg.setText("");
    }

    /**
     * unpair and delete all the paired bluetooth devices.
     * <p/>
     * be carefull, as this will delete all the paired bluetooth devices to the
     * device.
     */
    public boolean unPairBluetoothPrinters() {

        DebugLog.logTrace();

        Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();

        try {
            Class<?> btDeviceInstance = Class.forName(BluetoothDevice.class
                    .getCanonicalName());
            Method removeBondMethod = btDeviceInstance.getMethod("removeBond");
            boolean cleared = false;
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                String sDeviceName = bluetoothDevice.getName();
                DebugLog.logTrace("Device Name : " + sDeviceName);
                if (checkIfCieBtPrinter(sDeviceName)) {
                    removeBondMethod.invoke(bluetoothDevice);
                    DebugLog.logTrace("Cleared Pairing");
                    cleared = true;
                }
            }

            if (cleared) {
                // disconnect and clear any stored printers.
//                disconnectFromPrinter();
                clearPreferredPrinter();
                return true;
            }
        } catch (Exception e) {
            DebugLog.logException("Error pairing", e);
        }
        return false;
    }

    private boolean checkIfCieBtPrinter(String dName) {
        DebugLog.logTrace();
        return dName != null && dName.toUpperCase(Locale.getDefault()).startsWith("BTP");
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
//            case R.id.action_settings:
//                stopPrintService();
//                return true;
//            case R.id.action_clear_printer:
//                clearPreferredPrinter();
//                return true;
            case R.id.action_about:
                AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
                aboutBuilder.setTitle(R.string.app_name);
                aboutBuilder
                        .setMessage("App Version : " + BuildConfig.VERSION_CODE + "\nDeveloped By : Looped Labs Pvt. Ltd.\nhttp://loopedlabs.com")
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

    public boolean startPrintService() {
        DebugLog.logTrace();
        try {
            if (!mAdapter.isEnabled()) {
                DebugLog.logTrace("mAdapter");
                Intent enableBt = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.startActivityForResult(enableBt,
                        REQUEST_ENABLE_BT);
                return false;
            }
        }catch (NullPointerException e){
            DebugLog.logException(e);
        }

        if (!BtpPrintService.isRunning()) {
            DebugLog.logTrace("m11");
            Intent intent = new Intent(MainActivity.this, BtpPrintService.class);
            startService(intent);
        }

        doBindService();
        return true;
    }

    public void stopPrintService() {
        DebugLog.logTrace();
        if (BtpPrintService.isRunning()) {
            Intent intent = new Intent(this, BtpPrintService.class);
            doUnbindService();
            stopService(intent);
            setStatusMsg(R.string.stopping_print_service);
        }
//        tbService.setChecked(false);
    }
}
