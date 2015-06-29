package com.cie.cieprinter.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cie.btp.BtpConsts;
import com.cie.btp.DebugLog;
import com.cie.cieprinter.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static android.widget.AdapterView.OnItemClickListener;

enum BluetoothDeviceList {
    INSTANCE;
    // Debugging
    // private static final String TAG = "DeviceListActivity";

    // Member fields

    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private Context mContext;

    private AlertDialog alertDialog;
    private Activity mActivity;


    private static final UUID BT_SPP_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    public void initDeviceList(Context ctx) {
        this.mContext = ctx;
        // this.mActivity = activity;
    }

    public static boolean bDialogShown = false;
    private TextView title_paired_devices;
    private ListView pairedListView;
    private TextView title_new_devices;
    private ListView newDevicesListView;
    private ProgressBar progressBar;
    private Button scanButton;
    private List<String> newDevicesAddress;
    private List<String> pairedDevicesAddress;

    public void createDialog1(Activity activity) {

        DebugLog.logTrace();

        newDevicesAddress = new ArrayList<>();
        pairedDevicesAddress = new ArrayList<>();

        mContext = activity;
        mActivity = activity;
        AlertDialog.Builder alertDialogBuilder;

        //TODO : FIX IT
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            alertDialogBuilder = new AlertDialog.Builder(activity);
        } else {
            alertDialogBuilder = new AlertDialog.Builder(activity);
        }

