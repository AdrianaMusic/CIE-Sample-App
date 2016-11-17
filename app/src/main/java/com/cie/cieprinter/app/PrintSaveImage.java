
package com.cie.cieprinter.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.cie.btp.DebugLog;
import com.cie.btp.ImageFactory;
import com.cie.cieprinter.R;
import com.cie.cieprinter.loopedlabs.LlFragment;
import com.cie.cieprinter.loopedlabs.selector.FileOperation;
import com.cie.cieprinter.loopedlabs.selector.FileSelector;
import com.cie.cieprinter.loopedlabs.selector.OnHandleFileListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrintSaveImage extends LlFragment {
    private int indexNumber;
    private TextView txtView;
    final String[] mFileFilter = {".png", ".bmp", ".jpeg", ".jpg"};
    private String[] index = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private int imageAlignment = 0;
    private boolean bImgAlgoGrayScale = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.print_save_image, container, false);
        MainActivity.printerSelection();
        initControls(view);
        Spinner indexValue = (Spinner) view.findViewById(R.id.index);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_item, index);
        indexValue.setAdapter(adapter);
        AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> aAdapter, View aView, int arg2, long arg3) {
                TextView textViewItem = (TextView) aView;
                try {
                    indexNumber = Integer.valueOf(textViewItem.getText().toString());
                }
                catch (NullPointerException e) {
                    DebugLog.logException(e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        };
        indexValue.setOnItemSelectedListener(onItemSelectedListener);

        RadioGroup rgImgAlign = (RadioGroup) view.findViewById(R.id.rgImgAlign);
        rgImgAlign.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.rbLeftAlign:
                        imageAlignment = 0;
                        break;
                    case R.id.rbCenterAlign:
                        imageAlignment = 1;
                        break;
                    case R.id.rbRightAlign:
                        imageAlignment = 2;
                        break;
                }
            }
        });

        final TableRow tr1 = (TableRow) view.findViewById(R.id.tr1);

        RadioGroup rgImgAlgo = (RadioGroup) view.findViewById(R.id.rgImgAlgo);
        rgImgAlgo.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.rbBinarize:
                        bImgAlgoGrayScale = false;
                        tr1.setVisibility(View.VISIBLE);
                        break;
                    case R.id.rbGrayscale:
                        bImgAlgoGrayScale = true;
                        tr1.setVisibility(View.GONE);
                        break;
                }
                previewImage(threshold);
            }
        });

        Button SaveImage = (Button) view.findViewById(R.id.saveImage);
        SaveImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (fileUri.getPath() == null) {
                        Toast.makeText(getActivity(), "No Logo Selected",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Boolean Invert;
                    Invert = !bInvertBitmap;
                    boolean r = MainActivity.mPrinter.saveImage(fileUri.getPath(), Invert, threshold, indexNumber);
                    if (r) {
                        Toast.makeText(getActivity(), "Image saved on index " + indexNumber,
                                Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getActivity(), MainActivity.mPrinter.getPrinterStatusMessage(), Toast.LENGTH_SHORT).show();
                    }

                }
                catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Enter Index for save Image",
                            Toast.LENGTH_SHORT).show();

                }
                catch (NullPointerException e) {
                    DebugLog.logException(e);
                }
            }
        });
        Button printSavedImage = (Button) view.findViewById(R.id.printImage);
        printSavedImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.mPrinter.printSavedImage(indexNumber);
            }
        });
        Button printDirect = (Button) view.findViewById(R.id.printDirect);
        printDirect.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    if (fileUri.getPath() == null) {
                        Toast.makeText(getActivity(), "No Logo Selected",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Boolean Invert;
                    Invert = !bInvertBitmap;
                    DebugLog.logTrace("A : " + bIgnoreAlpha + " I : " + Invert + " T : " + threshold);

                    boolean r;
                    if (bImgAlgoGrayScale) {
                        r = MainActivity.mPrinter.printGrayScaleImage(fileUri.getPath(), imageAlignment);
                    }
                    else {
                        r = MainActivity.mPrinter.printBinarizedImage(fileUri.getPath(), Invert, threshold, imageAlignment);
                    }

                    if (r) {
                        Toast.makeText(getActivity(), "Image Printed", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getActivity(), MainActivity.mPrinter.getPrinterStatusMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                catch (NullPointerException e) {
                    DebugLog.logException(e);
                }
            }
        });
        return view;
    }

    private ImageView imgPreviewAsIs, imgLogoPreview;


    private Uri fileUri;
    // file url to store captured image

    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    // directory name to store captured images and videos
    private static final String IMAGE_DIRECTORY_NAME = "BTP";
    private static final int RESULT_LOAD_IMAGE = 99;
    private static final int MIN_IMG_SIZE = 8;

    private int ivAsIsHeight = 0;
    private int ivAsIsWidth = 0;

    private int ivLogoPreviewHeight = 0;
    private int ivlogoPreviewWidth = 0;

    private TextView tvAsIsSize, tvPrintSize, tvlogoSize;

    private SeekBar seekBar;

    private boolean bInvertBitmap = false;
    private boolean bIgnoreAlpha = true;
    private FileSelector mFileSel;

    private int threshold = 127;

    /**
     * Receiving activity result method will be called after closing the camera
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // successfully captured the image
                // display it in image view
                previewCapturedImage();
                previewImage(0);
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getActivity(), R.string.img_capture_cancelled,
                        Toast.LENGTH_SHORT).show();
            }
            else {
                // failed to capture image
                Toast.makeText(getActivity(), R.string.img_capture_failed,
                        Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == RESULT_LOAD_IMAGE
                && resultCode == Activity.RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            fileUri = Uri.parse(picturePath);
            previewCapturedImage();
            previewImage(0);
        }
    }

    private void previewCapturedImage() {
        if (!isImageSizeOK()) {
            Toast.makeText(getActivity(), R.string.img_too_small, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        try {
            // bitmap factory
            BitmapFactory.Options options = new BitmapFactory.Options();
            int inSampleSize = 1;

            // downsize image for larger images

            if (orgImgHeight > ivAsIsHeight || orgImgWidth > ivAsIsWidth) {
                if (orgImgWidth > orgImgHeight) {
                    inSampleSize = Math.round((float) orgImgHeight
                            / (float) ivAsIsHeight);
                }
                else {
                    inSampleSize = Math.round((float) orgImgWidth
                            / (float) ivAsIsWidth);
                }
            }

            tvAsIsSize.setText(orgImgWidth + "x" + orgImgHeight);

            options.inSampleSize = inSampleSize;

            final Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(),
                    options);


            float ratio = (float) bitmap.getWidth()
                    / (float) bitmap.getHeight();

            int imgX = ivAsIsWidth;
            int imgY = (int) (ivAsIsWidth / ratio);

            if (imgY > ivAsIsHeight) {
                imgY = ivAsIsHeight;
                imgX = (int) (ratio * ivAsIsHeight);
            }

            imgPreviewAsIs.setImageBitmap(Bitmap.createScaledBitmap(bitmap,
                    imgX, imgY, false));

        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private int orgImgHeight = 0;
    private int orgImgWidth = 0;

    private boolean isImageSizeOK() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileUri.getPath(), options);
        orgImgHeight = options.outHeight;
        orgImgWidth = options.outWidth;

        return !((orgImgHeight < MIN_IMG_SIZE) || (orgImgWidth < MIN_IMG_SIZE));
    }

    /**
     * Capturing Camera Image will launch camera app request image capture
     */
    public void dialogBox() {
        CharSequence storage[] = new CharSequence[]{"Select from Gallery", "Select from SD Card"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Pick a storage");
        builder.setItems(storage, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Intent i = new Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                        startActivityForResult(i, RESULT_LOAD_IMAGE);
//						loadImage(i);
                        break;
                    case 1:
                        mFileSel = new FileSelector(getActivity(), FileOperation.LOAD, mLoadFileListener, mFileFilter);
                        mFileSel.show();
                        break;
                }
            }

        });
        builder.show();
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        fileUri = getOutputMediaFileUri();

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    /**
     * Creating file uri to store image/video
     */
    public Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }

    /**
     * returning image / video
     */
    private File getOutputMediaFile() {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
                        + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
                        + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }
        return mediaFile;
    }

    private void previewImage(int threshold) {
        try {
            Bitmap b = ImageFactory.LoadImage(fileUri.getPath());
            if (b == null) {
                return;
            }
            int iLogoWidth = b.getWidth();
            int iLogoHeight = b.getHeight();
            DebugLog.logTrace("invert  " + bInvertBitmap);

            if (threshold == 0) {
                this.threshold = ImageFactory.getThreshold();
                seekBar.setProgress((this.threshold * 100) / 255);
            }
            else {
                this.threshold = threshold;
            }

            tvPrintSize.setText(iLogoWidth + "x" + iLogoHeight);

            tvlogoSize.setText(Integer.toString((iLogoWidth * iLogoHeight) / 16));

            float ratio = (float) b.getWidth() / (float) b.getHeight();

            int imgX = ivlogoPreviewWidth;
            int imgY = (int) (ivlogoPreviewWidth / ratio);

            if (imgY > ivLogoPreviewHeight) {
                imgY = ivLogoPreviewHeight;
                imgX = (int) (ratio * ivLogoPreviewHeight);
            }

            Bitmap bImg = Bitmap.createScaledBitmap(b, imgX, imgY, false);

            if (bImgAlgoGrayScale) {
                imgLogoPreview.setImageBitmap(ImageFactory.ditherBitmap(bImg));
            }
            else {
                imgLogoPreview.setImageBitmap(ImageFactory.BinarizeImage(bImg, true,
                        bInvertBitmap, threshold));
            }
        }
        catch (NullPointerException e) {
            DebugLog.logException(e);
        }
    }

    private void initControls(View v) {
        imgPreviewAsIs = (ImageView) v.findViewById(R.id.imgPreviewAsIs);
        ViewTreeObserver vto1 = imgPreviewAsIs.getViewTreeObserver();
        vto1.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imgPreviewAsIs.getViewTreeObserver().removeOnPreDrawListener(
                        this);
                ivAsIsHeight = imgPreviewAsIs.getMeasuredHeight();
                ivAsIsWidth = imgPreviewAsIs.getMeasuredWidth();
                return true;
            }
        });

        imgLogoPreview = (ImageView) v.findViewById(R.id.imgPreviewBiTonal);
        ViewTreeObserver vto2 = imgLogoPreview.getViewTreeObserver();
        vto2.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imgLogoPreview.getViewTreeObserver().removeOnPreDrawListener(
                        this);
                ivLogoPreviewHeight = imgLogoPreview.getMeasuredHeight();
                ivlogoPreviewWidth = imgLogoPreview.getMeasuredWidth();
                return true;
            }
        });
        Button btnCapturePicture = (Button) v.findViewById(R.id.btnCapturePicture);
        txtView = (TextView) v.findViewById(R.id.textView);
        btnCapturePicture.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // capture picture
                captureImage();
            }
        });

        Button btnFromGallery = (Button) v.findViewById(R.id.btnFromGallery);
        btnFromGallery.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dialogBox();
            }
        });


        tvAsIsSize = (TextView) v.findViewById(R.id.tvAsIsSize);
        tvPrintSize = (TextView) v.findViewById(R.id.tvPrintSize);
        tvlogoSize = (TextView) v.findViewById(R.id.tvlogoSize);

        CheckBox cbInvertBitmap = (CheckBox) v.findViewById(R.id.cbInvertBitmap);
        cbInvertBitmap.setChecked(false);
        bInvertBitmap = false;
        cbInvertBitmap
                .setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        DebugLog.logTrace();
                        bInvertBitmap = isChecked;
                        try {
                            if (fileUri.getPath() == null) {
                                Toast.makeText(getActivity(), "Select Image ", Toast.LENGTH_SHORT).show();

                            }
                            else {
                                previewImage(threshold);
                            }
                        }
                        catch (NullPointerException e) {
                            Toast.makeText(getActivity(), "Select Image ", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        seekBar = (SeekBar) v.findViewById(R.id.seekBar);
        seekBar.setProgress(50);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                progressChanged = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                int t = ((255 / 100) * progressChanged);
                previewImage(t);
                DebugLog.logTrace("o t : " + threshold + "  n t : " + t);
            }
        });

    }

    OnHandleFileListener mLoadFileListener = new OnHandleFileListener() {
        @Override
        public void handleFile(final String filePath) {
            txtView.setText(filePath);
            fileUri = Uri.parse(filePath);
            previewImage(threshold);
            previewCapturedImage();
            mFileSel.dismiss();
        }
    };

    @Override
    public void onResume() {
        MainActivity.printerSelection();
        super.onResume();
    }
}
