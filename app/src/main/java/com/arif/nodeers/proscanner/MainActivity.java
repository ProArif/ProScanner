package com.arif.nodeers.proscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CameraView cameraView;
    private boolean isDetected = false;
    private FirebaseVisionBarcodeDetectorOptions options;
    private FirebaseVisionBarcodeDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera_view);
        cameraView.setLifecycleOwner(this);

        options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_ALL_FORMATS)
                .build();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);

        Dexter.withActivity(this).withPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO}).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                setUpCamera();
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

            }
        }).check();

    }

    private void setUpCamera() {
        cameraView.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {

                processImage(getImage(frame));
            }
        });
    }

    private void processImage(FirebaseVisionImage visionImage){
        if (!isDetected){
            detector.detectInImage(visionImage)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {

                            processResult(firebaseVisionBarcodes);

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void processResult(List<FirebaseVisionBarcode> barcodes){
        if (barcodes.size() > 0 ){
            for (FirebaseVisionBarcode visionBarcode: barcodes){
                int value_type = visionBarcode.getValueType();

                switch (value_type){
                    case FirebaseVisionBarcode.TYPE_TEXT:

                    case FirebaseVisionBarcode.FORMAT_QR_CODE:
                        showDialog(visionBarcode.getRawValue());
                        break;

                    case FirebaseVisionBarcode.TYPE_URL:
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(visionBarcode.getRawValue())));
                        break;

                    case FirebaseVisionBarcode.TYPE_CONTACT_INFO:
                        String info = "Name: " +
                                visionBarcode.getContactInfo().getName().getFormattedName() +
                                "\n" +
                                "Address: " +
                                visionBarcode.getContactInfo().getAddresses().get(0).getAddressLines()[0] +
                                "\n" +
                                "Email: " +
                                visionBarcode.getContactInfo().getEmails().get(0).getAddress() +
                                "\n" +
                                "Phone Number: " +
                                visionBarcode.getContactInfo().getPhones().get(0).getNumber();
                        showDialog(info);
                        break;

                    case FirebaseVisionBarcode.TYPE_PRODUCT:
                        if (visionBarcode.getContactInfo() != null){
                        String infoProduct = "Name: " +
                                visionBarcode.getContactInfo().getTitle() +
                                visionBarcode.getContactInfo().getOrganization();
                        showDialog(infoProduct);}
                        break;

                    case FirebaseVisionBarcode.FORMAT_AZTEC:
                        showDialog(visionBarcode.getRawValue());
                        break;

                        default:
                            showDialog("No barcode detected");
                            break;


                }
            }
        }
    }

    private void showDialog(String rawValue) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(rawValue)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        dialogInterface.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private FirebaseVisionImage getImage(Frame frame){
        byte [] data = frame.getData();
        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setHeight(frame.getSize().getHeight())
                .setWidth(frame.getSize().getWidth())
                .build();
        return FirebaseVisionImage.fromByteArray(data,metadata);
    }
}
