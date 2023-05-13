package com.tcs.stellarsurfers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.tcs.stellarsurfers.bluetooth.BluetoothServer
import com.tcs.stellarsurfers.databinding.ActivitySetupConnectionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


class SetupConnectionActivity : AppCompatActivity() {
    lateinit var bAdapter: BluetoothAdapter
    lateinit var bManager: BluetoothManager
    private lateinit var server: BluetoothServer
    private lateinit var binding: ActivitySetupConnectionBinding
    companion object {
        lateinit var socket: BluetoothSocket
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setup_connection)
        bManager = getSystemService(BluetoothManager::class.java)
        bAdapter = bManager.adapter
        if (bAdapter == null) {
            exitBecause("Bluetooth unavailable")
        } else if (!bAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            resultTurnOnLauncher.launch(intent)
        }

        server = BluetoothServer(bAdapter, "Stellar-Surfers", UUID.fromString("f296bf37-5412-460d-954d-2fcc31b072c0"))
        server.setOnConnectListener {
            Toast.makeText(applicationContext, "Connected", Toast.LENGTH_SHORT).show()
            server.stop()
            socket = it
            awaitStartSignal()
        }
        server.setOnDisconnectListener {
            binding.startServerButton.setBackgroundColor(Color.RED)
        }
        server.setOnStateChangeListener {
            binding.startServerButton.setBackgroundColor(Color.CYAN)
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

    private fun awaitStartSignal() {
        Dispatchers.IO.run {
            try {
                Log.i("SetupConnectionActivity", "in await start signal")
                val buffer = ByteArray(6)
                socket.inputStream.read(buffer)
                Log.i("SetupConnectionActivity", "Received start signal")
                startGame()
            } catch (ignored: IOException) {
                Log.i("SetupConnectionActivity", "exception in await start signal")
            }
        }
    }

    private fun startGame() {
        startActivity(Intent(applicationContext, GameActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }

    private var resultTurnOnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> binding.startServerButton.setBackgroundColor(Color.BLUE)
            else -> exitBecause("App requires bluetooth")
        }
    }

    private fun exitBecause(reason: String) {
        Toast.makeText(
            applicationContext,
            reason,
            Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java))
    }

}
