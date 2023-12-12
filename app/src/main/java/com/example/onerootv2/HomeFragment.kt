package com.example.onerootv2

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.onerootv2.MainActivity.Companion.TAG
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonArray
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
private var  numberOfCoconuts= ""
private var numberOfSessions = ""
private var location = ""
private var role = ""
private var profileDataLoaded = false
private var status =  ""
private var command = "no commands"
class HomeFragment : Fragment() {

    private lateinit var loadButtonEvent: Button
    private lateinit var unLoadButtonEvent: Button
    private lateinit var resumeSessionButton: Button
    private lateinit var sessionHistoryButton: ImageButton

    // creating database
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        // inflate the layout and bind to the _binding
        val view = inflater.inflate(R.layout.fragment_home,container,false)
        loadButtonEvent = view.findViewById(R.id.LoadButton)!!
        unLoadButtonEvent = view.findViewById(R.id.unLoadButton)!!
        resumeSessionButton = view.findViewById(R.id.sessionButton)!!
        sessionHistoryButton = view.findViewById(R.id.sessionHistoryButton)!!
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // reading profile.json from storage
        readProfileFromStorage()

        val sharedPref = activity?.getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val editor = sharedPref?.edit()
        val sessionStatus = sharedPref?.getInt("session",5)

        // check for profile status updated or not
        val profileStoredInDb = sharedPref?.getBoolean("profileDb",true)
        val sessionStoredInDb = sharedPref?.getBoolean("sessionDb",true)
        val commandNotUpdated = sharedPref?.getBoolean("commandUpdate",true)

        // check internet for getting command from firebase
        if (( activity?.let { checkForInternet(it) } == true)&&(commandNotUpdated!!)) {
            //Toast.makeText(activity, "internet available", Toast.LENGTH_SHORT).show()
            // get CommandFrom Firebase else get command from profile.json when offline
            command = getCommandFromFireBase()
            println("----------------> command from firebase: $command")
        }
        else
        {
            println("command from firebase not available cause of no internet connection")
            command = getCommandFromProfileJson()
            println("----------------> command from profile.json: $command")
        }

        if (command == "uninstall")
        {
            println("command to uninstall app")
            startAppUninstallActivity()
            updateStatusToFirebase("user was commanded to uninstall")
        }
        if (command == "uploadGallery")
        {
            println("command to update gallery to firebase")
            startUploadToCloudActivity()
            updateStatusToFirebase("user was commanded to update gallery")
        }

//        if (command == "update")
//        {
//            //TODO redirect to updateAppActivity()
//        }


        // checking profile upload ------------------------------------------------------->
        if (profileStoredInDb != true)
        {

            println("profile data not uploaded to firebase trying to upload again")
            // reading profile data from storage
            readProfileFromStorage()
            if(profileDataLoaded) {
                try
                {
                    println("<--------trying to upload profile data to database again ------------->")
                    // check internet connection before storing in database
                    if (activity?.let { it1 -> checkForInternet(it1) } == true) {
                        Toast.makeText(activity, "internet available", Toast.LENGTH_SHORT).show()
                        // saving profile data in a firebase database
                        println("............................")
                        saveProfileDataToFirebase()
                        println("............................")
                        println(" ")
                        // reading From database
                        //readProfileDataFromFirebase()
                    }
                    else
                    {

                        Toast.makeText(activity, "internet  not available", Toast.LENGTH_SHORT)
                            .show()
                        // <------------------ alert box ----------------------------------->
                        // https://www.digitalocean.com/community/tutorials/android-alert-dialog-using-kotlin
                        // alert color problem https://stackoverflow.com/questions/39481735/alertdialog-do-not-show-positive-and-negative-button
                        val builder = activity?.let { it1 -> AlertDialog.Builder(it1) }
                        //set title for alert dialog
                        builder?.setTitle("internet alert")
                        //set message for alert dialog
                        builder?.setMessage("Please connect to internet")
                        builder?.setIcon(android.R.drawable.ic_popup_reminder)

                        // get wifi settings
                        //val wifi = activity?.getSystemService(WIFI_SERVICE) as WifiManager
                        builder?.setPositiveButton("connect to internet") { _, _ ->
                            //startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                            startActivity( Intent(Settings.ACTION_WIFI_SETTINGS))
                            //wifi.isWifiEnabled = true
                            // save profile data when wifi enabled
                            saveProfileDataToFirebase()
                        }
                        builder?.setNegativeButton("cancel") { _, _ ->
                            //wifi.isWifiEnabled = false

                        }

                        // colors for alert buttons
                        val alert = builder?.create()
                        alert?.setOnShowListener {
                            alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(Color.BLUE)
                            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(Color.BLUE)
                        }
                        alert?.show()
                    }
                }
                catch (e: Exception)
                {
                    println("error in home fragment/ trying to upload profile data in database again failed :")
                    println(e)
                }
            }
            else
            {
                println("error in Home Fragment/profile data:loading profile data from external storage failed")
            }

            updateStatusToFirebase("profile data not uploaded by $userName trying to upload again")

        }
        else
        {
            println("profile data uploaded to firebase no need to upload again")
        }

