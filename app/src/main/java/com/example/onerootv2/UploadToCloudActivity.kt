package com.example.onerootv2

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException

class UploadToCloudActivity : AppCompatActivity() {

    private val allImagePaths = arrayListOf<String>()
    private val allCloudPaths = arrayListOf<String>()
    private var userName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_to_cloud)

        readUsernameFromStorage()
        userName = userName.replace(" ","").replace(".","")

        // check internet connection before storing in database
        if (checkForInternet(this))
        {
            Toast.makeText(this, "internet available", Toast.LENGTH_SHORT).show()
            // saving profile data in a firebase database
            println("............................")
            getImagePaths()
            println("............................")
            println(" ")

        }
        else
        {

            Toast.makeText(this, "internet  not available", Toast.LENGTH_SHORT).show()
            // <------------------ alert box ----------------------------------->
            // https://www.digitalocean.com/community/tutorials/android-alert-dialog-using-kotlin
            // alert color problem https://stackoverflow.com/questions/39481735/alertdialog-do-not-show-positive-and-negative-button
            val builder = this.let { it1 -> AlertDialog.Builder(it1) }
            //set title for alert dialog
            builder.setTitle("internet alert")
            //set message for alert dialog
            builder.setMessage("Please connect to internet")
            builder.setIcon(android.R.drawable.ic_popup_reminder)

            // get wifi settings
            //val wifi = activity?.getSystemService(WIFI_SERVICE) as WifiManager
            builder.setPositiveButton("connect to wifi") { _, _ ->
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                //startWifiIntent()
                //wifi.isWifiEnabled = true
                // save profile data when wifi enabled
                getImagePaths()
            }
            builder.setNegativeButton("cancel") { _, _ ->
                //wifi.isWifiEnabled = false

            }

            // colors for alert buttons
            val alert = builder.create()
            alert.setOnShowListener {
                alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Color.BLUE)
                alert.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Color.BLUE)
            }
            alert.show()
        }

    }
    private  fun getImagePaths() {

        var imagePathname: String
        var cloudPathname: String

        val gpath: String = Environment.getExternalStorageDirectory().absolutePath
        val spath = "DCIM/one_root_images/"
        val fullPath = gpath + File.separator + spath
        println("getting paths from $fullPath")
        val files = File(fullPath).listFiles()

        val imageDirectoriesList = mutableListOf<String>()
        if (files != null) {
            for (file in files) {
                imageDirectoriesList.add(file.path)
                //println(file.path)
            }
        } else {
            println("error in uploadingToCloud activity: no image directories in path---> $fullPath")
        }

        // getting list of directories
        // https://www.techiedelight.com/list-all-subdirectories-of-a-directory-in-kotlin/
        // val directories = File(fullPath).listFiles { pathname -> pathname.isDirectory }
        // println(directories!!.contentToString())


        for (eachImageDirectory in imageDirectoriesList)
        {

            val imageFiles = File(eachImageDirectory).listFiles()
            for (file in imageFiles!!) {
                if (file.name.endsWith(".jpg")) {
                    imagePathname = file.path
                    cloudPathname = userName+"_images/"+ (file.path.split("one_root_images")[1])

                    // upload single image
                    uploadImagesToCloud(imagePathname,cloudPathname)

                    allImagePaths.add(imagePathname)
                    allCloudPaths.add(cloudPathname)
                    println("----> ${file.path}")
                }
            }
        }

//        for (i in 0 until allImagePaths.size)
//        {
//            uploadImagesToCloud(allImagePaths[],cloudPathname)
//        }



        // println("allImagePaths: $allImagePaths")

    }

    private fun uploadImagesToCloud(imagePath: String, cloudPath: String)
    {
        println("<----- Uploading image to cloud: $imagePath ------------>")
        val uri2 = Uri.fromFile(File(imagePath))
        // start
        // https://firebase.google.com/docs/storage/android/start
        // storage
        // https://firebase.google.com/docs/storage/android/upload-files
        // google cloud setup
        // https://firebase.google.com/docs/android/setup

        // error in bytes -> https://gorkemkara.medium.com/upload-files-to-android-firebase-stroage-4228fdd8d47f
        val storage = Firebase.storage

        // Create a storage reference from our app
        val storageRef = storage.reference

        // File or Blob
        //val file = Uri.fromFile(File("path/to/mountains.jpg"))

        // Create the file metadata
        val metadata = storageMetadata {
            contentType = "image/jpeg"
        }


        // Upload file and metadata to the path 'images/mountains.jpg'
        if (userName!="") {

            val uploadTask = storageRef.child(cloudPath)
                .putFile(uri2, metadata)

            // Listen for state changes, errors, and completion of the upload.
            // You'll need to import com.google.firebase.storage.component1 and
            // com.google.firebase.storage.component2
            uploadTask.addOnProgressListener {
                val progress = (100.0 * it.bytesTransferred) / it.totalByteCount
                println("Upload is $progress% done")
            }.addOnPausedListener {
                println("Upload is paused")
            }.addOnFailureListener {
                // Handle unsuccessful uploads
                println("error: uploading image to goggle cloud")
            }.addOnSuccessListener {
                // Handle successful uploads on complete
                // ...
                println("image uploaded to google successfully")
            }
        }
        else
        {
            println("error in uploadToCloudActivity : username was null fail to upload images to cloud")
        }

    }

    private fun readUsernameFromStorage()
    {
        // https://www.youtube.com/watch?v=JUlZYddw03o
        // https://github.com/sandipapps/ExternalStorageDemo/blob/master/app/src/main/java/com/sandipbhattacharya/externalstoragedemo/MainActivity.java

        val filepath = "OneRootFiles"
        val fileName  = "profile.json"
        println("reading detection json data on video activity")
        println("reading json data")

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
                println("reading from json data file")
                val dataFromJson = readFromExternalStorage()
                println(" json data:  $dataFromJson")
                val jsonObject = JSONObject(dataFromJson)
                userName = jsonObject.getString("username")
                //mobileNo = jsonObject.getString("mobileNo")
                //location = jsonObject.getString("location")
                //role = jsonObject.getString("role")
//                println("username from file: $userName")
//                println("mobile No from file: $mobileNo")

            }
            else {
                Toast.makeText(
                    this,
                    "External storage not available for reading",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        catch (e: JSONException) {
            println("error in profile fragment: json file cant be read")
            println(e)
            e.printStackTrace()
        }
        finally {
            println("profile data read from folder successfully")
        }
    }

    private fun checkForInternet(context: Context): Boolean {

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

}