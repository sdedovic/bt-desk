package btdesk.dedovic.com.bt_desk;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class ControllerActivity extends AppCompatActivity {

    private static final int BT_ENABLE_REQUEST = 9;
    private static final int BT_IN_MESSAGE = 91;
    private static final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String TAG = "bt-desk";

    private Button upButton;
    private Button downButton;
    private BluetoothAdapter bluetoothAdapter;
    private ListView btDeviceListView;
    private ArrayAdapter<String> btDeviceAdapter;
    private ConnectionTread connectionTread;
    private ConnectThread connectThread;
    private Handler deviceHandler;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_LONG).show();
            finish();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent startBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(startBt, BT_ENABLE_REQUEST);
        }

        deviceHandler = new Handler(Looper.getMainLooper() ) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case BT_IN_MESSAGE:
                        Log.i(TAG, msg.toString());
                        break;
                    default:
                        super.handleMessage(msg);

                }
            }
        };

        upButton = (Button) findViewById(R.id.btn_up);
        downButton = (Button) findViewById(R.id.btn_down);


        upButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (connectionTread != null) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            connectionTread.upCmd();
                            break;
                        case MotionEvent.ACTION_UP:
                            connectionTread.stopCmd();
                            break;
                    }
                }
                return false;
            }
        });

        downButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (connectionTread != null) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            connectionTread.downCmd();
                            break;
                        case MotionEvent.ACTION_UP:
                            connectionTread.stopCmd();
                            break;
                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        setRed();
        showDevices();

        if (connectionTread != null) {
            connectionTread.run();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (connectThread != null) {
            connectThread.cancel();
        }
        if (connectionTread != null) {
            connectionTread.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == BT_ENABLE_REQUEST) {
            showDevices();
        }
    }

    private void showDevices() {
        btDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        btDeviceListView = (ListView) findViewById(R.id.listView);

        final Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                btDeviceAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

        btDeviceListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String itemStr = btDeviceAdapter.getItem(position);
                String mac = itemStr.split("\n")[1];
                Log.i(TAG, "Establishing connection to: " + itemStr);

                // kill old connection
                if (connectionTread != null) {
                    connectionTread.cancel();
                }
                // and establish a new one
                new ConnectThread(bluetoothAdapter.getRemoteDevice(mac)).run();

                return true;
            }
        });

        btDeviceListView.setAdapter(btDeviceAdapter);
    }

    private class ConnectionTread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectionTread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    deviceHandler.obtainMessage(BT_IN_MESSAGE, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    setRed();
                    break;
                }
            }
        }

        public void upCmd() {
            write((byte) 1);
        }

        public void downCmd() {
            write((byte) 2);
        }

        public void stopCmd() {
            write((byte) 255);
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte cmd) {
            byte[] bytes = new byte[]{cmd};
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            String uuids = "";
            for (ParcelUuid parcelUuid : device.getUuids()) {
                uuids += parcelUuid.toString() + "\n";
            }

            Log.d(TAG, uuids);

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createInsecureRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Failed to connect to: " + mmDevice.getName(), e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "Failed to connect to socket of: " + mmDevice.getName(), e);

                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException ee) {
                    Log.e(TAG, "Failed to close socket connection to: " + mmDevice.getName(), ee);

                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        Log.i(TAG, "Connected to: " + socket.toString());
        connectionTread = new ConnectionTread(socket);
        setGreen();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setRed() {
        int color = getResources().getColor(R.color.colorRed);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
        getWindow().setStatusBarColor(color);
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setGreen() {
        int color = getResources().getColor(R.color.colorPrimary);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
        getWindow().setStatusBarColor(color);
    }
}
