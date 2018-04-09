package com.example.kha.myapplication;

//import android.support.v7.app.AppCompatActivity;
//import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.net.*;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.*;
import java.io.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.zip.ZipEntry;

public class MainActivity extends AppCompatActivity {
    private ScanCallback mScanCallback;
    private static final int REQUEST_ENABLE_BT = 0;
    // define this at the very beginning of your class
    private BluetoothAdapter mBluetoothAdapter;
    // Initializes Bluetooth adapter
    final BluetoothManager bluetoothManager =
            (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();

    FirebaseStorage storage = FirebaseStorage.getInstance();
    // Create a storage reference from our app
    StorageReference storageRef = storage.getReference();
    StorageReference ourFileRef = storageRef.child("boardData/testfile.txt");
    private ZipEntry mRecyclerViewAdapter;
    private Object mListener;
    private BluetoothGatt mGatt;
    private Activity activity;
    private BluetoothGattCallback mCallback;
    private BluetoothGattService mMovService;
    private BluetoothGattCharacteristic mEnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if
                (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE
        )) {
            Toast.makeText(this, R.string.ble_not_supported,
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {// because it throws an exception
            uploadResource();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent
            data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScanFragment.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getActivity(), "Bluetooth Disabled",
                        Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
    }

    private void startScan() {
        Log.d("startScan", "scan!");
        if (mRecyclerViewAdapter.getSize() == 0)
            mListener.onShowProgress();
        if (Build.VERSION.SDK_INT < 21) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            // request BluetoothLeScanner if it hasn't been
            initialized yet
            if (mLeScanner == null) mLeScanner =
                    mBluetoothAdapter.getBluetoothLeScanner();
            // start scan in low latency mode
            mLeScanner.startScan(new ArrayList<ScanFilter>(),
                    new
                            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build(),
                    mScanCallback);
        }
    }

    private void connectDevice(String address) {
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getActivity(), R.string.ble_disable,
                    Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        mListener.onShowProgress();
        BluetoothDevice device =
                mBluetoothAdapter.getRemoteDevice(address);
        mGatt = device.connectGatt(getActivity(), false, mCallback);
        Log.d("BLE", "connectDevice");
    }

    private BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int
                status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    // as soon as we're connected, discover services
                    mGatt.discoverServices();
                    Log.d("BLE", "onConnectionStateChange");
                    break;
            }
        }
    }

    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        // as soon as services are discovered, acquire characteristi and try enabling
                mMovService = mGatt.getService(UUID.fromString("02366E80-CF3A-
        11E1-9AB4-0002A5D5C51B"));
        mEnable =
                mMovService.getCharacteristic(UUID.fromString("340A1B80-CF4B-11E1-AC36-
                        0002A5D5C51B"));
        if (mEnable == null) {
            Toast.makeText(getActivity(), R.string.service_not_found,
                    Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
        mGatt.readCharacteristic(mEnable);
        deviceConnected();
    }

    private void deviceConnected() {
    }

    void uploadResource() throws IOException {
        // the text we will upload
        String someTextToUpload = "Hello world! \nthis is to test uploading data";
        //convert the text to bytes
        byte[] file = someTextToUpload.getBytes();
        // Now we need to use the UploadTask class to upload to our cloud
        UploadTask uploadTask = ourFileRef.putBytes(file);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                System.out.println("I failed in uploading the file :(");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>(){
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-typ
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                System.out.println("Successfuly uploaded the file");
            }
        });
    }

    public Activity getActivity() {
        return activity;
    }
}

