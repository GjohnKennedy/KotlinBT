package com.example.kotlinbt

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceListAdapter(context: Context?, deviceList: ArrayList<DeviceInfoModel>?):RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var context: Context? = null
    private var deviceList: ArrayList<DeviceInfoModel>? = null
    init {
        this.context = context
        this.deviceList = deviceList
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var textName: TextView
        var textAddress: TextView
        var linearLayout: LinearLayout

        init {
            textName = v.findViewById(R.id.textViewDeviceName)
            textAddress = v.findViewById(R.id.textViewDeviceAddress)
            linearLayout = v.findViewById(R.id.linearLayoutDeviceInfo)

        }
    }

//    fun DeviceListAdapter(context: Context?, deviceList: List<Any?>?) {
//        this.context = context
//        this.deviceList = deviceList as List<Any>?
//    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.device_info_layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemHolder = holder as ViewHolder
        val deviceInfoModel = deviceList!![position] as DeviceInfoModel
        itemHolder.textName.setText(deviceInfoModel.deviceName)
        itemHolder.textAddress.setText(deviceInfoModel.deviceHardwareAddress)

        // When a device is selected
        itemHolder.linearLayout.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            // Send device details to the MainActivity
            intent.putExtra("deviceName", deviceInfoModel.deviceName)
            intent.putExtra("deviceAddress", deviceInfoModel.deviceHardwareAddress)
            // Call MainActivity
            context!!.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return deviceList!!.size
        Log.d("COUNT","${deviceList!!.size}")
    }
}
