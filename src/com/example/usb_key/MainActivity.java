package com.example.usb_key;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.os.Vibrator;

public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    protected static final String TAG = "george_debug";
    //protected static final String MAC_ADDR = "00:13:EF:00:08:F7";
    protected static final String MAC_ADDR = "78:9E:D0:64:07:A8";
    protected static int bluetooth_found_status = 0;
    protected static short bluetooth_found_distance = 0;

    ListView listDevicesFound;
    Button btnScanDevice;
    TextView btconnectionstate;
    TextView stateBluetooth;
    BluetoothAdapter bluetoothAdapter;

    ArrayAdapter<String> btArrayAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnScanDevice = (Button)findViewById(R.id.scandevice);

        stateBluetooth = (TextView)findViewById(R.id.bluetoothstate);
        btconnectionstate = (TextView)findViewById(R.id.btconnectedstate);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        listDevicesFound = (ListView)findViewById(R.id.devicesfound);
        btArrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
        listDevicesFound.setAdapter(btArrayAdapter);

        CheckBlueToothState();

        btnScanDevice.setOnClickListener(btnScanDeviceOnClickListener);
        //btnPingDevice.setOnClickListener(btnPingDeviceOnClickListener);

        /* Register broadcast receivers here */
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(ActionDiscoveryFinishedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        //registerReceiver(ActionDiscoveryStartedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiver(ActionFoundReceiver);
    }

    private void CheckBlueToothState(){
        if (bluetoothAdapter == null){
            stateBluetooth.setText("Bluetooth NOT supported");
        }else{
            if (bluetoothAdapter.isEnabled()){
                if(bluetoothAdapter.isDiscovering()){
                    stateBluetooth.setText("Bluetooth is currently in device discovery process.");
                }else{
                    stateBluetooth.setText("Bluetooth is Enabled.");
                    btnScanDevice.setEnabled(true);
                }
            }else{
                stateBluetooth.setText("Bluetooth is NOT Enabled!");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private Button.OnClickListener btnScanDeviceOnClickListener
            = new Button.OnClickListener(){

        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
            btArrayAdapter.clear();
            bluetoothAdapter.startDiscovery();
        }};


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if(requestCode == REQUEST_ENABLE_BT){
            CheckBlueToothState();
        }
    }

    private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();

            Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();


            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);;
                if ( device.getAddress().equals(MAC_ADDR)) {

                    Log.v(TAG, "Detected USB_KEY" );
                    Log.v(TAG, "RSSI is " + rssi );
                    bluetooth_found_distance = rssi;
                    Toast.makeText(getApplicationContext(), "USB_KEY detected", Toast.LENGTH_SHORT).show();
                    btconnectionstate.setTextColor(R.color.opaque_green);
                    btconnectionstate.setText("CONNECTED");
                    bluetooth_found_status = 1;

                } else {

                    Log.v(TAG, "USB_KEY is out of range or cannot be detected");
                    Toast.makeText(getApplicationContext(), "USB_KEY is out of range or cannot be detected", Toast.LENGTH_SHORT).show();

                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                    btconnectionstate.setTextColor(R.color.opaque_red);
                    btconnectionstate.setText("DISCONNECTED");

                }
                //btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                btArrayAdapter.notifyDataSetChanged();
            }


        }};

    private final BroadcastReceiver ActionDiscoveryFinishedReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();



            if ( bluetooth_found_status == 0 ) {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(5000);
                Toast.makeText(getApplicationContext(), "USB_KEY is either out of range or cannot be detected! It's last detected distance is " + bluetooth_found_distance, Toast.LENGTH_SHORT).show();
            }

            if ( bluetooth_found_status == 1 ) {
                bluetooth_found_status = 0; // reset found flag
            }

            bluetoothAdapter.startDiscovery(); // reloop discovery

        }};

}