        if (sessionStoredInDb!= true)
        {
            try
            {
                println("<--------trying to upload session data to database again ------------->")
                // check internet connection before storing in database
                if (activity?.let { it1 -> checkForInternet(it1) } == true) {
                    Toast.makeText(activity, "internet available", Toast.LENGTH_SHORT).show()
                    // saving profile data in a firebase database
                    println("............................")
                    storeSessionDataToFirebase()
                    println("............................")
                    println(" ")
                }
                else
                {

                    Toast.makeText(activity, "internet  not available", Toast.LENGTH_SHORT)
                        .show()
                    // <------------------ alert box ----------------------------------->
                    // https://www.digitalocean.com/community/tutorials/android-alert-dialog-using-kotlin
                    // alert color problem https://stackoverflow.com/questions/39481735/alertdialog-do-not-show-positive-and-negative-button
                    val builder = activity?.let { it1 -> AlertDialog.Builder(it1) }
                    //set title for alert dialog
                    builder?.setTitle("internet alert")
                    //set message for alert dialog
                    builder?.setMessage("Please connect to internet")
                    builder?.setIcon(android.R.drawable.ic_popup_reminder)

                    // get wifi settings
                    //val wifi = activity?.getSystemService(WIFI_SERVICE) as WifiManager
                    builder?.setPositiveButton("connect to internet") { _, _ ->
                        //startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                        startActivity( Intent(Settings.ACTION_WIFI_SETTINGS))
                        //wifi.isWifiEnabled = true
                        // save session data when wifi enabled
                        storeSessionDataToFirebase()
                    }
                    builder?.setNegativeButton("cancel") { _, _ ->
                        //wifi.isWifiEnabled = false

                    }

                    // colors for alert buttons
                    val alert = builder?.create()
                    alert?.setOnShowListener {
                        alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(Color.BLUE)
                        alert.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setTextColor(Color.BLUE)
                    }
                    alert?.show()
                }
            }
            catch (e: Exception)
            {
                println("error in home fragment/ trying to upload session data in database again failed :")
                println(e)
            }
        }
        else
        {
            println("session data uploaded to firebase no need to upload again")
        }

        println("sessionStatus: $sessionStatus")

