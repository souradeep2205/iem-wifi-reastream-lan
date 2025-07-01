package com.niusounds.flowsample

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.niusounds.flowsample.databinding.ActivityMainBinding
import com.niusounds.libreastream.receiver.play
import com.niusounds.libreastream.receiver.receiveReaStream
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var wifiLock: WifiManager.WifiLock
    private lateinit var wakeLock: PowerManager.WakeLock

    companion object {
        val executorAudio = Executors.newSingleThreadExecutor() {
            Thread(it).apply {
                priority = Thread.MAX_PRIORITY
            }
        }.asCoroutineDispatcher()
        val executorUdp = Executors.newSingleThreadExecutor(){
            Thread(it).apply {
                priority=Thread.MAX_PRIORITY
            }
        }.asCoroutineDispatcher()
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("WiFi IEM+ v0.2")
        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)

            // Show instruction
            findMyIpAddress()?.let { myIp -> text.text = getString(R.string.message, myIp) }
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val minbuf=am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
            //text.text="${text.text}\nSetting Minimum buffer size to $minbuf samples\n"
            val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiLockType = WifiManager.WIFI_MODE_FULL_HIGH_PERF
            wifiLock = wifiManager.createWifiLock(wifiLockType, "jammjotiemlock")
            wifiLock.acquire() // Acquire the wifi lock
            // Display a message indicating the Wi-Fi lock status
            if (wifiLock.isHeld) {
                val wifiLockMode = if (wifiLockType == WifiManager.WIFI_MODE_FULL_LOW_LATENCY) {
                    "Low Latency"
                } else {
                    "High performance"
                }
                text.text = "${text.text}\nWi-Fi lock acquired successfully in $wifiLockMode mode."
            } else {
                text.text = "${text.text}\nFailed to acquire Wi-Fi lock."
            }
            wakeLock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                    newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                        acquire()
                    }
                }
            if(wakeLock.isHeld)
            {
                text.text = "${text.text}\n\nWake lock acquired successfully."
            }
            else
            {
                text.text = "${text.text}\nFailed to acquire Wake lock."
            }

            lifecycleScope.launch {
                val packets = receiveReaStream(executorUdp)

                // Play received audio
                launch(executorAudio) {
                    packets.play(minbuf,this@MainActivity,sampleRate = 48000)
                }
            }
            Toast.makeText(this@MainActivity, "Listening...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findMyIpAddress(): String? {
        NetworkInterface.getNetworkInterfaces().asSequence().forEach { intf ->
            intf.inetAddresses.asSequence().forEach { inetAddress ->
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.hostAddress
                }
            }
        }

        return null
    }

    @SuppressLint("Wakelock")
    override fun onDestroy() {
        super.onDestroy()
        executorAudio.close()
        executorUdp.close()
        Log.d("MainActivity", "onDestroy: ")
        // Release the wifi lock when the activity is destroyed
        if (::wifiLock.isInitialized && wifiLock.isHeld) {
            wifiLock.release()
            //Toast.makeText(this, "Wi-Fi lock released", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "onDestroy: wifi lock released")
        }
        if(::wakeLock.isInitialized && wakeLock.isHeld)
        {
            wakeLock.release()
            //Toast.makeText(this, "Wake lock released", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "onDestroy: wake lock released")
        }
    }
}
