package com.example.kotlinbt

import android.R
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinbt.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {


        lateinit var bluetoothManager: BluetoothManager
        lateinit var bAdapter: BluetoothAdapter
        lateinit var binding: ActivityMainBinding
        private var deviceName: String? = null
        private var deviceAddress: String? = null
        public var handler: Handler? = null
        public var mmSocket: BluetoothSocket? = null
        lateinit var cmdText: String
        lateinit var btnState: String
        public var connectedThread: ConnectedThread? = null
        public var createConnectThread: CreateConnectThread? = null




    private val CONNECTING_STATUS = 1 // used in bluetooth handler to identify message status

    private val MESSAGE_READ = 2 // used in bluetooth handler to identify message update

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


//        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
//        val bAdapter: BluetoothAdapter? = bluetoothManager.getAdapter()

        binding.progressBar.setVisibility(View.GONE)
        binding.buttonToggle.setEnabled(false)

        deviceName = getIntent().getStringExtra("deviceName")

        if (deviceName != null) {
            // Get the device address to make BT Connection
            deviceAddress = intent.getStringExtra("deviceAddress")
            // Show progree and connection status
            binding.toolbar.setSubtitle("Connecting to $deviceName...")
            binding.progressBar.setVisibility(View.VISIBLE)
            binding.buttonConnect.setEnabled(false)

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */

            val bluetoothManager = getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter = bluetoothManager.adapter
//            createConnectThread = bAdapter?.let { CreateConnectThread(it, deviceAddress) }
//            createConnectThread!!.start()
            createConnectThread = CreateConnectThread(bluetoothAdapter, deviceAddress)
            createConnectThread!!.start()
        }

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    CONNECTING_STATUS -> when (msg.arg1) {
                        1 -> {
                            binding.toolbar.setSubtitle("Connected to $deviceName")
                            binding.progressBar.setVisibility(View.GONE)
                            binding.buttonConnect.setEnabled(true)
                            binding.buttonToggle.setEnabled(true)
                        }
                        -1 -> {
                            binding.toolbar.setSubtitle("Device fails to connect")
                            binding.progressBar.setVisibility(View.GONE)
                            binding.buttonConnect.setEnabled(true)
                        }
                    }
                    MESSAGE_READ -> {
                        val arduinoMsg: String = msg.obj.toString() // Read message from Arduino
                        when (arduinoMsg.lowercase(Locale.getDefault())) {
                            "led is turned on" -> {
                                binding.imageView.setBackgroundColor(resources.getColor(R.color.background_dark))
                                Log.e("FIND","${arduinoMsg}")
                                binding.textViewInfo.setText("Arduino Message : $arduinoMsg").toString()
                            }
                            "led is turned off" -> {
                                binding.imageView.setBackgroundColor(resources.getColor(R.color.background_light))
                                binding.textViewInfo.setText("Arduino Message : $arduinoMsg").toString()
                                Log.e("FIND","${arduinoMsg}")
                            }
                        }
                    }
                }
            }
        }

        binding.buttonConnect.setOnClickListener {
            val intent = Intent(this@MainActivity, SelectDeviceActivity::class.java)
            startActivity(intent)

        }

        binding.buttonToggle.setOnClickListener {
            btnState = binding.buttonToggle.text.toString().lowercase()

            when (btnState) {
                "turn on" -> {
                    binding.buttonToggle.setText("Turn Off")
                    // Command to turn on LED on Arduino. Must match with the command in Arduino code
                    cmdText = "<turn on>"
                }
                "turn off" -> {
                    binding.buttonToggle.setText("Turn On")
                    // Command to turn off LED on Arduino. Must match with the command in Arduino code
                    cmdText = "<turn off>"
                }
            }
            connectedThread?.write(cmdText);
        }
    }

    ///////////////////////////////////////
    @SuppressLint("MissingPermission")
    inner class CreateConnectThread(bluetoothAdapter: BluetoothAdapter, address: String?) :
        Thread() {
        @SuppressLint("MissingPermission")
        lateinit var bluetoothAdapter: BluetoothAdapter
        init {
            this.bluetoothAdapter=bluetoothAdapter
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery()
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket?.connect()
                Log.e("Status", "Device connected")
                handler?.obtainMessage(CONNECTING_STATUS, 1, -1)
                    ?.sendToTarget()
            } catch (connectException: IOException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket?.close()
                    Log.e("Status", "Cannot connect to device")
                   handler?.obtainMessage(CONNECTING_STATUS, -1, -1)
                        ?.sendToTarget()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Could not close the client socket", closeException)
                }
                return
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = ConnectedThread(mmSocket!!)
            connectedThread!!.run()
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }

        init {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            var tmp: BluetoothSocket? = null
            val uuid = bluetoothDevice.uuids[0].uuid
            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid)
            } catch (e: IOException) {
                Log.e(TAG, "Socket's create() method failed", e)
            }
            mmSocket = tmp
        }
    }
    ///////////////////////////


