package com.example.usb_key;

import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.content.DialogInterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by george on 3/7/14.
 */
public class UsbKeySearch extends Activity {

    ImageView signalStatus;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usbkeysearch_layout);

        signalStatus = (ImageView) findViewById(R.id.signalStatus);

        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    @Override
    protected void onDestroy() {

        // TODO Auto-generated method stub
        super.onDestroy();

    }

    private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {

            // TODO Auto-generated method stub
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);

                if ( device.getAddress().equals(MainActivity.MAC_ADDR) ) {

                        Log.d("George_debug", "Main Acitivity RSSI is " + MainActivity.get_rssi_value());

                   if ( MainActivity.get_rssi_value() <= 40) {
                       signalStatus.setBackgroundResource(R.drawable.signal_four);
                   } else if ( MainActivity.get_rssi_value() > 40 && MainActivity.get_rssi_value() <= 70) {
                       signalStatus.setBackgroundResource(R.drawable.signal_three);
                   } else {
                       signalStatus.setBackgroundResource(R.drawable.signal_zero);
                   }

                }
            }
        }
    };
}