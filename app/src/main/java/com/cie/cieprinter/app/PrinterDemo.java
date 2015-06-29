package com.cie.cieprinter.app;


import android.annotation.TargetApi;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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

import com.cie.btp.BtpPrintService;
import com.cie.btp.DebugLog;
import com.cie.cieprinter.R;
import com.cie.cieprinter.bill.Bill;
import com.cie.cieprinter.loopedlabs.LlFragment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

import static com.cie.btp.BtpConsts.BTP_DEVICE_MAC_ID;


public class PrinterDemo extends LlFragment {
    private static android.app.FragmentManager fragMgr;
    private Fragment mContent = null;
    public static ToggleButton tbPrinter;
    private static final int REQUEST_ENABLE_BT = 2;
    private Messenger mService = null;
    private BluetoothAdapter mAdapter;
    private String mBtpDevice = "";
    private SharedPreferences mSp;
    private EditText etQRcode;
    public final static int WIDTH=360;
    public final static int WHITE=0xffffffff;
    public final static int BLACK=0xff000000;
    String sBillData = "{\"order_state\":\"confirmed\",\"invoice_id\":null,\"line_items\":[{\"items\":" +
            "[{\"product_id\":\"FRTVBT4MR3BQ\",\"id\":13187,\"product_name\":\"24 Mantra Juice - Apple, Organic 1 L Carton" +
            "\",\"image_url\":\"http://d2ak9kvjyosd3u.cloudfront.net/Catalog/LocalEnv/Images/FRTVBT4MR3BQ_sma.jpg\"," +
            "\"quantity\":20,\"amount\":4480,\"state\":\"scheduled\",\"price\":224},{\"product_id\":\"FRT0IP868K4C\"," +
            "\"id\":13188,\"product_name\":\"American Garden Vinegar Juice - Grapes 473 ml Bottle\",\"image_url\":" +
            "\"http://d2ak9kvjyosd3u.cloudfront.net/Catalog/LocalEnv/Images/FRT0IP868K4C_small.png\",\"quantity\":6," +
            "\"amount\":984,\"state\":\"scheduled\",\"price\":164},{\"product_id\":\"FRTS1FCAQGD5\",\"id\":13184," +
            "\"product_name\":\"24 Letter Mantra Juice - Guava 1 L\",\"image_url\":null,\"quantity\":9,\"amount" +
            "\":1611,\"state\":\"scheduled\",\"price\":179},{\"product_id\":\"FRTJFXZJPA01\",\"id\":13189,\"product_name" +
            "\":\"Appy 250 ml\",\"image_url\":\"http://d2ak9kvjyosd3u.cloudfront.net/Catalog/LocalEnv/Images/FRTJFXZJPA01_small.jpg" +
            "\",\"quantity\":16,\"amount\":4032,\"state\":\"scheduled\",\"price\":252},{\"product_id\":\"FRTV9T48MXJ6\"," +
            "\"id\":13186,\"product_name\":\"24 Mantra Juice - Mango, Organic 1 L Carton\",\"image_url\":" +
            "\"http://d2ak9kvjyosd3u.cloudfront.net/Catalog/LocalEnv/Images/FRTV9T48MXJ6_small.jpg\",\"quantity\":1," +
            "\"amount\":274,\"state\":\"scheduled\",\"price\":274}],\"category\":\"Fruit_juice\"},{\"items\":[{\"product_id" +
            "\":\"TDPE0I459BAF\",\"id\":13191,\"product_name\":\"Revlon Charlie Perfumed Body Spray - Neon Chic 150 ml\"," +
            "\"image_url\":\"http://d2ak9kvjyosd3u.cloudfront.net/Catalog/LocalEnv/Images/TDPE0I459BAF_small.jpg\"," +
            "\"quantity\":13,\"amount\":2639,\"state\":\"scheduled\",\"price\":203},{\"product_id\":\"TDP52PM2F2RJ\"," +
            "\"id\":13190,\"product_name\":\"Revlon Charlie Deodorant - True Blue, men 120 ml\",\"image_url\":null," +
            "\"quantity\":18,\"amount\":2718,\"state\":\"scheduled\",\"price\":151}],\"category\":\"Talc_deo\"},{\"items" +
            "\":[{\"product_id\":\"CRLR1ZKB803N\",\"id\":13185,\"product_name\":\"Kellogg's Corn Flakes - Real Almond Honey 300 gm" +
            "\",\"image_url\":\"http://d2ak9kvjyosd3u.cloudfront.net/Catalog/LocalEnv/Images/CRLR1ZKB803N_small.jpg\",\"quantity" +
            "\":10,\"amount\":1200,\"state\":\"scheduled\",\"price\":120}],\"category\":\"Cereal\"}],\"customer_name\":" +
            "\"Devansh\",\"customer_order_no\":100811,\"time_to_rts\":0,\"status\":\"SUCCESS\"}";
    private com.cie.cieprinter.app.BluetoothDeviceList mDialogLayout = com.cie.cieprinter.app.BluetoothDeviceList.INSTANCE;
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v= inflater.inflate(R.layout.printer_status, container, false);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        Bill b = new Bill();
        b.fromString(sBillData);
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
                mDialogLayout.createDialog1(getActivity());
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
                    Bitmap bitmap = encodeAsBitmap(data, BarcodeFormat.CODE_128, 384, 100);
                    MainActivity.mBtp.printDirect(bitmap, true, 0);
                }catch (NumberFormatException nfe){
                    Toast.makeText(getActivity(), "Enter Numeric Number to print barcode",
                            Toast.LENGTH_SHORT).show();
                } catch (WriterException e) {
                    e.printStackTrace();

                }
               /* PrintCmds cmt = new PrintCmds();
                boolean t = cmt.printBarcode(txt);
                if (t) {
                    byte[] d = cmt.PrintCmd();
                    mListener.onAppSignal(AppConsts.PRINT, d);
                } else {
                    Toast.makeText(getActivity(), "Please enter the barcode data between 1 to 11", Toast.LENGTH_SHORT).show();
                }*/

            }
        });
        Button btnQRCode = (Button) v.findViewById(R.id.codePrint);
        btnQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String data = etQRcode.getText().toString();
                    Bitmap bitmap = encodeAsBitmap(data);
                    MainActivity.mBtp.printDirect(bitmap, true, 0);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }
        });
        Button btnTestBill = (Button) v.findViewById(R.id.print_bill);
        btnTestBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mListener.onAppSignal(AppConsts.PRINT_BILL);
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


    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int img_width, int img_height) throws WriterException {
        String contentsToEncode = contents;
        if (contentsToEncode == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        String encoding = guessAppropriateEncoding(contentsToEncode);
        if (encoding != null) {
            hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
            result = writer.encode(contentsToEncode, format, img_width, img_height, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }


}
