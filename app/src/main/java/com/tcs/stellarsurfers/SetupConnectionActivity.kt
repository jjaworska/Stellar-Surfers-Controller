package com.tcs.stellarsurfers

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.tcs.stellarsurfers.databinding.ActivitySetupConnectionBinding
import java.io.IOException
import java.util.*


class SetupConnectionActivity : AppCompatActivity() {
    lateinit var bAdapter: BluetoothAdapter
    lateinit var bManager: BluetoothManager
    private lateinit var binding: ActivitySetupConnectionBinding
    lateinit var listenSocket: BluetoothServerSocket
    lateinit var socket: BluetoothSocket

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
        if (bAdapter.isEnabled) {
            binding.bluetoothIv.setImageResource(R.drawable.ic_bluetooth_on)
        } else {
            binding.bluetoothIv.setImageResource(R.drawable.ic_bluetooth_off)
        }

        binding.turnOnBtn.setOnClickListener {
            if (!bAdapter.isEnabled) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                resultTurnOnLauncher.launch(intent)
            }
        }

        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(applicationContext, "Permission Granted! :o", Toast.LENGTH_LONG).show()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN) -> {
                print("Should show rationale")
                Toast.makeText(applicationContext, "Should show rationale", Toast.LENGTH_LONG).show()
                // TODO
            }
            else -> {
                print("Permission request")
                Toast.makeText(applicationContext, "Launching permission request", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.

        binding.discoverableBtn.setOnClickListener {
            if (!bAdapter.isDiscovering) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                resultDiscoverableLauncher.launch(intent)
            }
        }

        AcceptThread().start()
    }

    override fun onDestroy() {
        super.onDestroy()
        listenSocket.close()
        socket.close()
    }

    private inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bAdapter.listenUsingInsecureRfcommWithServiceRecord("Stellar Surfers",
                UUID.fromString("e8e10f95-1a70-4b27-9ccf-02010264e9c8"))
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    Log.e(TAG, "Yup, I'm here")
                    mmServerSocket!!.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                Log.e(TAG, "Got this far")
                socket?.also {
                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    private fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket) {
        Toast.makeText(applicationContext, "aMAzInG", Toast.LENGTH_LONG).show()
    }

    private fun exitBecause(reason: String) {
        Toast.makeText(
            applicationContext,
            reason,
            Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java))
    }

    private var resultTurnOnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> binding.bluetoothIv.setImageResource(R.drawable.ic_bluetooth_on)
            else -> binding.bluetoothIv.setImageResource(R.drawable.ic_bluetooth_off)
        }
    }

    private var resultDiscoverableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> Toast.makeText(applicationContext, "Success!", Toast.LENGTH_LONG).show()
            else -> Toast.makeText(applicationContext, "F for Failure", Toast.LENGTH_LONG).show()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                print("Success!!!")
                // Toast.makeText(applicationContext, "Success!!!", Toast.LENGTH_LONG).show()
            } else {
                print("This is not a world of free people :<")
                // exitBecause("This is not a world of free people")
            }
        }

}
