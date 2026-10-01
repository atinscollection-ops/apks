package com.example.printing

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

object BluetoothThermalPrinter {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printBitmapDirect(
        device: BluetoothDevice,
        bitmap: Bitmap
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            val outputStream: OutputStream = socket.outputStream

            // 1. Reset / Init printer
            outputStream.write(byteArrayOf(0x1B, 0x40))

            // 2. Center alignment
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01))

            // 3. Print Bitmap as ESC/POS raster image (GS v 0)
            val rasterBytes = bitmapToEscPosRaster(bitmap)
            outputStream.write(rasterBytes)

            // 4. Feed & cut paper
            outputStream.write(byteArrayOf(0x1B, 0x64, 0x03)) // Feed 3 lines
            outputStream.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10)) // GS V A 16 (Cut paper)

            outputStream.flush()
            socket.close()
            Result.success(Unit)
        } catch (e: Exception) {
            try { socket?.close() } catch (_: Exception) {}
            Result.failure(Exception("Gagal mencetak via Bluetooth: ${e.localizedMessage ?: "Koneksi terputus"}"))
        }
    }

    private fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val header = byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,
            (widthBytes and 0xFF).toByte(),
            ((widthBytes shr 8) and 0xFF).toByte(),
            (height and 0xFF).toByte(),
            ((height shr 8) and 0xFF).toByte()
        )

        val imageBytes = ByteArray(widthBytes * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                // If dark pixel, set bit to 1
                if (luminance < 160) {
                    val byteIndex = y * widthBytes + (x / 8)
                    val bitIndex = 7 - (x % 8)
                    imageBytes[byteIndex] = (imageBytes[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }

        return header + imageBytes
    }
}