        // set title
        alertDialogBuilder.setTitle(BtpConsts.DEVICE_LIST);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.device_list, null, false);

        // Initialize the button to perform device discovery
        scanButton = (Button) v.findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_list_item_1);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_list_item_1);

        // Find and set up the ListView for paired devices
        pairedListView = (ListView) v.findViewById(R.id.pairedListView);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mPairedDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        newDevicesListView = (ListView) v.findViewById(R.id.newDevicesListView);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mNewDeviceClickListener);

        title_new_devices = (TextView) v.findViewById(R.id.title_new_devices);
        title_new_devices.setVisibility(View.GONE);

        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        alertDialogBuilder.setView(v);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        mActivity.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mActivity.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevicesSet = mBtAdapter
                .getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevicesSet.size() > 0) {
            // We are filtering bluetooth devices basing on UUID to make the
            // listview onItemSelect according to position..Here co

            for (BluetoothDevice b : pairedDevicesSet) {

                if (isBluetoothSerialPortDevice(getUUID(b))
                    //&& checkIfCieBtPrinter(pairedDevices[i].getName())
                        ) {
                    //if (DebugLog.isDebugMode())
                    mPairedDevicesArrayAdapter.add(b.getName() + "\n"
                            + b.getAddress());
                    //else
                    //   mPairedDevicesArrayAdapter.add(pairedDevices[i].getName());

                    // + " : i : " + i
                    pairedDevicesAddress.add(b.getAddress());
                }
            }

        } else {
            String noDevices = BtpConsts.no_paired_devices_found;
            mPairedDevicesArrayAdapter.add(noDevices);
        }
        alertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                onDestroyAlertDialog();
            }

        });
        bDialogShown = true;
        alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(true);
        alertDialog.show();
    }

    public void onDestroyAlertDialog() {
        DebugLog.logTrace();
        bDialogShown = false;
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        // Unregister broadcast listeners
        if ( mContext != null) {
            try {
                mContext.unregisterReceiver(mReceiver);
            } catch (IllegalArgumentException e) {
                // skip
            }
        }
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        // Indicate scanning in the title
        mActivity.setProgressBarIndeterminateVisibility(true);

        // Turn on sub-title for new devices
        title_new_devices.setVisibility(View.VISIBLE);
        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mPairedDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2,
                                long arg3) {
            DebugLog.logTrace();
            // Cancel discovery because it's costly and we're about to
            // connect
            mBtAdapter.cancelDiscovery();
            try {
                String address = pairedDevicesAddress.get(arg2);
                Log.d("addrs", "Paired Device Address : " + address
                        + " : arg2 :" + arg2);
                ((com.cie.cieprinter.app.IConnectToPrinter) mActivity).connectToPairedPrinter(address);
            } catch (IndexOutOfBoundsException ex) {
                System.out.println("ex" + ex);
            }
            alertDialog.cancel();
        }
    };

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mNewDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2,
                                long arg3) {
            DebugLog.logTrace();
            // Cancel discovery because it's costly and we're about to
            // connect
            mBtAdapter.cancelDiscovery();
            try {
                String address = newDevicesAddress.get(arg2);
                DebugLog.logTrace("New Device Address : " + address);
                //BluetoothPrinter.INSTANCE.bConnectingToNewDevice = true;

                ((com.cie.cieprinter.app.IConnectToPrinter) mActivity).connectToUnPairedPrinter(address);

            } catch (IndexOutOfBoundsException ex) {
                // Do nothing
            }
            alertDialog.cancel();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint({"InlinedApi", "NewApi"})
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            switch (action) {
                case BluetoothDevice.ACTION_FOUND: {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    // If it's already paired, skip it, because it's been listed
                    // already

                    if (device.getBondState() != BluetoothDevice.BOND_BONDED
                        //        && checkIfCieBtPrinter(device.getName())
                        //        && isBluetoothSerialPortDevice(getUUID(device))
                            ) {
//                      if (device.fetchUuidsWithSdp()) {
//                          Toast.makeText(mContext,"OK fetchUuidsWithSdp",Toast.LENGTH_SHORT).show();
//                      } else {
//                          Toast.makeText(mContext,"FAILED fetchUuidsWithSdp",Toast.LENGTH_SHORT).show();
//                      }
                        //    if(DebugLog.isDebugMode())
                        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                        //    else
                        //       mNewDevicesArrayAdapter.add(device.getName());
                        newDevicesAddress.add(device.getAddress());
                    }

                    // When discovery is finished, change the Activity title
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    // setProgressBarIndeterminateVisibility(false);
                    // setTitle(R.string.select_device);
                    if (mNewDevicesArrayAdapter.getCount() == 0) {
                        // String noDevices = getResources().getText(
                        // R.string.none_found).toString();
                        mNewDevicesArrayAdapter.add(BtpConsts.no_devices_found);
                        progressBar.setVisibility(View.GONE);
                    }
                    break;
                case BluetoothDevice.ACTION_UUID:
                    DebugLog.logTrace("ACTION_UUID");
                    BluetoothDevice btd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    DebugLog.logTrace("Received uuids for " + btd.getName());
                    Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                    StringBuilder sb = new StringBuilder();
                    //List<String> uuids = new ArrayList<String>(uuidExtra.length);
                    DebugLog.logTrace("uuidExtra length : " + uuidExtra.length);
                    if (uuidExtra != null) {
                        for (Parcelable anUuidExtra : uuidExtra) {
                            sb.append(anUuidExtra.toString()).append(',');
                            //      uuids.add(uuidExtra[i].toString());
                        }
                    }
                    DebugLog.logTrace("ACTION_UUID received for " + btd.getName() + " uuids: " + sb.toString());

                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    BluetoothDevice device = intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
//                    BluetoothPrinter.INSTANCE.bConnectingToNewDevice = false;
//                }
                    break;
                }
            }
        }
    };


    @SuppressLint("NewApi")
    // This method does not start a service discovery procedure to retrieve the UUIDs from the remote device.
    // Instead, the local cached copy of the service UUIDs are returned.
    // Use fetchUuidsWithSdp() if fresh UUIDs are desired.
    private ParcelUuid[] getUUID(BluetoothDevice device) {
        DebugLog.logTrace("address : " + device.getAddress());
        // BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        ParcelUuid[] listUUID = null;
        // This method does not start a service discovery procedure to retrieve the UUIDs from the remote device.
        // Instead, the local cached copy of the service UUIDs are returned.
        // Use fetchUuidsWithSdp() if fresh UUIDs are desired.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            Method privateStringMethod;
            try {
                privateStringMethod = BluetoothDevice.class
                        .getDeclaredMethod("getUuids");
                privateStringMethod.setAccessible(true);

                listUUID = (ParcelUuid[]) privateStringMethod
                        .invoke(device);
                if (listUUID != null) {
                    return listUUID;
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        } else {
            // This method does not start a service discovery procedure to retrieve the UUIDs from the remote device.
            // Instead, the local cached copy of the service UUIDs are returned.
            // Use fetchUuidsWithSdp() if fresh UUIDs are desired.
            listUUID = device.getUuids();
            if (listUUID == null) {
                DebugLog.logTrace("UUIDs are null : " + device.getName());
                return listUUID;
            } else {
                for (int i = 0; i < listUUID.length; i++) {
                    DebugLog.logTrace(i + " : " + device.getUuids()[i].getUuid().toString()
                            + "\n");
                }
            }
            return listUUID;
        }

        return listUUID;
    }

    private boolean isBluetoothSerialPortDevice(ParcelUuid[] list) {

        if (list == null)
            return false;

        for (ParcelUuid aList : list) {
            if (aList.getUuid().equals(BT_SPP_UUID))
                return true;
        }
        return false;
    }

    private boolean checkIfCieBtPrinter(String dName) {
        return dName != null && dName.toUpperCase(Locale.getDefault()).startsWith("BTP");
    }
}