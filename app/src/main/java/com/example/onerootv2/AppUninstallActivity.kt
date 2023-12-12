package com.example.onerootv2

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class AppUninstallActivity : AppCompatActivity() {
    // https://www.tutorialspoint.com/uninstall-apks-programmatically
    // go to app uninstall info
    //https://stackoverflow.com/questions/17167442/how-to-launch-app-info-for-a-android-package

    private val packageName = "onerootv2"
    override fun onCreate(savedInstanceState: Bundle?) {
        println("uninstall process started")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_uninstall)

        // open uninstall settings
        val i = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        i.addCategory(Intent.CATEGORY_DEFAULT)
        i.data = Uri.parse("package:com.example.$packageName")
        startActivity(i)
    }

    // TODO delete images folder before uninstall


}