package com.tcs.stellarsurfers.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

class BluetoothServer (
    private val bluetoothAdapter: BluetoothAdapter,
    private val serviceName: String,
    private val serviceUUID: UUID
) : AppCompatActivity() {
    private companion object {
        const val TAG = "BluetoothClient"
    }

    private var onConnectListener: (suspend (BluetoothSocket) -> Unit)? = null

    private var onDisconnectListener: ((BluetoothSocket) -> Unit)? = null

    private var onStateChangeListener: ((Boolean) -> Unit)? = null

    private var serverSocket: BluetoothServerSocket? = null

    private val clientSockets: MutableList<BluetoothSocket> = mutableListOf()

    private var _isStopped: Boolean = true
    private var isStopped: Boolean
        get() = _isStopped
        set(value) {
            onStateChangeListener?.invoke(value)
            _isStopped = value
        }

    init {
        CoroutineScope(Dispatchers.Default).launch { monitorConnections() }
    }

    fun setOnConnectListener(listener: (suspend (BluetoothSocket) -> Unit)?) {
        onConnectListener = listener
    }

    fun setOnDisconnectListener(listener: ((BluetoothSocket) -> Unit)?) {
        onDisconnectListener = listener
    }

    fun setOnStateChangeListener(listener: ((Boolean) -> Unit)?) {
        onStateChangeListener = listener
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    suspend fun startLoop() = withContext(Dispatchers.IO) {
        serverSocket = bluetoothAdapter
            .listenUsingRfcommWithServiceRecord(serviceName, serviceUUID)

        bluetoothAdapter.startDiscovery()
        //startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), 1)

        println("state: " + bluetoothAdapter.state)
        println("discovering: " + bluetoothAdapter.isDiscovering)

        isStopped = false

        Log.i(TAG, "Server loop started")
        do {
            Log.v(TAG, "Server loop next iteration")
            val clientSocket: BluetoothSocket = acceptConnection() ?: continue

            Log.i(TAG, "Connection accepted : ${clientSocket.remoteDevice.name}")

            withContext(Dispatchers.Main) { onConnectListener?.invoke(clientSocket) }
            delay(1000)
            clientSockets.add(clientSocket)
        } while (!isStopped)
    }

    fun stop() {
        isStopped = true
        clientSockets.forEach { it.close() }
        clientSockets.clear()
        serverSocket?.close()
    }

    private fun acceptConnection(): BluetoothSocket? {
        return try {
            serverSocket!!.accept()
        } catch (e: Exception) {
            when (e) {
                is IOException,
                is NullPointerException -> {
                    Log.w(TAG, "Socket's accept() method failed", e)
                    isStopped = true

                    null
                }
                else -> throw e
            }
        }
    }

    private suspend fun monitorConnections() = withContext(Dispatchers.IO) {
        while (true) {
            clientSockets.removeAll { runBlocking { !checkConnectionState(it) } }
            delay(1000)
        }
    }

    private suspend fun checkConnectionState(socket: BluetoothSocket): Boolean =
        withContext(Dispatchers.IO) {
            try {
                socket.inputStream.skip(1)
            } catch (ignored: IOException) {
                Log.i(TAG, "Socket connection closed")
                socket.close()
                withContext(Dispatchers.Main) { onDisconnectListener?.invoke(socket) }
                return@withContext false
            }
            return@withContext true
        }

}
