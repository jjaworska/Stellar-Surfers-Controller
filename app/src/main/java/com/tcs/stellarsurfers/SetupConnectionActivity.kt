package com.tcs.stellarsurfers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.tcs.stellarsurfers.bluetooth.BluetoothServer
import com.tcs.stellarsurfers.databinding.ActivitySetupConnectionBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.*


class SetupConnectionActivity : AppCompatActivity() {
    lateinit var bAdapter: BluetoothAdapter
    lateinit var bManager: BluetoothManager
    private lateinit var server: BluetoothServer
    private lateinit var binding: ActivitySetupConnectionBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setup_connection)
        bManager = getSystemService(BluetoothManager::class.java)
        bAdapter = bManager.adapter
        if (bAdapter == null) {
            exitBecause("Bluetooth unavailable")
        } else {
            binding.bluetoothStatusTv.text = "Bluetooth available"
        }

        server = BluetoothServer(bAdapter, "Stellar-Surfers", UUID.fromString("f296bf37-5412-460d-954d-2fcc31b072c0"))
        server.setOnConnectListener {
            binding.bluetoothStatusTv.text = "Connected"
            server.stop()
        }
        server.setOnDisconnectListener {
            binding.bluetoothStatusTv.text = "Disconnected"
        }
        server.setOnStateChangeListener {
            if (!it) binding.bluetoothStatusTv.text = "Server active"
        }
        binding.startServerButton.setOnClickListener {
            MainScope().launch {
                server?.apply {
                    stop()
                    server.startLoop()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }

    private fun exitBecause(reason: String) {
        Toast.makeText(
            applicationContext,
            reason,
            Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java))
    }

}