        when (sessionStatus) {
            // loading paused
            3 -> {
                println("user paused loading")
                // disable other buttons
                loadButtonEvent.isEnabled = false
                loadButtonEvent.visibility = View.GONE
                unLoadButtonEvent.isEnabled = false
                unLoadButtonEvent.visibility = View.GONE
                sessionHistoryButton.isEnabled = false
                sessionHistoryButton.visibility = View.GONE

                // enable resume button
                resumeSessionButton.isEnabled = true
                resumeSessionButton.visibility = View.VISIBLE

                // press resume session
                resumeSessionButton.setOnClickListener { dispatchTakeVideoIntent() }

            }
            // unloading paused
            4 -> {
                println("user paused unloading")
                // disable other buttons
                loadButtonEvent.isEnabled = false
                loadButtonEvent.visibility = View.GONE
                unLoadButtonEvent.isEnabled = false
                unLoadButtonEvent.visibility = View.GONE
                sessionHistoryButton.isEnabled = false
                sessionHistoryButton.visibility = View.GONE

                // enable resume button
                resumeSessionButton.isEnabled = true
                resumeSessionButton.visibility = View.VISIBLE

                // press resume session
                resumeSessionButton.setOnClickListener { dispatchTakeVideoIntent() }
            }
            else -> {
                // disable pause session button
                resumeSessionButton.isEnabled = false
                resumeSessionButton.visibility = View.GONE

                // show other buttons
                loadButtonEvent.isEnabled = true
                loadButtonEvent.visibility = View.VISIBLE
                unLoadButtonEvent.isEnabled = true
                unLoadButtonEvent.visibility = View.VISIBLE
                sessionHistoryButton.isEnabled = true
                sessionHistoryButton.visibility = View.VISIBLE

                println("user was choosing: ")
                updateStatusToFirebase("user completed last session user was choosing loading or unloading")
                loadButtonEvent.setOnClickListener {
                    try {
                        println("user chosen loading")
                        updateStatusToFirebase("user clicked loading, session was going on")
                        // loading play
                        editor?.apply {
                            putInt("session", 1)
                            apply() //asynchronously
                        }
                        // take video
                        dispatchTakeVideoIntent()

                    } catch (e: ActivityNotFoundException)
                    {
                        Log.e(TAG, e.message.toString())
                    }
                }
                unLoadButtonEvent.setOnClickListener {
                    try {
                        // unloading play
                        println("user chosen unloading")
                        updateStatusToFirebase("user clicked unloading, session was going on")
                        editor?.apply {
                            putInt("session", 2)
                            apply() //asynchronously
                        }
                        // take video
                        dispatchTakeVideoIntent()
                    } catch (e: ActivityNotFoundException) {
                        Log.e(TAG, e.message.toString())
                    }
                }

                sessionHistoryButton.setOnClickListener {
                    try
                    {
                        dispatchSessionHistoryActivity()
                    }
                    catch (e: ActivityNotFoundException) {
                        println("error in opening dispatchSessionHistoryActivity()")
                        Log.e(TAG, e.message.toString())
                    }

                }
            }
        }
    }

    override fun onDestroyView() {
        updateStatusToFirebase("user was offline")
        super.onDestroyView()
    }

    private fun replaceFragment(fragment : Fragment){

        val fragmentManager = activity?.supportFragmentManager
        val fragmentTransaction = fragmentManager?.beginTransaction()
        fragmentTransaction?.replace(R.id.frame_layout,fragment)
        fragmentTransaction?.commit()
    }

    // activity or fragment dispatch functions
    private fun dispatchTakeVideoIntent() {
        val intent = Intent(activity, VideoActivity::class.java)
        startActivity(intent)
        println("home activity closed")
        replaceFragment(HomeFragment())
        activity?.finish()
    }
    private fun dispatchSessionHistoryActivity() {
        println("user trying to open session History")
        val intent = Intent(activity, SessionHistory::class.java)
        startActivity(intent)
        println("Home activity closed")
        replaceFragment(HomeFragment())
        activity?.finish()
    }
    private fun startAppUninstallActivity(){
        val intent = Intent(activity, AppUninstallActivity::class.java)
        startActivity(intent)
        println("home activity closed")
        activity?.finish()
    }
    private fun startUploadToCloudActivity(){
        val intent = Intent(activity, UploadToCloudActivity::class.java)
        startActivity(intent)
        println("home activity closed")
        activity?.finish()
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

    // profile related functions
    private fun readProfileFromStorage()
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
                println("reading from profile json data file")
                val dataFromJson = readFromExternalStorage()
                println(" json data:  $dataFromJson")
                val jsonObject = JSONObject(dataFromJson)
                userName = jsonObject.getString("username")
                mobileNo = jsonObject.getString("mobileNo")
                numberOfCoconuts = jsonObject.getString("numberOfCoconuts")
                numberOfSessions = jsonObject.getString("numberOfSessions")
                location = jsonObject.getString("location")
                role = jsonObject.getString("role")
                status =  jsonObject.getString("status")
                command = jsonObject.getString("command")



//                println("username from file: $userName")
//                println("mobile No from file: $mobileNo")

                // updating status
                profileDataLoaded = true

            }
            else
            {
                // updating status
                profileDataLoaded = false
                Toast.makeText(
                    context,
                    "External storage not available for reading",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        catch (e: JSONException)
        {
            // updating status
            profileDataLoaded = false

            println("error in HomeFragment/readProfileFromStorage(): failed to read profile.json ")
            e.printStackTrace()
            val errorString = "\n error in HomeFragment/readProfileFromStorage(): failed to read profile.json \n${e.message} \n $e"
            saveErrorsInTextToStorage(errorString)
        }
        finally {
            println("profile data read from folder successfully")
        }
    }
    private fun saveProfileDataToFirebase()
    {

        //shared preferences
        val sharedPref = activity?.getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val editor = sharedPref?.edit()

        // firebase activation
        //https://www.youtube.com/watch?v=rFTJTLdoGDY&list=PLHQRWugvckFry9Q1OT6hLNfyUizT73PwX&index=2
        try
        {
            val user = hashMapOf(
                "username" to userName,
                "mobileNo" to mobileNo,
                "numberOfCoconuts" to numberOfCoconuts,
                "numberOfSessions" to numberOfSessions,
                "location" to location,
                "role" to role,
                "status" to "profile saved again when internet turned on",
                "command" to "no commands"
            )

            // Add a new document with a generated ID
            val documentName = userName.replace(" ","").replace(".","")+ "Data"
            db.collection("users").document(documentName)
                .set(user)
                .addOnSuccessListener {
//                    Toast.makeText(activity, "firebase profile data updated", Toast.LENGTH_SHORT).show()
                    println("<-------------database updated ------------->")
                    Log.d(TAG, "DocumentSnapshot added with ID:$documentName")

                    // updating db status
                    editor?.apply {
                        putBoolean("profileDb",true)
                        apply()
                    }
                }
                .addOnFailureListener { e ->
                    println("********************************************************")
                    Log.w(TAG, "failed adding profile document to firebase", e)

                    // updating db status
                    editor?.apply {
                        putBoolean("profileDb",false)
                        apply()
                    }
                }
        }
        catch (e:Exception)
        {
            println("error in HomeFragment/saveProfileDataToFirebase(): failed to save profile to firebase ")
            e.printStackTrace()
            val errorString = "\n error in HomeFragment/saveProfileDataToFirebase(): failed to save  to firebase   \n${e.message}"
            saveErrorsInTextToStorage(errorString)

            // updating db status
            editor?.apply {
                putBoolean("profileDb",false)
                apply()
            }
        }


    }


    // command related functions
    private  fun getCommandFromProfileJson(): String {
        var command1 = "getCommandFromProfileJson"
        try
        {
            val filepath = "OneRootFiles"
            val fileName = "profile.json"
            val filePath = context?.getExternalFilesDir(filepath)
            val myExternalFile = File(filePath, fileName)

            println("getting command in profile.json")

            fun isExternalStorageWritable(): Boolean {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state
            }

            if (isExternalStorageWritable()) {
                // get json object from path
                val jsonObject = JSONObject(myExternalFile.readText())
                command1 = jsonObject.getString("command")

                // Toast.makeText(activity, "profile json data stored on registration", Toast.LENGTH_SHORT).show()
                println("<--- command in profile.json: $command1")

            }
            else
            {
                Toast.makeText(
                    activity,
                    "External storage not available for getting command in profile json",
                    Toast.LENGTH_SHORT
                ).show()
                println("External storage not available for getting command in profile.json file")
            }

        }
        catch (e: Exception)
        {
            println("error in HomeFragment/getCommandFromProfileJson(): getting command from profile.json ")
            e.printStackTrace()
            val errorString = "\n error in HomeFragment/getCommandFromProfileJson(): getting command from  profile.json. \n${e.message}"
            saveErrorsInTextToStorage(errorString)
            command1 = "error getCommandFromFireBase"
        }
        return command1
    }
    private fun getCommandFromFireBase(): String {
        var command2 = "getCommandFromFireBase"
        try
        {
            val documentName = userName.replace(" ", "").replace(".", "") + "Data"

            // Get the document
            val documentReference = db.collection("users").document(documentName)
            documentReference.get().addOnSuccessListener { documentSnapshot ->
                // Get the field value
                command2 = documentSnapshot.get("command").toString()
                // Do something with the field value
                println("command successfully obtained: $command2")

                // updating only command in profile.json instead of updating everything
                updateCommandInProfileJson(command2)

                // put command updated
                //shared preferences
                val sharedPref = activity?.getSharedPreferences("myPref", Context.MODE_PRIVATE)
                val editor = sharedPref?.edit()

                editor?.apply {
                    putBoolean("commandUpdate", false)
                    apply() //asynchronously
                }

                // updating profile.json
                println("command from firebase updated to profile.json")
            }.addOnFailureListener {
                    e -> Log.w(TAG, "Error getCommandFromFireBase ", e)
                 command2 = "failed to get command From FireBase"

                // updating only command in profile.json instead of updating everything
                updateCommandInProfileJson(command2)
            }

        }
        catch (e: Exception)
        {
            println("error in HomeFragment/getCommandFromFireBase(): getting command from firebase failed ")
            e.printStackTrace()
            val errorString = "\n error in HomeFragment/getCommandFromFireBase(): getting command from firebase failed. \n${e.message}"
            saveErrorsInTextToStorage(errorString)
            command2 = "error getCommandFromFireBase"

            // updating only command in profile.json instead of updating everything
            updateCommandInProfileJson(command2)
        }

        return command2

    }
    private  fun updateCommandInProfileJson(command3:String)
    {
        try
        {
            val filepath = "OneRootFiles"
            val fileName = "profile.json"
            val filePath = context?.getExternalFilesDir(filepath)
            val myExternalFile = File(filePath, fileName)

            println("updating command in profile.json")

            fun isExternalStorageWritable(): Boolean {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state
            }

            if (isExternalStorageWritable()) {
                // get json object from path
                val jsonObject = JSONObject(myExternalFile.readText())
                //val commandObject = jsonObject.getJSONObject("command")
                jsonObject.put("command", command3)
                val fileWriter1 = FileWriter(myExternalFile)
                fileWriter1.write(jsonObject.toString())
                fileWriter1.close()

                // Toast.makeText(activity, "profile json data stored on registration", Toast.LENGTH_SHORT).show()
                println("<--- command in profile.json file updated: $command3")
            } else {
                Toast.makeText(
                    activity,
                    "External storage not available for writing command in profile json",
                    Toast.LENGTH_SHORT
                ).show()
                println("External storage not available for updating command in profile.json file")
            }
        }
        catch (e: Exception)
        {
            println("error in HomeFragment/updateCommandInProfileData(): failed to update command in profile.json ")
            e.printStackTrace()
            val errorString = "\n error in HomeFragment/updateCommandInProfileData(): failed to update command in profile.json \n${e.message}"
            saveErrorsInTextToStorage(errorString)
        }

    }


    // status related functions
    private fun updateStatusToProfileJson(statusString1: String)
    {
        try
        {
            val filepath = "OneRootFiles"
            val fileName = "profile.json"
            val filePath = context?.getExternalFilesDir(filepath)
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
                    activity,
                    "External storage not available for writing status in profile json",
                    Toast.LENGTH_SHORT
                ).show()
                println("External storage not available for updating status in profile.json file")
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
    private fun updateStatusToFirebase(statusText: String)
    {
        // https://saveyourtime.medium.com/firebase-cloud-firestore-add-set-update-delete-get-data-6da566513b1b
        // https://firebase.google.com/docs/firestore/manage-data/add-data

        // firestore update
        // https://firebase.google.com/docs/firestore/manage-data/add-data
        try
        {
            if (activity?.let { it1 -> checkForInternet(it1) } == true)
            {
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
            println("error in HomeFragment/updateStatusToFirebase(): failed to update status to firebase ")
            e.printStackTrace()
            val errorString = "\n error in HomeFragment/updateStatusToFirebase(): failed to update status to firebase  \n${e.message}"
            saveErrorsInTextToStorage(errorString)
        }
        // updating status in profile.json
        updateStatusToProfileJson(statusText)
    }
    // store session in firebase
    private fun storeSessionDataToFirebase()
    {
        //shared preferences
        val sharedPref = activity?.getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val editor = sharedPref?.edit()

        println("<---------------------- storing session data in firebase ------------------------>")
        // firebase activation
        //https://www.youtube.com/watch?v=rFTJTLdoGDY&list=PLHQRWugvckFry9Q1OT6hLNfyUizT73PwX&index=2
        try {
            val filepath = "OneRootFiles"
            val fileName  = "session.json"
            println("sending detection json data to firebase")

            fun isExternalStorageReadable(): Boolean {
                val state = Environment.getExternalStorageState()
                return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
            }

            fun readFromExternalStorage(): JsonArray? {
                val myExternalFile = File(activity?.getExternalFilesDir(filepath), fileName)
                try {
                    if (myExternalFile.exists() and isExternalStorageReadable())
                    {
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
                        }
                        catch (e: IOException) {
                            e.printStackTrace()
                        }
                        // convert to json array object
                        val jsonArrayData = Gson().fromJson(stringBuilder.toString(), JsonArray::class.java)
                        println("***** json array data reading from external storage: $jsonArrayData")
                        return jsonArrayData
                    }
                    else
                    {
                        println("no session json file exists returning empty json array")
                    }
                }
                catch (e: JSONException) {
                    println("error in video activity: json file cant be read from external storage")
                    e.printStackTrace()
                }
                return JsonArray()
            }

            val sessionDataFromFolder = Gson().toJson(readFromExternalStorage())

            val sessionData = hashMapOf(
                "sessionData" to sessionDataFromFolder
            )
            println("sessionData $sessionData")

            // Add a new document with a generated ID
            val documentName1 = sessionUser+"SessionData"
            db.collection("session").document(documentName1).set(sessionData)
                .addOnSuccessListener {
                    //Toast.makeText(activity, "firebase session updated again", Toast.LENGTH_SHORT).show()
                    println("session data updated successfully")
                    Log.d(TAG, "DocumentSnapshot added with ID: data")

                    // update session status
                    editor?.apply {
                        putBoolean("sessionDb",true)
                        apply() //asynchronously
                    }

                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document to firebase", e)

                    // update session status
                    editor?.apply {
                        putBoolean("sessionDb",false)
                        apply() //asynchronously
                    }
                }

        }
        catch (e: Exception)
        {
            println("error in HomeFragment/storeSessionDataToFirebase(): failed to store session data to firebase ")
            e.printStackTrace()
            val errorString = "\n error in HomeFragment/storeSessionDataToFirebase(): failed to store session data to firebase   \n${e.message}"
            saveErrorsInTextToStorage(errorString)

            // update session status
            editor?.apply {
                putBoolean("sessionDb",false)
                apply() //asynchronously
            }
        }
        finally
        {
            println("session data stored successfully in database")
        }

    }


//    private fun restartActivity() {
//        super.onDestroy()
//        val i = Intent(activity, MainActivity::class.java)
//        activity?.finish()
//        activity?.overridePendingTransition(0, 0)
//        startActivity(i)
//        activity?.overridePendingTransition(0, 0)
//    }

//    private fun returnFragmentBack() {
//        activity?.supportFragmentManager?.popBackStack()
//
//    }

}
