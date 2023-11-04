package com.example.onerootv2

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException


class SessionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readSessionFromStorage()
        setContentView(R.layout.activity_session)
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