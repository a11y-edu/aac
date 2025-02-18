package com.a11yEdu.a11y;

import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.scan.ScanResult;

class MyScanResult {
    public int rssi;
    public String macAddress;
    public String name;
    public String connectedSerial;
    public boolean previouslyConnected;

    public MyScanResult(ScanResult scanResult) {
        this.macAddress = scanResult.getBleDevice().getMacAddress();
        this.rssi = scanResult.getRssi();
        this.name = scanResult.getBleDevice().getName();
        this.previouslyConnected = false;
    }

    public MyScanResult(MyScanResult myScanResult, boolean previouslyConnected) {
        this.macAddress = myScanResult.macAddress;
        this.rssi = myScanResult.rssi;
        this.name = myScanResult.name;
        this.previouslyConnected = previouslyConnected;
    }

    public boolean isConnected() {
        return connectedSerial != null;
    }

    public void markConnected(String serial) {
        connectedSerial = serial;
    }

    public void markDisconnected() {
        connectedSerial = null;
    }

    public boolean equals(Object object) {
        if(object instanceof MyScanResult && ((MyScanResult)object).macAddress.equals(this.macAddress)) {
            return true;
        }
        else return object instanceof RxBleDevice && ((RxBleDevice) object).getMacAddress().equals(this.macAddress);
    }

    @NonNull
    public String toString() {
        return (isConnected() ? "Connected: " : "") + name;
        /*
        return (isConnected() ? "*** " : "") + macAddress + " - " + name + " [" + rssi + "]" + (isConnected()?" ***":"");
        */
    }
}
