package com.example.onerootv2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

// Recycler view
// https://www.youtube.com/watch?v=VVXKVFyYQdQ

// when recycler item clicked navigate to new view
// https://www.youtube.com/watch?v=WqrpcWXBz14
// https://www.youtube.com/watch?v=dB9JOsVx-yY
// https://www.youtube.com/watch?v=EoJX7h7lGxM
var imagePath = "/DCIM/one_root_images/"
var sessionNo2 = "1"
class SessionHistory : AppCompatActivity()
{
    // create new userRecyclerview to access recyclerview in xml layout
    private lateinit var userRecyclerView: RecyclerView
    // create a new userList using SessionUser.kt class
    private lateinit var userArrayList: ArrayList<SessionUser>
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_history)
        println("user clicked session history")

        userRecyclerView = findViewById(R.id.RecyclerGalleryView)
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.setHasFixedSize(true)

        // creating empty userArray list
        userArrayList = arrayListOf()

        getUserdata()
    }
    private fun getUserdata()
    {
        try {
            // getting session data from session.json

            readSessionFromStorage()

            println("-------------------->user array list: $userArrayList")

            // crated an adapter for user
            val adapter = SessionAdapter(userArrayList)
            userRecyclerView.adapter = adapter

            // when user clicked
            adapter.setOnClickListener(object : SessionAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
//                val context = baseContext
//                Toast.makeText( context,"user clicked item no:$position", Toast.LENGTH_SHORT).show()
                    dispatchGalleryIntent(position)
                }

            })
        }
        catch (e:Exception)
        {
            println("error in SessionHistory/get user data(): failed to get data from storage ")
            e.printStackTrace()
            val errorString = "\n error in SessionHistory/get user data(): failed to get data from storage    \n${e.message} \n $e"
            saveErrorsInTextToStorage(errorString)
        }

    }

    private fun readSessionFromStorage()
    {
        // https://www.youtube.com/watch?v=JUlZYddw03o
        // https://github.com/sandipapps/ExternalStorageDemo/blob/master/app/src/main/java/com/sandipbhattacharya/externalstoragedemo/MainActivity.java

        val filepath = "OneRootFiles"
        val fileName  = "session.json"
        println("reading session.json data on session activity ")

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

        fun getCoconutCountFromListString(listChar: String):Int{
            var coconutCount = 0
            val listCharSplit = listChar.replace("[","").replace("]","").replace(" ","").split(",")
            for (char1 in listCharSplit) {
                // newList.add(char1.toInt())
                coconutCount += char1.toInt()
            }
            return coconutCount
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
                    // create a sessionUser class
                    val sessionUserData = SessionUser()

                    val jsonObject = jsonObjectList.getJSONObject(i)
                    convertedJsonObjectList.add(jsonObject)
//                    val name = jsonObject["sessionNo"]
//                    println("jsonObject: $jsonObject, name: $name")
                    sessionUserData.sessionNo = "session"+(jsonObject.getString("sessionNo"))
                    sessionUserData.sessionType = jsonObject.getString("sessionType")
                    sessionUserData.sessionDate = jsonObject.getString("date")
                    val stringList = jsonObject.getString("detectionData")
                    sessionUserData.coconutCount = getCoconutCountFromListString(stringList)
                    userArrayList.add(sessionUserData)
                }
                // println("convertedJsonObjectList: $convertedJsonObjectList")

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
            println("error in recyclerViewGalleryActivity: session json file cant be read")
            println(e)
            e.printStackTrace()
        }
        finally {
            println("session data read from folder successfully")
        }
    }

    private fun saveErrorsInTextToStorage(errorString: String)
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
            println("error in GalleryActivity/saveErrorsInTextToStorage() : errors.txt cant be saved")
            e.printStackTrace()
        }



    }
    private fun dispatchGalleryIntent(position1: Int)
    {
        try{
            val sUser = userArrayList[position1]
            val k = sUser.sessionNo
            sessionNo2 = k.toString()
            val bundle = Bundle()
            bundle.clear()
            imagePath = "/DCIM/one_root_images/"
            imagePath += sessionNo2
            bundle.putString("path", imagePath)

            println("SessionHistory.kt/dispatchGalleryIntent()/ getting images from $imagePath ")
            val i = Intent(this, GalleryActivity::class.java)
            i.putExtras(bundle)
            startActivity(i)

            // clearing image path
            imagePath = "/DCIM/one_root_images/session"
            bundle.clear()
            // (activity as Activity?)!!.overridePendingTransition(0, 0)
        }
        catch (e: Exception)
        {
            println("error in sessionHistory/reading from bundle(): failed to get path userList ")
            e.printStackTrace()
            val errorString = "\n error in sessionHistory/reading from bundle(): failed to get path from userList  \n${e.message}\n $e"
            saveErrorsInTextToStorage(errorString)
        }
    }

    private fun dispatchTakeHomeIntent() {
        val intent = Intent(this, MainActivity::class.java)
        println("home activity closed")
        startActivity(intent)
        this.finish()
    }

    @Deprecated("Deprecated in Java",
        ReplaceWith("super.onBackPressed()", "androidx.appcompat.app.AppCompatActivity")
    )
    override fun onBackPressed() {
        super.onBackPressed()
        dispatchTakeHomeIntent()
    }
}