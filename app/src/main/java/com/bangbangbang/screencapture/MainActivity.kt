package com.bangbangbang.screencapture

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // 请求权限
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Settings.canDrawOverlays(this)) {
                    startFloatingWindowService()
                } else {
                    Toast.makeText(this, "您已经关闭了悬浮窗权限。无法显示悬浮窗", Toast.LENGTH_SHORT).show()
                }
                finish()
            }.launch(intent)
        } else {
            startFloatingWindowService()
        }
        startService(Intent(this, FloatingViewService::class.java))
//        finish()
        // 权限检查
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingWindowService()
            } else {
                Toast.makeText(this, "您已经关闭了悬浮窗权限。无法显示悬浮窗", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun startFloatingWindowService() {
        startService(Intent(this, FloatingViewService::class.java))
    }
}