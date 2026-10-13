package com.example.virtualcamera // আপনার প্রজেক্টের আসল প্যাকেজ নাম এখানে বসাবেন

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // ১. প্রয়োজনীয় ভেরিয়েবল ডিক্লেয়ারেশন
    private lateinit var btnToggleDeepfake: Switch
    private lateinit var btnVoiceChange: Button
    private var selectedVideoPath: String = "/storage/emulated/0/Download/my_video.mp4"
    private var isDeepfakeActive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ২. UI কম্পোনেন্ট ইনিশিয়ালাইজেশন (XML ফাইলের সাথে কানেক্ট করা)
        btnToggleDeepfake = findViewById(R.id.btnToggleDeepfake)
        btnVoiceChange = findViewById(R.id.btnVoiceChange)

        // ৩. ডিপফেক সুইচ অন/অফ করার টগল লিসেনার
        btnToggleDeepfake.setOnCheckedChangeListener { _, isChecked ->
            isDeepfakeActive = isChecked
            if (isChecked) {
                DeepfakeEngine.initializeDeepfakeFeed(selectedVideoPath)
                Toast.makeText(this, "Deepfake Hook Activated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Deepfake Hook Deactivated", Toast.LENGTH_SHORT).show()
            }
        }

        // ৪. ভয়েস চেঞ্জার বাটন লিসেনার
        btnVoiceChange.setOnClickListener {
            val sampleAudioBuffer = ByteArray(1024)
            val isSuccess = DeepfakeEngine.nativeProcessVoice(sampleAudioBuffer)
            if (isSuccess) {
                Toast.makeText(this, "Voice Interception Applied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ৫. ক্যামেরা প্রিভিউ ফ্রেম ক্যাচ করে ডিপফেক ফিড দিয়ে রিপ্লেস করার মেথড
    fun onPreviewFrameCaptured(rawCameraData: ByteArray): ByteArray {
        return if (isDeepfakeActive) {
            DeepfakeEngine.processCameraFrame(rawCameraData)
        } else {
            rawCameraData // ডিপফেক অফ থাকলে নরমাল ক্যামেরা চলবে
        }
    }
}
