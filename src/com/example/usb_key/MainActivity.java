package com.example.usb_key;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.*;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    String btToastLabel;
    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket btSocket;
    BluetoothDevice btDevice;
    OutputStream btOutputStream;
    InputStream btInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    int REQUEST_ENABLE_BT = 1;
    String TAG = "george_debug"; // This is a debug tag used to filter debug messages
    String MAC_ADDR = "00:13:EF:00:08:F7"; // This is BT Bee
    //String MAC_ADDR = "78:9E:D0:64:07:A8"; // This is Galaxy Note 10.1
    int bluetooth_found_status = 0;
    short bluetooth_found_distance = 0;
    int bluetooth_connected_status = 0;

    Button btnConnectDevice;
    TextView stateBluetooth;
    Button btnDisconnectDevice;
    Button btnDecrypt;
    Button btnEncrypt;
    ImageView lockStatus;

    /** Called when the activity is first created. **/
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnectDevice = (Button)findViewById(R.id.connectdev);
        btnDisconnectDevice = (Button)findViewById(R.id.disconnectdev);
        //btnEncrypt = (Button)findViewById(R.id.btnencrypt);
        btnDecrypt = (Button)findViewById(R.id.btndecrypt);



        stateBluetooth = (TextView)findViewById(R.id.bluetoothstate);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        lockStatus = (ImageView) findViewById(R.id.lockStatusView);


        CheckBlueToothState();

        //myTextbox = (EditText)findViewById(R.id.myTextbox);
        btnConnectDevice.setOnClickListener(btnConnectDeviceOnClickListener);
        btnDisconnectDevice.setOnClickListener(btnDisconnectDeviceOnClickListener);
        btnDecrypt.setOnClickListener(btnDecryptOnClickListener);


        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(ActionDiscoveryFinishedReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        bluetoothAdapter.startDiscovery();

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

    }

    private void CheckBlueToothState(){
        if (bluetoothAdapter == null)
        {
            stateBluetooth.setText("Bluetooth is not supported on this device.");
        } else
            {
            if (bluetoothAdapter.isEnabled())
            {
                if(bluetoothAdapter.isDiscovering())
                {
                    stateBluetooth.setText("Bluetooth is currently in device discovery process.");
                } else
                    {
                        stateBluetooth.setText("Bluetooth is Enabled.");
                        btnConnectDevice.setEnabled(true);
                    }
            } else
                {
                    stateBluetooth.setText("Bluetooth is NOT Enabled!");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        }

    private  Button.OnClickListener btnDecryptOnClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View arg0) {

            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

            alert.setTitle("Input your passkey to decrypt");
            //alert.setMessage("Message");

            // Set an EditText view to get user input
            final EditText textToDev = new EditText(MainActivity.this);
            alert.setView(textToDev);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    try {
                        sendData(textToDev);
                    } catch ( Exception io ) {
                        //
                    }
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();

            /*
            try {

                sendData();

            } catch ( IOException io ) {
                btToastLabel = "Unable to send Data, try again";
                Toast.makeText(getApplicationContext(), btToastLabel, Toast.LENGTH_LONG).show();
            }*/

        }
    };

    private Button.OnClickListener btnDisconnectDeviceOnClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View arg0) {

            try {
                closeBT();
            } catch ( IOException io ) {
                btToastLabel = "Unable to Disconnect from USB_KEY";
                Toast.makeText(getApplicationContext(), btToastLabel, Toast.LENGTH_LONG).show();
            }
        }
    };

    private Button.OnClickListener btnConnectDeviceOnClickListener
            = new Button.OnClickListener()
    {

        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
            btToastLabel = "Searching and Connecting...";
            Toast.makeText(getApplicationContext(), btToastLabel, Toast.LENGTH_LONG).show();

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if(pairedDevices.size() > 0)
            {
                for(BluetoothDevice device : pairedDevices)
                {
                    if(device.getAddress().equals(MAC_ADDR))
                    {
                        btDevice = device;
                        break;
                    }
                }
            }

            //ParcelUuid ParcelUuid[];

            //ParcelUuid[] uuids = btDevice.getUuids();

            //for (ParcelUuid uuid: uuids) {
            //    Log.d(TAG, "UUID: " + uuid.getUuid().toString());
            //}
            btToastLabel = "USB_KEY Found";

            Toast.makeText(getApplicationContext(), btToastLabel,Toast.LENGTH_SHORT).show();
            bluetoothAdapter.startDiscovery();


            try {
                initiate_bt_connection();
                btnDisconnectDevice.setEnabled(true);
               // btnEncrypt.setEnabled(true);
                btnDecrypt.setEnabled(true);


            } catch ( IOException io ) {
                btToastLabel = "Unable to connect to USB_KEY";
                Toast.makeText(getApplicationContext(), btToastLabel, Toast.LENGTH_LONG).show();
            }

        }
    };

    protected void initiate_bt_connection() throws IOException
    {

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
        btSocket.connect();
        btOutputStream = btSocket.getOutputStream();
        btInputStream = btSocket.getInputStream();

        beginListenForData();

        btToastLabel = "Connected to USB_KEY";
        Toast.makeText(getApplicationContext(), btToastLabel, Toast.LENGTH_LONG).show();
        bluetooth_connected_status = 1;
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = btInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            btInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            btToastLabel = data;
                                            if ( data.equals("samantha")) {
                                                ///


                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendData(EditText textToDev) throws IOException
    {

        String msg = textToDev.getText().toString();

        //String msg = "\n";
        //msg += "\n";
        btOutputStream.write(msg.getBytes());
        btToastLabel = "Data sent";
        Log.v(TAG, "MSG is" + msg);
        Toast.makeText(getApplicationContext(), btToastLabel,
                Toast.LENGTH_LONG).show();


        lockStatus.setBackgroundResource(R.drawable.unlock);

        bluetoothAdapter.startDiscovery();

    }

    void closeBT() throws IOException
    {
        stopWorker = true;
        btOutputStream.close();
        btInputStream.close();
        btSocket.close();
        btToastLabel = "Disconnected from USB_KEY";
        Toast.makeText(getApplicationContext(), btToastLabel,
                Toast.LENGTH_LONG).show();
        bluetooth_connected_status = 0;
        bluetooth_found_status = 0;

        lockStatus.setBackgroundResource(R.drawable.lock);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // TODO Auto-generated method stub
        if(requestCode == REQUEST_ENABLE_BT)
        {
            CheckBlueToothState();
        }
    }

    private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);

                if ( device.getAddress().equals(MAC_ADDR) ) {

                    Log.d(TAG, "Detected USB_KEY");
                    Log.v(TAG, "RSSI is " + rssi);
                    bluetooth_found_distance = rssi;
                    if ((-50) - rssi > 17) {
                        bluetooth_found_status = 0;

                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        r.play();
                        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(5000);
                        Toast.makeText(getApplicationContext(), "USB_KEY is either out of range or cannot be detected! It's last detected distance is " + bluetooth_found_distance, Toast.LENGTH_SHORT).show();

                        //bluetoothAdapter.cancelDiscovery();

                    } else {
                        bluetooth_found_status = 1;
                        Toast.makeText(getApplicationContext(), "USB_KEY detected RSSI:" + rssi, Toast.LENGTH_SHORT).show();

                        bluetoothAdapter.cancelDiscovery();
                    }
                }
            }
        }};

    private final BroadcastReceiver ActionDiscoveryFinishedReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent)
        {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if ( bluetooth_found_status == 0) //&& bluetooth_connected_status == 0)
            {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(5000);
                Toast.makeText(getApplicationContext(), "USB_KEY is either out of range or cannot be detected! It's last detected distance is " + bluetooth_found_distance, Toast.LENGTH_SHORT).show();

            }

           /* if ( bluetooth_found_status == 1 )
            {
                bluetooth_found_status = 0; // reset found flag
            } */

            bluetoothAdapter.startDiscovery(); // reloop discovery

        }
    };

}