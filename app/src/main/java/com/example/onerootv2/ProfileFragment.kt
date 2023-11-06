package com.example.onerootv2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException

private var userName = ""
private var mobileNo = ""
private var  numberOfCoconuts= ""
private var numberOfSessions = ""
private var location = ""
private var role = ""

class ProfileFragment : Fragment() {
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // shared preference
//        val sharedPref = activity?.getSharedPreferences("myPref", Context.MODE_PRIVATE)
//        val userName = sharedPref?.getString("username", null)
//        val noOfCoconuts = sharedPref?.getInt("imageNo", 0)!!
//        val sessionsNo = sharedPref.getInt("sessionNo", 0)

        // read profile data from storage
        readProfileFromStorage()


        // Inflate the layout for this fragment
        val view1 = inflater.inflate(R.layout.fragment_profile, container, false)

        // put user name in profile
        val profileView = view1.findViewById<TextView>(R.id.userNameView)
        profileView.text = userName

        // put location in profile
        val locationView = view1.findViewById<TextView>(R.id.locationView)
        locationView.text = location

        // displaying no of coconuts collected
        val coconutElement = view1.findViewById<TextView>(R.id.coconutNo)
        coconutElement.text = numberOfCoconuts

        val sessionElement = view1.findViewById<TextView>(R.id.SessionsNo)
        sessionElement.text = numberOfSessions

        // gallery button press
        val galleryButton = view1.findViewById<Button>(R.id.gallery_button)
        galleryButton?.setOnClickListener {
            dispatchGalleryIntent()
        }

        val uploadButton = view1.findViewById<Button>(R.id.cloud_upload_button)
        uploadButton?.setOnClickListener {
            dispatchUploadActivity()
        }

        return view1

    }
    private fun dispatchGalleryIntent()
    {
        val i = Intent(activity, GalleryActivity::class.java)
        startActivity(i)
        (activity as Activity?)!!.overridePendingTransition(0, 0)
    }

    private fun dispatchUploadActivity()
    {
        val i = Intent(activity, UploadToCloudActivity::class.java)
        startActivity(i)
    }

    private fun readProfileFromStorage()
    {
        // https://www.youtube.com/watch?v=JUlZYddw03o
        // https://github.com/sandipapps/ExternalStorageDemo/blob/master/app/src/main/java/com/sandipbhattacharya/externalstoragedemo/MainActivity.java

        val filepath = "OneRootFiles"
        val fileName  = "profile.json"
        println("storing detection json data on video activity")
        println("storing json data")

        fun isExternalStorageReadable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

        fun readFromExternalStorage(): String {
            val myExternalFile = File(activity?.getExternalFilesDir(filepath), fileName)
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
                mobileNo = jsonObject.getString("mobileNo")
                numberOfCoconuts = jsonObject.getString("numberOfCoconuts")
                numberOfSessions = jsonObject.getString("numberOfSessions")
                location = jsonObject.getString("location")
                role = jsonObject.getString("role")

//                println("username from file: $userName")
//                println("mobile No from file: $mobileNo")

            } else {
                Toast.makeText(
                    context,
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

}
