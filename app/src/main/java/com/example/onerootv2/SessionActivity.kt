package com.example.onerootv2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException


class SessionActivity : AppCompatActivity() {
    private lateinit var sessionView: ImageView
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private var id2 = 0
    private var noOfFiles = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readSessionFromStorage()
        setContentView(R.layout.activity_session)
        sessionView = findViewById(R.id.sessionImageView)

        previousButton = findViewById(R.id.previousButton)

        nextButton = findViewById(R.id.nextButton)

        val gpath: String = Environment.getExternalStorageDirectory().absolutePath
        val spath = "DCIM/one_root_images/session1/"
        val fullPath = File(gpath + File.separator + spath+ File.separator)

        val files = fullPath.listFiles()
        val imagePaths = mutableListOf<String>()
        for (file in files!!) {
            if (file.name.endsWith(".jpg")) {
                imagePaths.add(file.path)
            }
        }

        //imageReaderNew(fullPath)
        println("full path: $fullPath")
        println("imagePaths: $imagePaths")

        noOfFiles = imagePaths.size



        previousButton.setOnClickListener{
            id2-=1
            println("previous button clicked")

            if (id2<=0){
                id2 = 0
            }
            if (noOfFiles>1)
            {
                if (id2>=noOfFiles-1){
                    id2 = noOfFiles-1
                }
            }
            else{
                println("error :failed to list files")
            }


            val image_path1 = imagePaths[id2]
            println("image loading from $image_path1")
            val uri = Uri.fromFile(File(image_path1))
            // Set the image on the ImageView.
            sessionView.setImageURI(uri)
            sendImageToGoogleCloud(uri)
        }


        nextButton.setOnClickListener{
            id2+=1
            println("next button clicked")

            if (id2<=0){
                id2 = 0
            }
            if (noOfFiles>1)
            {
                if (id2>=noOfFiles-1){
                    id2 = noOfFiles-1
                }
            }
            else{
                println("error :failed to list files")
            }


            val image_path1 = imagePaths[id2]
            println("image loading from $image_path1")
            val uri = Uri.fromFile(File(image_path1))
            // Set the image on the ImageView.
            sessionView.setImageURI(uri)
            sendImageToGoogleCloud(uri)
        }


    }


    private fun sendImageToGoogleCloud(uri1: Uri){
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
        val uploadTask = storageRef.child("images/${uri1.lastPathSegment}").putFile(uri1, metadata)

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


    private fun readSessionFromStorage()
    {
        // https://www.youtube.com/watch?v=JUlZYddw03o
        // https://github.com/sandipapps/ExternalStorageDemo/blob/master/app/src/main/java/com/sandipbhattacharya/externalstoragedemo/MainActivity.java

        val filepath = "OneRootFiles"
        val fileName  = "session.json"
        println("reading session json data on session activity ")

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
                println("reading from session json data file")
                val dataFromJson = readFromExternalStorage()
                //println(" json data:  $dataFromJson")

                val convertedJsonObjectList:MutableList<JSONObject> = arrayListOf()

                // converting to json object list
                // https://www.baeldung.com/kotlin/iterate-over-jsonarray
                val jsonObjectList = JSONArray(dataFromJson)
                for (i in 0 until jsonObjectList.length()) {
                    val jsonObject = jsonObjectList.getJSONObject(i)
                    convertedJsonObjectList.add(jsonObject)
//                    val name = jsonObject["sessionNo"]
//                    println("jsonObject: $jsonObject, name: $name")
                }
                println("convertedJsonObjectList: $convertedJsonObjectList")

            }
            else
            {
                // updating status

                Toast.makeText(
                    this,
                    "External storage not available for reading",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        catch (e: JSONException)
        {
            println("error in Home fragment: session json file cant be read")
            println(e)
            e.printStackTrace()
        }
        finally {
            println("session data read from folder successfully")
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
        startActivity(intent)
        this.finish()
    }

}