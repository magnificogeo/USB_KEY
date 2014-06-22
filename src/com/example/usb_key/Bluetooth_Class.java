package com.example.usb_key;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by george on 23/6/14.
 */
public class Bluetooth_Class {

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket btSocket;
    BluetoothDevice btDevice;

    // Class constructor
    public void Bluetooth_Class() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

}
