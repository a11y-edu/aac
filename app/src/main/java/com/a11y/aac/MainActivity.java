package com.a11y.aac;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsConnectionListener;
import com.movesense.mds.MdsException;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static Mds mMds;
    public static String connectedSerial;
    private static RxBleClient mBleClient;
    private Disposable mScanSubscription;
    private static final String PREFS_NAME = "ConnectedDevices";
    private static final String PREFS_KEY_DEVICES = "devices";

    private final ArrayList<MyScanResult> mPreviousDeviceArrayList = new ArrayList<>();
    private ArrayAdapter<MyScanResult> mPreviousDeviceAdapter;

    private final ArrayList<MyScanResult> mNewDeviceArrayList = new ArrayList<>();
    private ArrayAdapter<MyScanResult> mNewDeviceAdapter;
    private Button connectButton, homeButton, disconnectButton;
    ListView mScanResultListView, mPreviouslyConnectedListView;
    private final ArrayList<MyScanResult> selectedDevices = new ArrayList<>();
    private boolean scanning = false;
    private boolean permissionsGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI elements
        connectButton = findViewById(R.id.connectButton);
        homeButton = findViewById(R.id.homeButton);
        mScanResultListView = findViewById(R.id.newDevicesListView);
        mPreviouslyConnectedListView = findViewById(R.id.previouslyConnectedListView);
        disconnectButton = findViewById(R.id.disconnectButton);

        // Bypass to home activity
        // NOTE: This activity used to be the main activity, but it was replaced by HomeActivity
        // So this code is here to redirect the user to the HomeActivity
        // The user will never see this activity
        // All the code in this activity is not used
        // The code is here just in case we need to use it in the future
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (hasPermissions(this, permissions)) {
            initializeBluetoothFeatures(); // Initialize Bluetooth operations after permissions are granted
        }
    }

    public void createDirectories() {
        File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File rawData = new File(directory, "rawData");
        File trimmedData = new File(directory, "trimmedData");
        File models = new File(directory, "models");
        File rawVideos = new File(directory, "rawVideos");

        if (!rawData.exists()) {
            rawData.mkdirs();
        }

        if (!trimmedData.exists()) {
            trimmedData.mkdirs();
        }

        if (!models.exists()) {
            models.mkdirs();
        }

        if (!rawVideos.exists()) {
            rawVideos.mkdirs();
        }
    }

    /**
     * Load previously connected devices
     * Get the previously connected devices from the SharedPreferences
     * Convert the JSON strings to MyScanResult objects
     * Add the devices to the mPreviousDeviceArrayList
     * Notify the mPreviousDeviceAdapter
     */
    private void loadPreviouslyConnectedDevices() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> myScanResultJsonSet = prefs.getStringSet(PREFS_KEY_DEVICES, new HashSet<>());

        Gson gson = new Gson();
        for (String myScanResultJson : myScanResultJsonSet) {
            MyScanResult myScanResult = gson.fromJson(myScanResultJson, MyScanResult.class);
            if (myScanResult != null) {
                MyScanResult result = new MyScanResult(myScanResult, true);
                mPreviousDeviceArrayList.add(result);
            }
        }

        mPreviousDeviceAdapter.notifyDataSetChanged();
    }


    /**
     * Initialize the previously connected adapter
     * Sets the adapter for the mPreviouslyConnectedListView
     * Sets the onItemClickListener for the mPreviouslyConnectedListView
     * Sets padding for the TextView inside the default layout
     * Notifies the mPreviousDeviceAdapter
     * Logs any errors
     */
    private void initPreviouslyConnectedAdapter() {
        mPreviousDeviceAdapter = new ArrayAdapter<MyScanResult>(this, android.R.layout.simple_list_item_1, mPreviousDeviceArrayList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // Apply padding to the TextView inside the default layout
                TextView textView = view.findViewById(android.R.id.text1);
                float newSizeInSP = 23;
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, newSizeInSP);
                textView.setPadding(20, 30, 20, 30); // Set padding in pixels (left, top, right, bottom)

                return view;
            }
        };
        mPreviouslyConnectedListView.setAdapter(mPreviousDeviceAdapter);
        mPreviouslyConnectedListView.setOnItemClickListener(this);
    }


    /**
     * Initialize the scan result adapter
     * Sets the adapter for the mScanResultListView
     * Sets the onItemClickListener for the mScanResultListView
     * Sets padding for the TextView inside the default layout
     * Notifies the mNewDeviceAdapter
     * Logs any errors
     */
    private void initMScanResAdapter() {
        mNewDeviceAdapter = new ArrayAdapter<MyScanResult>(this, android.R.layout.simple_list_item_1, mNewDeviceArrayList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // Apply padding to the TextView inside the default layout
                TextView textView = view.findViewById(android.R.id.text1);
                float newSizeInSP = 23;
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, newSizeInSP);
                textView.setPadding(20, 30, 20, 30); // Set padding in pixels (left, top, right, bottom)

                return view;
            }
        };
        mScanResultListView.setAdapter(mNewDeviceAdapter);
        mScanResultListView.setOnItemClickListener(this);
    }

    private void initializeBluetoothFeatures() {
        // Check if the necessary permissions are granted before proceeding with Bluetooth operations
        if (hasBluetoothPermissions()) {
            // Initialize all Bluetooth features here
            createDirectories();
            initMds();
            initMScanResAdapter();
            initPreviouslyConnectedAdapter();
            loadPreviouslyConnectedDevices();
        }
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (API level 31) and above
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            // For Android versions below 12
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }



    /**
     * Get the RxBleClient object
     * If the object is null, create a new RxBleClient object
     *
     * @return: The RxBleClient object
     */
    private RxBleClient getBleClient() {
        if (mBleClient == null) {
            mBleClient = RxBleClient.create(this);
        }

        return mBleClient;
    }


    /**
     * Initialize the Mds object
     */
    private void initMds() {
        mMds = Mds.builder().build(this);
    }


    /**
     * Check if the app has the necessary permissions
     * If the app does not have the necessary permissions, return false
     * If the app has the necessary permissions, return true
     * Log the permissions that are granted and not granted
     */
    private boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permission not granted: " + permission);
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Request permissions needed for the app to function
     * Request the necessary permissions
     * Call hasPermissions() to confirm check if the app has the necessary permissions
     * Return true if the app has the necessary permissions
     */
    private boolean requestNeededPermissions() {
        int PERMISSION_ALL = 1;

        // Permissions needed for the app to function
        String[] PERMISSIONS = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
        };

        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);

        // Call hasPermissions() to confirm check if the app has the necessary permissions
        return hasPermissions(this, PERMISSIONS);
    }


    /**
     * Start scanning for BLE devices
     * Clears the mNewDeviceArrayList and notifies the mNewDeviceAdapter
     * Subscribes to the scanBleDevices method of the RxBleClient
     * Adds the scanned devices to the mNewDeviceArrayList
     * Notifies the mNewDeviceAdapter
     * Logs any errors
     */
    public void startScan() {
        connectButton.setText("Scanning");
        mNewDeviceArrayList.clear();
        mNewDeviceAdapter.notifyDataSetChanged();

        mScanSubscription = getBleClient().scanBleDevices(new ScanSettings.Builder().build()).subscribe(scanResult -> {
            Log.d(LOG_TAG, "scanResult: " + scanResult);

            if (scanResult.getBleDevice() != null && scanResult.getBleDevice().getName() != null && scanResult.getBleDevice().getName().startsWith("Movesense")) {

                MyScanResult msr = new MyScanResult(scanResult);
                // Check if the device already exists in mPreviousDeviceArrayList
                boolean existsInPrevious = false;
                for (MyScanResult prevDevice : mPreviousDeviceArrayList) {
                    if (prevDevice.macAddress.equals(msr.macAddress) && prevDevice.name.equals(msr.name)) {
                        existsInPrevious = true;
                        break;
                    }
                }

                // If the device doesn't exist in mPreviousDeviceArrayList, add it to mNewDeviceArrayList
                if (!existsInPrevious) {
                    if (mNewDeviceArrayList.contains(msr))
                        mNewDeviceArrayList.set(mNewDeviceArrayList.indexOf(msr), msr);
                    else mNewDeviceArrayList.add(0, msr);

                    mNewDeviceAdapter.notifyDataSetChanged();
                }
            }
        }, throwable -> Log.e(LOG_TAG, "scan error: " + throwable));
    }


    /**
     * Stop scanning for BLE devices
     * Disposes the mScanSubscription
     */
    public void stopScan() {
        if (mScanSubscription != null) {
            mScanSubscription.dispose();
            mScanSubscription = null;
        }
    }


    /**
     * onItemClick method for the mScanResultListView
     * Connects to the BLE device that was clicked
     * If the device is not connected, connect to the device
     *
     * @param parent: The AdapterView where the click happened
     * @param view: The view within the AdapterView that was clicked
     * @param position: The position of the view in the adapter
     * @param id: The row id of the item that was clicked
     *
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MyScanResult device = null;
        if (parent == mScanResultListView) {
            // Get the device that was clicked
            device = mNewDeviceArrayList.get(position);

            // Add the device to the previously connected devices
            addDeviceToPreviouslyConnected(device);
            mPreviousDeviceArrayList.add(0, device);
            mPreviousDeviceAdapter.notifyDataSetChanged();

            // Remove the device from the scan result list
            mNewDeviceArrayList.remove(device);
            mNewDeviceAdapter.notifyDataSetChanged();
        } else if (parent == mPreviouslyConnectedListView) {
            // Get the device that was clicked
            device = mPreviousDeviceArrayList.get(position);
            if (device.isConnected()) {
                return;
            }
        }

        if (device != null) {
            connectBLEDevice(device, true);
        }
    }


    /**
     * Connect to the BLE device
     * Connect to the BLE device using the Mds object
     * If the connection is successful, mark the device as connected
     * Notify the mNewDeviceAdapter
     * Set the connectedSerial
     * Enable the homeButton
     * Set the homeButton background tint
     *
     * @param device: The BLE device to connect to
     */
    public void connectBLEDevice(MyScanResult device, boolean newConnection) {
        // Toast.makeText(getApplicationContext(), "Connecting, this may take a moment...", Toast.LENGTH_SHORT).show();

        RxBleDevice bleDevice = getBleClient().getBleDevice(device.macAddress);

        Log.i(LOG_TAG, "Connecting to BLE device: " + bleDevice.getMacAddress());

        stopScan();
        mMds.connect(bleDevice.getMacAddress(), new MdsConnectionListener() {
            @Override
            public void onConnect(String s) {
                connectButton.setText("Connecting");
                connectButton.setEnabled(false);
                Log.d(LOG_TAG, "onConnect:" + s);
            }

            @Override
            public void onConnectionComplete(String macAddress, String serial) {
                for (MyScanResult sr : mPreviousDeviceArrayList) {
                    if (sr.macAddress.equalsIgnoreCase(macAddress)) {
                        sr.markConnected(serial);
                        break;
                    }
                }

                mPreviousDeviceAdapter.notifyDataSetChanged();
                connectedSerial = serial;

                connectButton.setText("Scan");

                connectButton.setEnabled(true);
                homeButton.setEnabled(true);

                // Set home button background tint
                homeButton.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.theme));
                selectedDevices.add(device);

                disconnectButton.setEnabled(true);
                disconnectButton.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.theme));
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "onError:" + e);

                connectButton.setEnabled(true);
                connectButton.setText("Scan");
                String msg = "Error connecting to Movesense device. Make sure the device is turned on and in range. If the problem persists, try restarting the app.";
                showConnectionError(msg);
            }

            @Override
            public void onDisconnect(String bleAddress) {
                Log.d(LOG_TAG, "onDisconnect: " + bleAddress);
                for (MyScanResult sr : mNewDeviceArrayList) {
                    if (bleAddress.equals(sr.macAddress)) {
                        sr.markDisconnected();
                    }
                }

                // String msg = "Disconnected from device: " + bleAddress;
                // Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                mPreviousDeviceAdapter.notifyDataSetChanged();
            }
        });
    }


    /**
     * Add the device to the previously connected devices
     * Convert the device to a JSON string
     * Get the previously connected devices from the SharedPreferences
     * Add the device to the deviceSet
     * Save the deviceSet to the SharedPreferences
     *
     * @param device: The device to add to the previously connected devices
     */
    private void addDeviceToPreviouslyConnected(MyScanResult device) {
        Gson gson = new Gson();
        String scanResultJson = gson.toJson(device);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> deviceSet = prefs.getStringSet(PREFS_KEY_DEVICES, new HashSet<>());

        Set<String> newDeviceSet = new HashSet<>(deviceSet);

        newDeviceSet.add(scanResultJson);

        prefs.edit().putStringSet(PREFS_KEY_DEVICES, newDeviceSet).apply();
    }


    /**
     * Disconnect the BLE device
     * Disconnect the BLE device using the Mds object
     * If the device is connected, disconnect the device
     */
    public void disconnectBLEDevice() {
        for (MyScanResult selectedDevice : selectedDevices) {
            if (selectedDevice != null && selectedDevice.isConnected()) {
                Log.d(LOG_TAG, "Disconnecting from device: " + selectedDevice.macAddress);

                if (mMds != null)
                    mMds.disconnect(selectedDevice.macAddress);

                selectedDevice.markDisconnected();
                Log.d(LOG_TAG, "Disconnected successfully.");
            } else {
                Log.d(LOG_TAG, "Cannot disconnect. mMds is null or selectedDevice is not connected.");
            }
        }
        selectedDevices.clear();
        disconnectButton.setEnabled(false);
        disconnectButton.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.theme2));
        homeButton.setEnabled(false);
        homeButton.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.theme2));
    }


    /**
     * Show an alert dialog with the error message
     *
     * @param e: The MdsException object
     */
    private void showConnectionError(MdsException e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Connection Error:").setMessage(e.getMessage());

        builder.create().show();
    }


    /**
     * Show an alert dialog with the error message
     *
     * @param msg: The error message
     */
    private void showConnectionError(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Connection Error:").setMessage(msg);

        builder.create().show();
    }


    /**
     * Called when the connect button is clicked
     *
     * @param view: The view that was clicked
     */
    public void connectButtonClicked(View view) {
        // Check if the required permissions are granted
        if (!permissionsGranted) {
            permissionsGranted = requestNeededPermissions();
            System.out.println("Permissions granted: " + permissionsGranted);
        } else {
            scanning = !scanning;
            if(scanning) {
                homeButton.setEnabled(false);
                homeButton.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.theme2));
                startScan();
            } else {
                stopScan();
                connectButton.setText("Scan");
            }
        }
    }


    /**
     * Called when the home button is clicked
     *
     * @param view: The view that was clicked
     */
    public void homeButtonClicked(View view) {
        // Go to the HomeActivity
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }


    /**
     * Called when the disconnect button is clicked
     *
     * @param view: The view that was clicked
     */
    public void disconnectButtonClicked(View view) {
        disconnectBLEDevice();
    }


    /**
     * Called when the app is started
     * Get the BLE client
     * Print the number of connected peripherals
     */
    @Override
    protected void onStart() {
        super.onStart();
        mBleClient = getBleClient();
        System.out.println(mBleClient.getConnectedPeripherals().size());
    }


    /**
     * Called when the app is destroyed
     * Disconnect any BLE device that is connected
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnectBLEDevice();
    }
}