//    inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
//        private var mmInStream: InputStream = mmSocket.inputStream
//        private var mmOutStream: OutputStream = mmSocket.outputStream
//        private val mmBuffer: ByteArray = ByteArray(1024) // mmB
//
//
//        override fun run() {
//            var numBytes: Int // bytes returned from read()
//
//            // Keep listening to the InputStream until an exception occurs.
//            while (true) {
//                // Read from the InputStream.
//                numBytes = try {
//                    mmInStream.read(mmBuffer)
//
//                } catch (e: IOException) {
//                    Log.d(TAG, "Input stream was disconnected", e)
//                    break
//                }
//
//                // Send the obtained bytes to the UI activity.
//                val readMsg =handler?.obtainMessage(MESSAGE_READ, numBytes, -1, mmBuffer)
//                readMsg?.sendToTarget()
//            }
//        }
//
//        /* Call this from the main activity to send data to the remote device */
//        fun write(input: String) {
//            val bytes = input.toByteArray() //converts entered String into bytes
//            try {
//                mmOutStream!!.write(bytes)
//            } catch (e: IOException) {
//                Log.e("Send Error", "Unable to send message", e)
//            }
//        }
//
//
//        /* Call this from the main activity to shutdown the connection */
//        fun cancel() {
//            try {
//                mmSocket.close()
//            } catch (e: IOException) {
//            }
//        }
//
//        init {
//            var tmpIn: InputStream? = null
//            var tmpOut: OutputStream? = null
//
//            // Get the input and output streams, using temp objects because
//            // member streams are final
//            try {
//                tmpIn = mmSocket.inputStream
//                tmpOut = mmSocket.outputStream
//            } catch (e: IOException) {
//            }
//            mmInStream = tmpIn!!
//            mmOutStream = tmpOut!!
//        }
//    }
inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
    private val mmSocket: BluetoothSocket
    private val mmInStream: InputStream?
    private val mmOutStream: OutputStream?
    override fun run() {
        val buffer = ByteArray(1024) // buffer store for the stream

        var bytes: Int = 0 //bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                buffer[bytes] = mmInStream!!.read().toByte()

                var readMessage: String

                if (buffer[bytes]=='\n')
                {
                    readMessage = String(buffer, 0, bytes)
                    Log.e("Arduino Message", readMessage)
                    handler!!.obtainMessage(MESSAGE_READ, readMessage)
                        .sendToTarget()
                    bytes = 0
                } else {
                    bytes++
                }
            } catch (e: IOException) {
                e.printStackTrace()
                break
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    fun write(input: String) {
        val bytes = input.toByteArray() //converts entered String into bytes
        try {
            mmOutStream!!.write(bytes)
        } catch (e: IOException) {
            Log.e("Send Error", "Unable to send message", e)
        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            mmSocket.close()
        } catch (e: IOException) {
        }
    }

    init {
        mmSocket = socket
        var tmpIn: InputStream? = null
        var tmpOut: OutputStream? = null

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.inputStream
            tmpOut = socket.outputStream
        } catch (e: IOException) {
        }
        mmInStream = tmpIn
        mmOutStream = tmpOut
    }
}


    /* ============================ Terminate Connection at BackPress ====================== */
    override fun onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null) {
            createConnectThread!!.cancel()
        }
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
    }

}