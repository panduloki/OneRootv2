package com.example.onerootv2

import android.annotation.SuppressLint
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
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class UploadToCloudActivity : AppCompatActivity() {

    private val allImagePaths = arrayListOf<String>()
    private val allCloudPaths = arrayListOf<String>()
    private var userName = ""

    private lateinit var uploadView: View

    private lateinit var uploadingText: TextView

    private lateinit var progressBar: ProgressBar

    private lateinit var progressCount: TextView

    private var updateCount = 0

    private fun uploadTxtToCloud(folderTxtPath:String, txtCloudPath:String)
    {
        try {
            // TODO try else with if path exists module for internet
            println("<----- Uploading txt from: $folderTxtPath to cloud $folderTxtPath------------>")
            //val uri2 = Uri.fromFile(File(folderTxtPath))
            val fileInputStream = FileInputStream(File(folderTxtPath))
            // start
            // https://firebase.google.com/docs/storage/android/start
            // storage
            // https://firebase.google.com/docs/storage/android/upload-files
            // google cloud setup
            // https://firebase.google.com/docs/android/setup

            // error in bytes -> https://gorkemkara.medium.com/upload-files-to-android-firebase-stroage-4228fdd8d47f

            val storage = Firebase.storage

            // Create a storage reference from our app
            val storageRef = storage.reference.child(txtCloudPath)




            // Upload file and metadata to the path 'images/mountains.jpg'
            if (File(folderTxtPath).exists()) {
                // Get a reference to the file's contents.
                // Upload the file to Firebase Storage.
                storageRef.putBytes(fileInputStream.readBytes()).addOnSuccessListener {
                    // The file was uploaded successfully.
                    Toast.makeText(this, "The text file was uploaded successfully.", Toast.LENGTH_SHORT).show()
                }
            }


//                uploadTask.addOnProgressListener {
//                    val progress = (100.0 * it.bytesTransferred) / it.totalByteCount
//                    println("Upload is $progress% done")
//                }.addOnPausedListener {
//                    println("Upload is paused")
//                }.addOnFailureListener {
//                    // Handle unsuccessful uploads
//                    println("error: uploading image to goggle cloud")
//                }.addOnSuccessListener {
//                    // Handle successful uploads on complete
//                    // ...
//                    println("text uploaded to google successfully")
//                }
//            } else {
//                println("error in uploadToCloudActivity : username was null fail to upload images to cloud")
//            }
        }
        catch (e: Exception) {
            println("error in uploadToCloudActivity/uploadtxtFile() : ")
            e.printStackTrace()
        }

    }

    @SuppressLint("SetTextI18n")
    private  fun getImagePaths()
    {

        progressBar.progress = 0
        progressBar.max = 100

        // send images
        var imagePathname: String
        var cloudPathname: String

        val gPath: String = Environment.getExternalStorageDirectory().absolutePath
        val sPath = "DCIM/one_root_images/"
        val fullPath = gPath + File.separator + sPath
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

        var sessionNo = 0
        val noOfSessions = imageDirectoriesList.size
        for (eachImageDirectory in imageDirectoriesList)
        {
            sessionNo+=1
            uploadingText.text = "session: $sessionNo/${noOfSessions}"

            var imageNo = 0

            val imageFiles = File(eachImageDirectory).listFiles()
            // val noOfImages = imageFiles?.size


            for (file in imageFiles!!) {
                //TODO do condition for images end with .jpeg or other formats
                if (file.name.endsWith(".jpg")) {
                    imageNo +=1
                    // progressCount.text = "$imageNo/$noOfImages"
                    imagePathname = file.path
                    cloudPathname = userName+"_images"+ (file.path.split("one_root_images")[1])

                    // upload single image
                     uploadImageToCloud(imagePathname,cloudPathname)
                    allImagePaths.add(imagePathname)
                    allCloudPaths.add(cloudPathname)
                    println("----> $imagePathname")
                    println("how many images updated count: $updateCount")
                }
            }
        }

        // save paths txt in folder
        val filepath = "ml"
        val fileName  = "cloudPaths.txt"
        val txtFolderPath =  this.getExternalFilesDir(filepath)?.absolutePath+"/" +fileName
        writeTxtFile(txtFolderPath,allCloudPaths)
        // upload to cloud
        val txtCloudPath = userName+"_images"+"/$fileName"
        uploadTxtToCloud(txtFolderPath, txtCloudPath)


    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_to_cloud)
        uploadView = findViewById(R.id.UploadingView)
        uploadingText = findViewById(R.id.uploadTextView)
        progressBar = findViewById(R.id.progressBar)
        progressCount = findViewById(R.id.progressCountText)


        readUsernameFromStorage()
        userName = userName.replace(" ", "").replace(".", "")

        // check internet connection before storing in database
        if (checkForInternet(this)) {
            Toast.makeText(this, "internet available", Toast.LENGTH_SHORT).show()
            // saving profile data in a firebase database
            println("............................")
            getImagePaths()
            println("............................")
            println(" ")

        }
        else
        {
            uploadingText.text = "No internet"
            progressBar.visibility = View.GONE
            progressCount.visibility = View.GONE
            uploadView.background = getDrawable(R.drawable.b2)
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

    private fun writeTxtFile(filePath: String, fileDataArray: MutableList<String>) {
        println("writing files to path: $filePath")
        var fr: FileWriter?= null
        var br: BufferedWriter?= null
        try {
//            val txtLines: MutableList<String> = arrayListOf()
            val file = File(filePath)
            // read old paths
            fileDataArray+= readTxtFile(filePath)

            if (file.exists())
            {
                fr = FileWriter(file)
                br = BufferedWriter(fr)
                for (data in  fileDataArray) {
                    br.write(data+System.getProperty("line.separator"))
                }
                println("$filePath file was saved in storage")
            }
            else
            {
                println("error: no file in path: $filePath")
                fr = FileWriter(file)
                br = BufferedWriter(fr)
                br.write("")
                println("empty file was created")

            }
        }
        catch (e: IOException)
        {
            println("error in UploadToCloud/writeTxtFile() : $filePath cant be written")
            e.printStackTrace()
        }
        finally {
            try {
                br?.close()
                fr?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    private fun readTxtFile(filePath:String): MutableList<String> {
        try {
            val txtLines: MutableList<String> = arrayListOf()
            val file = File(filePath)
            return if (file.exists()) {
                val br = BufferedReader(FileReader(file))
                var st: String
                while (br.readLine().also { st = it } != null) txtLines.add(st)
                txtLines // return txt lines
            }
            else {
                arrayListOf()
            }
        } catch (e: Exception) {

            println("error in UploadToCloud/readTxtFile() : $filePath cant be read")
            e.printStackTrace()
            return arrayListOf()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun uploadImageToCloud(imagePath: String, cloudPath: String)
    {
        try {
            println("<----- Uploading image from: $imagePath to cloud $cloudPath ------------>")
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

            var progress: Int

            // Upload file and metadata to the path 'images/mountains.jpg'
            if ((userName != "")and(File(imagePath).exists())) {

                val uploadTask = storageRef.child(cloudPath)
                    .putFile(uri2, metadata)

                // Listen for state changes, errors, and completion of the upload.
                // You'll need to import com.google.firebase.storage.component1 and
                // com.google.firebase.storage.component2
                uploadTask.addOnProgressListener {
                    progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).toInt()
                    progressCount.text = "${progress} %"
                    println("Upload is $progress% done")
                    progressBar.progress = progress
                }.addOnPausedListener {
                    // Handle paused uploads
                    println("Upload is paused")
                }.addOnFailureListener {
                    // Handle unsuccessful uploads
                    println("error: uploading image to goggle cloud")
                }.addOnSuccessListener {
                    // Handle successful uploads on complete
                    println("image uploaded to google successfully")
                    updateCount+=1
                }
            } else {
                println("error in uploadToCloudActivity : username was null or path doesn't exist fail to upload images to cloud")
            }
        }
        catch (e: Exception) {
            println("error in uploadToCloudActivity/uploadImageToCloud() : ")
            e.printStackTrace()
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        dispatchTakeHomeIntent()
        super.onBackPressed()
    }
    private fun dispatchTakeHomeIntent() {
        val intent = Intent(this, MainActivity::class.java)
        println("home activity closed")
        this.finish()
        startActivity(intent)
    }
}




//fun saveImagePathsInFolder(fileName:String,pathsArrayData: Array<String>)
//{
//    val textFilePath = "ml"
//    // val fileName  = "image_paths.txt"
//    println("errors raised storing errors in txt file")
//    val filePath = this.getExternalFilesDir(textFilePath)
//    val myExternalFile = File(filePath, fileName)
//
//    fun isExternalStorageWritable(): Boolean {
//        val state = Environment.getExternalStorageState()
//        return Environment.MEDIA_MOUNTED == state
//    }
//
//    try
//    {
//        if (isExternalStorageWritable())
//        {
//            if (myExternalFile.exists())
//            {
//                // read old paths
//                println("image_paths.txt file exists read line from it and update")
//                val fileReader = BufferedReader(FileReader(myExternalFile))
//                val text = fileReader.readText()
//                fileReader.close()
//                // add New Path
//                val newText = "$text\n<--------------------------->\n$errorString\n"
//                val fileWriter = FileWriter(myExternalFile)
//                fileWriter.write(newText)
//                fileWriter.close()
//            }
//            else
//            {
//                println("errors.txt file does not exist make a new file and update")
//                val fileWriter = FileWriter(myExternalFile)
//                fileWriter.write("$errorString\n")
//                fileWriter.close()
//            }
//            // Toast.makeText(this, "errors are stored in text file", Toast.LENGTH_SHORT).show()
//            println("<---errors.txt file stored successfully ")
//        }
//        else
//        {
//            Toast.makeText(this, "External storage not available for writing errors.txt", Toast.LENGTH_SHORT).show()
//            println("External storage not available for storing errors.txt file")
//        }
//    }
//    catch (e: Exception) {
//        println("error in videoActivity/saveErrorsInTextToStorage() : errors.txt cant be saved")
//        e.printStackTrace()
//    }
//}