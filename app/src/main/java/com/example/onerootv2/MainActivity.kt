/**
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.onerootv2

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.example.onerootv2.databinding.ActivityMainBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

private var userName = ""
class MainActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        const val TAG = "TFLite - ODT"
    }



    // view binding
    private lateinit var binding:ActivityMainBinding
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // binding object
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get permission first then open app
        getPermission()
        // A function to hide NavigationBar
        hideSystemUI()

        //shared preferences
        val sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val editor = sharedPref?.edit()

        editor?.apply {
            putBoolean("commandUpdate", true)
            apply() //asynchronously
        }

        // get sharedPref values
        val registrationDone = sharedPref.getBoolean("registered", false)
        val autoSignInDone = sharedPref.getBoolean("autoSignIn",false)
        val signedIn = sharedPref.getBoolean("signedIn",false)
        val sessionType = sharedPref.getInt("session",0)

        if (registrationDone)
        {
          if (autoSignInDone or signedIn)
          {
              replaceFragment(HomeFragment())
              // TODO try to put navigation bar at front4
              binding.bottomNavigationView.setOnItemSelectedListener {
                  when(it.itemId)
                  {
                      R.id.home -> replaceFragment(HomeFragment())
                      R.id.profile -> replaceFragment(ProfileFragment())
                      R.id.settings -> replaceFragment(SettingsFragment())
                      else ->
                      {
                          replaceFragment(HomeFragment())
                      }
                  }
                  true
              }
          }
          else
          {
              if (sessionType == 0) {
                  editor?.apply {
                      putInt("session", 5)
                      apply() //asynchronously
                  }
              }
              // try sign in again
              replaceFragment(LogInFragment())
              this.supportFragmentManager.popBackStack()
          }
        }
        else
        {
            // try registration again
            Toast.makeText(this@MainActivity,"registering user", Toast.LENGTH_LONG).show()
            replaceFragment(RegisterFragment())
            this.supportFragmentManager.popBackStack()
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        val sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val autoSignInDone = sharedPref.getBoolean("autoSignIn",false)
        if (!autoSignInDone) {
            val editor = sharedPref.edit()
            // shared preferences
            editor.apply {
                putBoolean("signedIn", false)
                apply()
            }
        }
    }

    // update status when close
    private fun updateStatusToFirebase()
    {
        val statusText = "user app closed offline"
        // https://saveyourtime.medium.com/firebase-cloud-firestore-add-set-update-delete-get-data-6da566513b1b
        // https://firebase.google.com/docs/firestore/manage-data/add-data

        // firestore update
        // https://firebase.google.com/docs/firestore/manage-data/add-data
        fun saveErrorsInTextToStorage(errorString: String)
        {
            val textFilePath = "OneRootFiles"
            val fileName  = "errors.txt"
            println("errors raised storing errors in txt file")
            val filePath = this.getExternalFilesDir(textFilePath)
            val myExternalFile = File(filePath, fileName)

            fun isExternalStorageWritable(): Boolean {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state
            }

            try
            {
                if (isExternalStorageWritable())
                {
                    if (myExternalFile.exists())
                    {
                        println("errors.txt file exists read line from it and update")
                        val fileReader = BufferedReader(FileReader(myExternalFile))
                        val text = fileReader.readText()
                        fileReader.close()

                        val newText = "$text\n<--------------------------->\n$errorString\n"
                        val fileWriter = FileWriter(myExternalFile)
                        fileWriter.write(newText)
                        fileWriter.close()
                    }
                    else
                    {
                        println("errors.txt file does not exist make a new file and update")
                        val fileWriter = FileWriter(myExternalFile)
                        fileWriter.write("$errorString\n")
                        fileWriter.close()
                    }
                    // Toast.makeText(this, "errors are stored in text file", Toast.LENGTH_SHORT).show()
                    println("<---errors.txt file stored successfully ")
                }
                else
                {
                    Toast.makeText(this, "External storage not available for writing errors.txt", Toast.LENGTH_SHORT).show()
                    println("External storage not available for storing errors.txt file")
                }
            }
            catch (e: Exception) {
                println("error in videoActivity/saveErrorsInTextToStorage() : errors.txt cant be saved")
                e.printStackTrace()
            }



        }

        fun readUserNameFromStorage()
        {
            // https://www.youtube.com/watch?v=JUlZYddw03o
            // https://github.com/sandipapps/ExternalStorageDemo/blob/master/app/src/main/java/com/sandipbhattacharya/externalstoragedemo/MainActivity.java

            val filepath = "OneRootFiles"
            val fileName  = "profile.json"
            println("reading profile json data on home fragment")

            fun isExternalStorageReadable(): Boolean {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
            }

            fun readFromExternalStorage(): String {
                val myExternalFile = File(this.getExternalFilesDir(filepath), fileName)
                val stringBuilder = StringBuilder()
                try {
                    val fileReader = FileReader(myExternalFile)
                    val bufferedReader = BufferedReader(fileReader)
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                    bufferedReader.close()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                return stringBuilder.toString()
            }

            // read file  from external storage
            try {
                if (isExternalStorageReadable()) {
                    println("reading from profile json data file")
                    val dataFromJson = readFromExternalStorage()
                    println(" json data:  $dataFromJson")
                    val jsonObject = JSONObject(dataFromJson)
                    userName = jsonObject.getString("username")
                }
                else
                {
                    Toast.makeText(
                        this,
                        "External storage not available for reading",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            catch (e: JSONException)
            {
                println("error in mainActivity()/readProfileFromStorage(): failed to read profile.json ")
                e.printStackTrace()
                val errorString = "\n error in mainActivity()/readProfileFromStorage(): failed to read profile.json \n${e.message} \n $e"
                saveErrorsInTextToStorage(errorString)
            }
            finally {
                println("profile data read from folder successfully")
            }
        }

        fun checkForInternet(context: Context): Boolean {

        // register activity with the connectivity manager service
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // if the android version is equal to M
        // or greater we need to use the
        // NetworkCapabilities to check what type of
        // network has the internet connection

        // Returns a Network object corresponding to
        // the currently active default data network.
        val network = connectivityManager.activeNetwork ?: return false

        // Representation of the capabilities of an active network.
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            // Indicates this network uses a Wi-Fi transport,
            // or WiFi has network connectivity
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

            // Indicates this network uses a Cellular transport. or
            // Cellular has network connectivity
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

            // else return false
            else -> false
        }
    }


        fun updateStatusToProfileJson(statusString1: String)
        {
            try
            {
                val filepath = "OneRootFiles"
                val fileName = "profile.json"
                val filePath = this.getExternalFilesDir(filepath)
                val myExternalFile = File(filePath, fileName)

                println("updating status in profile.json")

                fun isExternalStorageWritable(): Boolean {
                    val state = Environment.getExternalStorageState()
                    return Environment.MEDIA_MOUNTED == state
                }

                if (isExternalStorageWritable()) {
                    // get json object from path
                    val jsonObject = JSONObject(myExternalFile.readText())
//                val statusObject = jsonObject.getJSONObject("status")
                    jsonObject.put("status",statusString1)
                    val fileWriter1 = FileWriter(myExternalFile)
                    fileWriter1.write(jsonObject.toString())
                    fileWriter1.close()

                    // Toast.makeText(activity, "status in profile.json file updated", Toast.LENGTH_SHORT).show()
                    println("<--- status in profile.json file updated")
                }
                else
                {
                    Toast.makeText(
                        this,
                        "MainActivity(): External storage not available for writing status in profile json",
                        Toast.LENGTH_SHORT
                    ).show()
                    println("MainActivity(): External storage not available for updating status in profile.json file")
                }

            }
            catch (e: Exception)
            {
                println("error in HomeFragment/updateStatusToProfileJson(): failed to update status to profile.json ")
                e.printStackTrace()
                val errorString = "\n error in HomeFragment/updateStatusToFirebase(): failed to update status to profile.json  \n${e.message}"
                saveErrorsInTextToStorage(errorString)
            }

        }

        // creating database
        val db = Firebase.firestore

        try
        {
            if (checkForInternet(this))
            {
                readUserNameFromStorage()
                println("updating status to firebase: $statusText")
                val documentName = userName.replace(" ", "").replace(".", "") + "Data"

                db.collection("users").document(documentName).update("status", statusText)
                    .addOnSuccessListener { Log.d(TAG, "StatusToFirebase successfully updated!") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error updateStatusToFirebase ", e) }
            }
            else
            {
                println("updating status to firebase failed please connect to internet")
            }
        }
        catch (e: Exception)
        {
            println("error in MainActivity()/updateStatusToFirebase(): failed to update status to firebase ")
            e.printStackTrace()
            val errorString = "\n error in MainActivity()/updateStatusToFirebase(): failed to update status to firebase  \n${e.message}"
            saveErrorsInTextToStorage(errorString)
        }
        // updating status in profile.json
        updateStatusToProfileJson(statusText)
    }

    // Function to hide NavigationBar
    @RequiresApi(Build.VERSION_CODES.S)
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window,
            window.decorView.findViewById(android.R.id.content)).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())

            // When the screen is swiped up at the bottom
            // of the application, the navigationBar shall
            // appear for some time
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * onClick(v: View?)
     *      Detect touches on the UI components
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home -> replaceFragment(HomeFragment())
            R.id.profile -> replaceFragment(ProfileFragment())
            R.id.settings -> replaceFragment(SettingsFragment())
        }
    }


    // permission for camera
    private fun getPermission(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA,android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.ACCESS_WIFI_STATE,android.Manifest.permission.INTERNET), 101)
        }
    }

//    private fun restartActivity() {
//        val intent = intent
//        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
//        finish()
//        startActivity(intent)
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            getPermission()
        }
    }

    private fun replaceFragment(fragment : Fragment){

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout,fragment)
        fragmentTransaction.commit()
    }

    override fun onStop() {
        super.onStop()
        updateStatusToFirebase()
    }
}


