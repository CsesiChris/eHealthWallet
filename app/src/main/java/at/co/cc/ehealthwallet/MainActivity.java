package at.co.cc.ehealthwallet;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends Activity implements
        CreateNdefMessageCallback, OnNdefPushCompleteCallback {

    private static final String LOG_TAG = "eH-Wallet";

    private ImageView dashboard;
    private TextView textInfo;
    private TextView textOut;

    /** Report List */
    private TextView labelReport;
    private LinearLayout tableReport;
    private TextView exam2;

    /** Report Detail*/
    private TextView detailHeader1;
    private TextView detailHeader2;
    private LinearLayout detailsGrid;
    private TextView detailsText;
    private ImageView backButton;

    private DrawerLayout mDrawerLayout;

    private NfcAdapter nfcAdapter;

    private String myMacAdress;

    /** Local Bluetooth adapter */
    private BluetoothAdapter mBluetoothAdapter = null;

    private int mState;
    private int mNewState;

    private ConnectedThread mConnectedThread;

//    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;

    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_TRANSFER_INFO = 6;

    public static final String INFO = "info";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textInfo = (TextView)findViewById(R.id.info);
        textOut = (TextView) findViewById(R.id.textout);
        dashboard = (ImageView) findViewById(R.id.navigation_dashboard);

        /** Report list view */
        labelReport = (TextView) findViewById(R.id.report_headline);
        labelReport.setVisibility(TextView.INVISIBLE);
        tableReport = (LinearLayout) findViewById(R.id.report_table);
        tableReport.setVisibility(TextView.INVISIBLE);
        exam2 = (TextView) findViewById(R.id.tableRowExam2);

        /** Report detail view */
        detailHeader1 = (TextView) findViewById(R.id.report_detail_headline1);
        detailHeader1.setVisibility(TextView.INVISIBLE);
        detailHeader2 = (TextView) findViewById(R.id.report_detail_headline2);
        detailHeader2.setVisibility(TextView.INVISIBLE);
        detailsGrid  = (LinearLayout) findViewById(R.id.detailsGrid);
        detailsGrid.setVisibility(TextView.INVISIBLE);
        detailsText = (TextView) findViewById(R.id.detailsText);

        Spanned sp = Html.fromHtml(getString(R.string.some_text));
        detailsText.setText(sp);

        backButton = (ImageView) findViewById(R.id.back_icon);
        backButton.setVisibility(TextView.INVISIBLE);

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        Toast.makeText(MainActivity.this,
                                menuItem.getTitle(),
                                Toast.LENGTH_LONG).show();

                        labelReport.setVisibility(TextView.VISIBLE);
                        tableReport.setVisibility(LinearLayout.VISIBLE);

                        /** hide dashboard */
                        dashboard.setVisibility(ImageView.INVISIBLE);

                        /** hide report detail */
                        detailHeader1.setVisibility(TextView.INVISIBLE);
                        detailHeader2.setVisibility(TextView.INVISIBLE);
                        detailsGrid.setVisibility(TextView.INVISIBLE);
                        backButton.setVisibility(TextView.INVISIBLE);

                        return true;
                    }
                });

        exam2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                /** hide report overview */
                labelReport.setVisibility(TextView.INVISIBLE);
                tableReport.setVisibility(LinearLayout.INVISIBLE);

                /** display report detail */
                detailHeader1.setVisibility(TextView.VISIBLE);
                detailHeader2.setVisibility(TextView.VISIBLE);
                detailsGrid.setVisibility(TextView.VISIBLE);
                backButton.setVisibility(TextView.VISIBLE);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                /** display report overview */
                labelReport.setVisibility(TextView.VISIBLE);
                tableReport.setVisibility(LinearLayout.VISIBLE);

                /** hide report detail */
                detailHeader1.setVisibility(TextView.INVISIBLE);
                detailHeader2.setVisibility(TextView.INVISIBLE);
                detailsGrid.setVisibility(TextView.INVISIBLE);
                backButton.setVisibility(TextView.INVISIBLE);
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter==null){
            Toast.makeText(MainActivity.this,
                    "nfcAdapter==null, no NFC adapter exists",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this,
                    "Ready for transfer ...",
                    Toast.LENGTH_LONG).show();
            nfcAdapter.setNdefPushMessageCallback(this, this);
            nfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }

        myMacAdress = getBluetoothMac(MainActivity.this);

        Toast.makeText(MainActivity.this,
                myMacAdress,
                Toast.LENGTH_LONG).show();

        textOut.setText(myMacAdress + "\n" + "MyBluetoothPassword");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String action = intent.getAction();
        if(action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)){
            Parcelable[] parcelables =
                    intent.getParcelableArrayExtra(
                            NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage inNdefMessage = (NdefMessage)parcelables[0];
            NdefRecord[] inNdefRecords = inNdefMessage.getRecords();
            NdefRecord NdefRecord_0 = inNdefRecords[0];
            String inMsg = new String(NdefRecord_0.getPayload());
            textInfo.setText(inMsg);
        }

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            textOut.setText("Bluetooth is not available");
        }

        textOut.setText("Bluetooth is available");

        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }

        /**
        Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        } */

        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {

        final String eventString = "Sending NFC info ...\n" + event.toString();
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        eventString,
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {

        byte[] bytesOut = myMacAdress.getBytes();

        NdefRecord ndefRecordOut = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA,
                "text/plain".getBytes(),
                new byte[] {},
                bytesOut);

        NdefMessage ndefMessageout = new NdefMessage(ndefRecordOut);
        return ndefMessageout;
    }

    private String getBluetoothMac(final Context context) {

        String result = null;
        if (context.checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH)
                == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Hardware ID are restricted in Android 6+
                // https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id
                // Getting bluetooth mac via reflection for devices with Android 6+
                result = android.provider.Settings.Secure.getString(context.getContentResolver(),
                        "bluetooth_address");
            } else {
                BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
                result = bta != null ? bta.getAddress() : "";
            }
        }
        return result;
    }


    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {

        Log.d(LOG_TAG, "connected, Socket Type:" + socketType);
        transferInfo("Start data transfer ...");

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        /*
        Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        } */

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        try
        {
            Thread.sleep(500);
            write(getFHIRmessage().getBytes());

            Thread.sleep(300);
            //write(getCT());

            try {
                Resources res = getResources();
                InputStream is = res.openRawResource(R.raw.radiology_hand);

                int nRead;
                byte[] data = new byte[8024];

                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    write(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Thread.sleep(300);
            write(new String("###END###").getBytes());

            transferInfo("Sending Health record finished!");
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;

        Log.i(LOG_TAG, "In the write block:\n" + out.length);

        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    private void transferInfo(String str) {
        Message msg = mHandler.obtainMessage(MESSAGE_TRANSFER_INFO);
        Bundle bundle = new Bundle();
        bundle.putString(INFO, str);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {

        Activity a = getParent();

        @Override
        public void handleMessage(Message msg) {

            // Log.d(LOG_TAG, "handleMessage:" + msg.what);

            switch (msg.what) {

                case MESSAGE_TRANSFER_INFO:
                    textInfo.append("\n" + msg.getData().getString(INFO));
                    break;

                case MESSAGE_TOAST:

                    Toast.makeText(a, msg.getData().getString("TOAST"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };



    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }

        public void run() {
            Log.d(LOG_TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Socket Type: '" + mSocketType + "' accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (MainActivity.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(LOG_TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(LOG_TAG, "END mAcceptThread, socket Type: " + mSocketType);
        }

        public void cancel() {
            Log.d(LOG_TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(LOG_TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(LOG_TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(LOG_TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                } catch (IOException e) {
                    Log.e(LOG_TAG, "disconnected", e);
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {

                mmOutStream.write(buffer);
                mmOutStream.flush();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Exception during write", e);
            }
        }


        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "close() of connect socket failed", e);
            }
        }
    }

    private String getFHIRmessage() {

        String fhirReport;

        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.fhir_report);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            fhirReport = new String(b);
        } catch (Exception e) {
            // e.printStackTrace();
            fhirReport = new String("Error: can't show help.");
        }

        return fhirReport;
    }

    private byte[] getCT() {

        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.radiology_hand);

            byte[] ctByteArray = new byte[in_s.available()];
            in_s.read(ctByteArray);

            Log.d(LOG_TAG, "CT bytes: " + ctByteArray.length);
            return ctByteArray;

        } catch (Exception e) {
            return new  byte[0];
        }
    }
}