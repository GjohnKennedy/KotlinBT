package com.example.kotlinbt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinbt.databinding.ActivitySelectDeviceBinding
import com.google.android.material.snackbar.Snackbar


class SelectDeviceActivity : AppCompatActivity() {

    lateinit var binding:ActivitySelectDeviceBinding
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivitySelectDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Bluetooth Setup
//        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager.adapter

        // Get List of Paired Bluetooth Device
//        val pairedDevices = bluetoothAdapter.bondedDevices

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        val deviceList: ArrayList<DeviceInfoModel> = ArrayList()

        if (pairedDevices!!.size > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (device in pairedDevices) {
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                val deviceInfoModel = DeviceInfoModel(deviceName, deviceHardwareAddress)
                deviceList.add(deviceInfoModel)
            }
            // Display paired device using recyclerView
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewDevice)
            recyclerView.layoutManager = LinearLayoutManager(this)
            val deviceListAdapter = DeviceListAdapter(this, deviceList)
            recyclerView.adapter = deviceListAdapter

            recyclerView.itemAnimator = DefaultItemAnimator()

        }
        else {
            val view = findViewById<View>(R.id.recyclerViewDevice)
            val snackbar = Snackbar.make(view, "Activate Bluetooth or pair a Bluetooth device", Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction("OK") { }
            snackbar.show()
        }

    }
}