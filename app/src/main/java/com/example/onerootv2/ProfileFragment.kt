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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

private var userName = ""
private var mobileNo = ""
private var  lastSessionCoconuts= 0
private var numberOfSessions = ""
private var location = ""
private var role = ""
private var sessionDataReadable = false


var sessionStatus = ""
var sessionType = ""
var sessionNo = 0
var sessionUser = ""
var sessionData = ""
var detectionList = mutableListOf<Int>()
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

        // read session data from storage
        readSessionFromStorage()
        if (sessionDataReadable)
        {
            // getting no of coconuts from last session
            println("session data from session.json $sessionData")

            detectionList = convertListCharToList(sessionData)

            println("after converting session data to list of integers detection data: $detectionList")
        }




        // Inflate the layout for this fragment
        val view1 = inflater.inflate(R.layout.fragment_profile, container, false)

        // put user name in profile
        val profileView = view1.findViewById<TextView>(R.id.userNameView)
        profileView.text = userName

        // put location in profile
        val locationView = view1.findViewById<TextView>(R.id.locationView)
        locationView.text = location

        // displaying no of coconuts collected
        val coconutElement = view1.findViewById<TextView>(R.id.LastCoconutNo)
        coconutElement.text = lastSessionCoconuts.toString()

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

    override fun onDestroy() {
        super.onDestroy()
        lastSessionCoconuts= 0
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

    private fun saveErrorsInTextToStorage(errorString: String)
    {
        val textFilePath = "OneRootFiles"
        val fileName  = "errors.txt"
        println("errors raised storing errors in txt file")
        val filePath = activity?.getExternalFilesDir(textFilePath)
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
                Toast.makeText(activity, "External storage not available for writing errors.txt", Toast.LENGTH_SHORT).show()
                println("External storage not available for storing errors.txt file")
            }
        }
        catch (e: Exception) {
            println("error in videoActivity/saveErrorsInTextToStorage() : errors.txt cant be saved")
            e.printStackTrace()
        }



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
                println("reading from profile.json data file")
                val dataFromJson = readFromExternalStorage()
                println(" json data:  $dataFromJson")
                val jsonObject = JSONObject(dataFromJson)
                userName = jsonObject.getString("username")
                mobileNo = jsonObject.getString("mobileNo")
                // numberOfCoconuts = jsonObject.getString("numberOfCoconuts")
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
        catch (e: JSONException)
        {
            println("error in profileFragment/readProfileFromStorage(): failed to read profile.json ")
            e.printStackTrace()
            val errorString = "\n error in profileFragment/readProfileFromStorage(): failed to read profile.json \n${e.message}"
            saveErrorsInTextToStorage(errorString)
        }
        finally {
            println("profile data read from folder successfully")
        }
    }

    private fun readSessionFromStorage()
    {
        // https://www.youtube.com/watch?v=JUlZYddw03o
        // https://github.com/sandipapps/ExternalStorageDemo/blob/master/app/src/main/java/com/sandipbhattacharya/externalstoragedemo/MainActivity.java

        val filepath = "OneRootFiles"
        val fileName  = "session.json"
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
                println("reading from session.json data file")
                val dataFromJson2 = readFromExternalStorage()
                println(" json data:  $dataFromJson2")
                // from json array get last json object
                val jsonObjectArray = JSONArray(dataFromJson2)
                val jsonObject1 = jsonObjectArray.getJSONObject(jsonObjectArray.length()-1)
//                sessionNo = jsonObject1.getInt("sessionNo")
//                sessionType = jsonObject1.getString("sessionType")
                sessionData = jsonObject1.getString("detectionData")
//                time = jsonObject1.getString("time")
//                date = jsonObject1.getString("date")
//                sessionUser = jsonObject1.getString("sessionUser")
                sessionDataReadable = true

            } else {
                sessionDataReadable = false
                Toast.makeText(
                    context,
                    "External storage not available for reading",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        catch (e: JSONException)
        {
            sessionDataReadable = false
            println("error in profileFragment/readSessionFromStorage(): failed to read session.json ")
            e.printStackTrace()
            val errorString = "\n error in profileFragment/readSessionFromStorage(): failed to read session.json  \n${e.message}"
            saveErrorsInTextToStorage(errorString)
        }
        finally {
            println("session data read from folder successfully")
        }
    }

    private fun convertListCharToList(listChar: String): MutableList<Int> {
        val newList = mutableListOf<Int>()
        val listCharSplit = listChar.replace("[","").replace("]","").replace(" ","").split(",")
        for (char1 in listCharSplit) {
                newList.add(char1.toInt())
            lastSessionCoconuts += char1.toInt()
        }
        return newList
    }



}
