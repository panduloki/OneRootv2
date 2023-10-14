package com.example.onerootv2

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.onerootv2.ml.Coconut400
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.text.DateFormat
import java.text.DateFormat.getDateInstance
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.sqrt

private var userName = ""
private var mobileNo = ""
private var location = ""
private var role = ""

var  playNewSessionClicked = false
var  stopButtonClicked = false
var  pauseButtonClicked = false
// global variables
var present_coconut_count = 0
var present_session_coconut_count = 0
var frame_count = 0
var totalCountUpdated = false

var autoCapture = true

private var confidenceList = mutableListOf<Float>()

var locationsL = mutableListOf<Float>()
var locationsT = mutableListOf<Float>()
var locationsR = mutableListOf<Float>()
var locationsB = mutableListOf<Float>()

var bunchCenterX = 0.0.toFloat()
var bunchCenterY = 0.0.toFloat()
var newBunchCenterX = 0.0.toFloat()
var newBunchCenterY = 0.0.toFloat()
var arrowDistance = 0.0.toFloat()

var minArrowDistance = 8
var maxArrowDistance = 90
var showBestPic = false
var bestPicSelected = false
var bestPic: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
var best_count = 0

var sessionStatus = ""
var sessionType = ""
var sessionNo = 0
var sessionUser = ""

var captureButtonPressed = false

class VideoActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var labels:List<String>
    val paint = Paint()
    private lateinit var imageProcessor: ImageProcessor
    lateinit var bitmap:Bitmap
    // late init var mutable:Bitmap
    lateinit var imageView: ImageView
    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    private lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    private lateinit var model:Coconut400

    // initialising object variables
    private lateinit var captureButton: Button
    private lateinit var switchButton: SwitchCompat

    private lateinit var playButton: Button
    private lateinit var stopButton: Button
    private lateinit var pauseButton: Button

    private var detections = mutableListOf<Int>()
    private var no = 0

    @SuppressLint("SimpleDateFormat")
    private fun getTime(): String {
        // Get an instance of the `SimpleDateFormat` class.
        val df = SimpleDateFormat("HH:mm:ss")

        // Get the current time.
        val date = Date()

        // Format the time using the `SimpleDateFormat` object.
        val formattedTime = df.format(date)

        // Print the formatted time.
        println("Time: $formattedTime")

        return formattedTime
    }
    private fun getDate():String{
        // Get an instance of the `DateFormat` class.
        val df = getDateInstance(DateFormat.SHORT)

        // Get the current date.
        val date = Date()

        // Format the date using the `DateFormat` object.
        val formattedDate = df.format(date)

        // Print the formatted date.
        println("Date: $formattedDate")
        return formattedDate
    }

    // main functions
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        // getting shared preference
        val sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val editor = sharedPref?.edit()

        // shared preference values
        no = sharedPref?.getInt("imageNo", 0)!!
        sessionNo = sharedPref.getInt("sessionNo", 0)
        sessionUser = sharedPref.getString("username","").toString()
        autoCapture = sharedPref.getBoolean("autoMode",true)


        detections = readPauseDetection()
        println("************************* detections ***************************")
        println(detections)
        println(" ")

        when (sharedPref.getInt("session",5)) {
            1 -> {
                sessionStatus = "loadingStarted"
                sessionType = "loading"
                println(" ")
                println("user chosen loading starting new session")
                println("----------------------------------------")
            }
            2 -> {
                sessionStatus = "unloadingStarted"
                sessionType = "unloading"
                println(" ")
                println("user chosen unloading starting new session")
                println("----------------------------------------")
            }
            3 -> {
                sessionStatus = "loadingPaused"
                sessionType = "loading"
                println(" ")
                println("loading was paused in past session")
                println("----------------------------------------")
            }
            4 -> {
                sessionStatus = "unloadingPaused"
                sessionType = "unloading"
                println(" ")
                println("unloading was paused in past session")
                println("----------------------------------------")
            }
            5 ->{
                sessionStatus = "stopped"
                println(" ")
                println("error: user didn't chose anything ")
                println("----------------------------------------")
            }
        }

        //  buttons initialisation after layout
        captureButton = findViewById(R.id.captureButton)
        captureButton.setOnClickListener(this)
        captureButton.visibility = View.GONE
        captureButton.isEnabled = false

        switchButton = findViewById(R.id.switch1)
        switchButton.isChecked = autoCapture

        playButton = findViewById(R.id.playButton)
        playButton.setOnClickListener(this)

        stopButton = findViewById(R.id.stopButton)
        stopButton.setOnClickListener(this)

        pauseButton = findViewById(R.id.pauseButton)
        pauseButton.setOnClickListener(this)

        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)


        labels = FileUtil.loadLabels(this, "coconut_labels.txt")
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = Coconut400.newInstance(this)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // getting distance values from shared preferences
        minArrowDistance = sharedPref.getInt("minArrowDist", 10)
        maxArrowDistance = sharedPref.getInt("maxArrowDist", 100)


        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                editor?.apply {
                    putInt("imageNo",no)
                    putInt("sessionNo", sessionNo)
                    putBoolean("autoMode", autoCapture)
                    apply() //asynchronously
                }
                stopButtonClicked = false
                model.close()
                finish()
                // saving paused detections
                storePausedDetectionsToFolder()
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                // get picture from camera
                bitmap = textureView.bitmap!!
                var mutable2 = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                // update mode based on switch
                if(switchButton.isChecked!= autoCapture){
                    editor?.apply {
                        putBoolean("autoMode", switchButton.isChecked)
                        apply()
                    }
                    autoCapture = switchButton.isChecked
                    println("auto mode updated to $autoCapture")
                }

                // if Best picture selected
                if (showBestPic)
                {
                    // best results are stored
                    if (bestPicSelected) {
                        val canvas5 = Canvas(bestPic)
                        paint.color = Color.RED
                        paint.style = Paint.Style.FILL
                        paint.textSize = 80F
                        canvas5.drawText(
                            "coconut stored",
                            (150).toFloat(), (550).toFloat(), paint
                        )

                        // show best pic
                        imageView.setImageBitmap(bestPic)

                        // save best pic in gallery
                        val fileName1 = "img$no"
                        val folderName1 = "session$sessionNo"
                        no+=1
                        storeBitmap(bestPic, fileName1, folderName1)
                        println("image: $fileName1 stored in file")

                        showBestPic = false
                        bestPicSelected = false
                        //update count
                        present_session_coconut_count+= best_count

                        // store in detection list
                        detections.add(best_count)
                    }
                    else
                    {
                        showBestPic = false
                    }
                }

                //<---------- manual mode ---------------------->
                if(!autoCapture)
                {
                    // capture button on
                    captureButton.visibility = View.VISIBLE
                    captureButton.isEnabled = true

                    // turn off remaining buttons
                    playButton.visibility = View.GONE
                    playButton.isEnabled = false
                    pauseButton.visibility = View.GONE
                    pauseButton.isEnabled = false
                    // show stop button
                    stopButton.visibility = View.VISIBLE
                    stopButton.isEnabled = true


                    // manual session
                    // set on touch listener example
                    // https://stackoverflow.com/questions/27520044/click-press-and-release-event-on-button
                    captureButton.setOnTouchListener(OnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                println("capture Button pressed ")
                                captureButtonPressed = true
                                return@OnTouchListener true
                            }

                            MotionEvent.ACTION_UP -> {
                                captureButtonPressed = false
                                println(" capture Button released ")
                                showBestPic = true
                                return@OnTouchListener true
                            }
                        }
                        false
                    })

                }
                //<---------- auto capture mode ---------------------->
                else
                {
                    // turn off capture button
                    captureButton.visibility = View.GONE
                    captureButton.isEnabled = false

                    // remaining buttons visible
                    playButton.visibility = View.VISIBLE
                    playButton.isEnabled = true
                    pauseButton.visibility = View.VISIBLE
                    pauseButton.isEnabled = true
                    stopButton.visibility = View.VISIBLE
                    stopButton.isEnabled = true

                    // automatic capturing session
                    if (playNewSessionClicked) {
                        // <------------------  auto capture mode -------------------->
                        totalCountUpdated = false

                        // stop button visible
                        stopButton.isEnabled = true
                        stopButton.visibility = View.VISIBLE

                        // resume button disabled
                        playButton.isEnabled = false
                        playButton.visibility = View.GONE

                        // pause button enabled
                        pauseButton.isEnabled = true
                        pauseButton.visibility = View.VISIBLE

                        // In auto detection mode coconut detections are enabled

                        if (mutable2.isMutable) {
                            mutable2 = detection(mutable2)
                        }

                        // display frame count
                        frame_count += 1
                        val canvas3 = Canvas(mutable2)
                        paint.color = Color.BLACK
                        paint.style = Paint.Style.FILL
                        paint.textSize = 60F
//                    canvas3.drawText(
//                        "frame count :$frame_count",
//                        (50).toFloat(), (550).toFloat(), paint
//                    )
                        canvas3.drawText(
                            "place coconut inside box",
                            (50).toFloat(), (1650).toFloat(), paint
                        )

                    }
                    else {
                        frame_count = 0
                        // only play button  and stop button was visible
                        playButton.isEnabled = true
                        playButton.visibility = View.VISIBLE

                        stopButton.isEnabled = true
                        stopButton.visibility = View.VISIBLE

                        pauseButton.isEnabled = false
                        pauseButton.visibility = View.GONE


                    }
                }

                if (captureButtonPressed)
                {
                    mutable2 = manualDetection(mutable2)
                }

                // display image
                imageView.setImageBitmap(mutable2)
            }
        }


    }

    override fun onDestroy() {
        // save imageNo when sudden close
        val sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val editor = sharedPref?.edit()
        editor?.apply {
            putInt("imageNo",no)
            apply() //asynchronously
        }

        // updating session status when closed suddenly
        when (sharedPref.getInt("session",5)) {
            // updating loading session
            1 -> {
                println("closed suddenly updating status to loading paused")
                editor?.apply {
                    putInt("session",3)
                    apply()
                }

                // save paused data
                storePausedDetectionsToFolder()

            }
            // updating unloading session
            2 -> {
                println("closed suddenly updating status to unloading paused")
                editor?.apply {
                    putInt("session",4)
                    apply()
                }
                // save paused data
                storePausedDetectionsToFolder()
            }
        }

        playNewSessionClicked = false
        stopButtonClicked = false
        pauseButtonClicked = true

        super.onDestroy()
        model.close()
        finish()
    }

    @SuppressLint("CommitPrefEdits")
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.playButton -> {
                try {
                    //Toast.makeText(applicationContext,"play button pressed",Toast.LENGTH_SHORT).show()
                    playNewSessionClicked = true
                    stopButtonClicked = false
                    pauseButtonClicked = false

                } catch (e: ActivityNotFoundException) {
                    Log.e(MainActivity.TAG, e.message.toString())
                }
            }
            // user pressed stop button
            R.id.stopButton ->{
                try {
                    //Toast.makeText(applicationContext,"stop button pressed",Toast.LENGTH_SHORT).show()
                    stopButtonClicked = true
                    playNewSessionClicked = false
                    pauseButtonClicked = false

                    if (showBestPic)
                    {
                        showBestPic = false
                        bestPicSelected = false
                        playNewSessionClicked = true
                    }

                    // stop button pressed
                    // <------------------ alert box ----------------------------------->
                    // https://www.digitalocean.com/community/tutorials/android-alert-dialog-using-kotlin
                    // alert color problem https://stackoverflow.com/questions/39481735/alertdialog-do-not-show-positive-and-negative-button
                    val builder = AlertDialog.Builder(this)
                    //set title for alert dialog
                    builder.setTitle("session close alert")
                    //set message for alert dialog
                    builder.setMessage("close present session?")
                    builder.setIcon(android.R.drawable.stat_sys_warning)

                    builder.setPositiveButton("yes") { _, _ ->
                        // making new session
                        val sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE)
                        val editor = sharedPref?.edit()
                        editor?.apply {
                            putInt("session",5)
                            apply()
                        }

                        // saving detection in a folder after session closed
                        if (detections.size > 0)
                        {
                            sessionNo+=1
                            editor?.apply {
                                putInt("sessionNo",sessionNo)
                                apply() //asynchronously
                            }
                            // storing session data to folder
                            storeSessionDataToFolder()

                            // update profile
                            updateProfileJson()

                            // saving file to database
                            storeSessionDataToFirebase()

                            // delete paused session
                            deletePausedDetection()
                            detections = readPauseDetection()

                            Toast.makeText(applicationContext, "session Data saved", Toast.LENGTH_SHORT).show()
                        }
                        else
                        {
                            Toast.makeText(applicationContext, "No detections ", Toast.LENGTH_SHORT).show()
                        }

                        Toast.makeText(applicationContext,
                            "session closed", Toast.LENGTH_SHORT).show()
                        // going back
                        model.close()
                        cameraDevice.close()
                        finish()
                        // going back to home fragment
                        dispatchTakeHomeIntent()
                    }

                    builder.setNegativeButton("no") { _, _ ->
                        Toast.makeText(applicationContext, "continuing session", Toast.LENGTH_SHORT).show()
                    }

//                    builder.setNeutralButton("cancel") { dialog, which ->
//                        Toast.makeText(applicationContext,
//                            "cancelling alert", Toast.LENGTH_SHORT).show()
//                    }
//                    builder.show()

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
                catch (e: ActivityNotFoundException) {
                    Log.e(MainActivity.TAG, e.message.toString())
                }
            }

            R.id.pauseButton ->{
                playNewSessionClicked = false
                stopButtonClicked = false
                pauseButtonClicked = true
            }

            R.id.captureButton ->{
                if (showBestPic)
                {
                    showBestPic = false
                    bestPicSelected = false
                }

            }
        }


    }

    // when user pressed back button
    @Deprecated("Deprecated in Java",
        ReplaceWith("super.onBackPressed()", "androidx.appcompat.app.AppCompatActivity")
    )
    override fun onBackPressed() {
        super.onBackPressed()
        // updating session status when closed suddenly
        // getting shared preference
        val sharedPref = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        val editor = sharedPref?.edit()
        when (sharedPref.getInt("session",5)) {
            // updating loading session
            1 -> {
                println("closed suddenly updating status to loading paused")
                editor?.apply {
                    putInt("session",3)
                    apply()
                }
            }
            // updating unloading session
            2 -> {
                println("closed suddenly updating status to unloading paused")
                editor?.apply {
                    putInt("session",4)
                    apply()
                }
            }
        }
        model.close()
        cameraDevice.close()
        finish()

        playNewSessionClicked = false
        stopButtonClicked = false
        pauseButtonClicked = true

        // save paused data
        storePausedDetectionsToFolder()

        dispatchTakeHomeIntent()

    }

    @SuppressLint("MissingPermission")
    private fun openCamera(){
        cameraManager.openCamera(cameraManager.cameraIdList[0], object:CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                val surfaceTexture = textureView.surfaceTexture
                val surface = Surface(surfaceTexture)

                val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                @Suppress("DEPRECATION")
                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(), null, null)
                    }
                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {
                cameraDevice.close()
                println("error: camera disconnected")
            }

            override fun onError(p0: CameraDevice, p1: Int) {
                cameraDevice.close()
                println("error: camera error")
            }
        }, handler)
    }

    private fun detection(mutable:Bitmap): Bitmap {
        try {
            // auto mode detection rectangle
            fun drawDetectionRectangles(mutable1: Bitmap, locationsList:FloatArray, scoresList:FloatArray): Bitmap {
                val h = mutable1.height
                val w = mutable1.width

                val xFactor = 250F
                val yFactor = 400F
                val xMax = (w-yFactor)
                val yMax = (h-yFactor)

                // <----------------------- drawing  detections --------------------------------------->
                val canvas = Canvas(mutable1)
                paint.textSize = h / 20f
                paint.strokeWidth = h / 200f
                var x: Int
                if (scoresList.isNotEmpty()) {
                    scoresList.forEachIndexed { index, fl ->
                        x = index
                        x *= 4
                        // display rectangles
                        if (fl > 0.5) {

                            // add to global list
                            confidenceList.add(fl)
                            locationsL.add((locationsList[x + 1] * w))
                            locationsT.add((locationsList[x] * h))
                            locationsR.add((locationsList[x + 3] * w))
                            locationsB.add((locationsList[x + 2] * h))


                            // draw detected rectangles
                            paint.color = Color.GREEN
                            paint.style = Paint.Style.STROKE
                            canvas.drawRect(
                                RectF(
                                    locationsList[x + 1] * w,
                                    locationsList[x] * h,
                                    locationsList[x + 3] * w,
                                    locationsList[x + 2] * h
                                ), paint
                            )

                            // display name of class and confidence
//                    paint.style = Paint.Style.FILL
//                    paint.color = Color.BLUE
//                    canvas.drawText(
//                        fl.toString(),
//                        //  labels[classes[index].toInt()] + " " + fl.toString(),
//                        locationsList[x + 1] * w,
//                        locationsList[x] * h,
//                        paint
//                    )

                        }

                        // for less confidence image
                        else if((fl>0.3)&&(fl<0.5))
                        {
                            // draw detected rectangles
                            paint.color = Color.RED
                            paint.style = Paint.Style.STROKE
                            canvas.drawRect(
                                RectF(
                                    locationsList[x + 1] * w,
                                    locationsList[x] * h,
                                    locationsList[x + 3] * w,
                                    locationsList[x + 2] * h
                                ), paint
                            )

                            // display name of class and confidence
                            paint.style = Paint.Style.FILL
                            paint.color = Color.BLUE
                            canvas.drawText(
                                fl.toString(),
                                //  labels[classes[index].toInt()] + " " + fl.toString(),
                                locationsList[x + 1] * w,
                                locationsList[x] * h,
                                paint
                            )

                        }

                    }
                }

                //<-----------------------------display  detection variables ------------------------->
                present_coconut_count = locationsL.size
                // display no of coconuts
                paint.color = Color.BLUE
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    "present count: $present_coconut_count",
                    (50).toFloat(), (100).toFloat(), paint
                )

                // display total no of coconuts
                paint.color = Color.RED
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    "session coconut count: $present_session_coconut_count",
                    (50).toFloat(), (250).toFloat(), paint
                )

                // <--------------------- calculate bunch Center ------------------------------------>
                for (index in 0 until locationsL.size) {
                    bunchCenterX += locationsL[index]
                    bunchCenterY += locationsT[index]
                }
                bunchCenterX /= (locationsL.size)
                bunchCenterY /= (locationsT.size)

                // draw bunch center
                paint.color = Color.BLACK
                paint.style = Paint.Style.FILL
                canvas.drawCircle(bunchCenterX, bunchCenterY, 10f, paint )

                // draw distance above arrow
                paint.color = Color.GRAY
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    "${arrowDistance.toInt()}",
                    bunchCenterX-2, bunchCenterY, paint
                )
                // draw point numbers
                paint.color = Color.WHITE
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    "$bunchCenterX, $bunchCenterY",
                    (w/5).toFloat(), (h-10).toFloat(), paint
                )

                // draw moment arrow
                if (newBunchCenterX!=0.0.toFloat())
                {
                    paint.color = Color.YELLOW
                    paint.style = Paint.Style.FILL
                    canvas.drawLine(newBunchCenterX, newBunchCenterY, bunchCenterX, bunchCenterY,paint)

                    // find arrow distance
                    arrowDistance = sqrt(((newBunchCenterX- bunchCenterX)*(newBunchCenterX- bunchCenterX))+((newBunchCenterY- bunchCenterY)*(newBunchCenterY- bunchCenterY)))
                    println("arrow distance $arrowDistance")
                }

                // when to choose best image
                if ((arrowDistance>0)&&(arrowDistance< minArrowDistance))
                {
                    best_count = present_coconut_count
                    bestPicSelected = true
                    bestPic = mutable1.copy(Bitmap.Config.ARGB_8888, true)
                }

                // ******************  when to save image******************

                // paint rectangle to show contain box for coconut placement
                paint.color = Color.BLACK
                paint.style = Paint.Style.STROKE
                canvas.drawRect(
                    RectF(
                        xFactor,
                        xFactor,
                        xMax,
                        yMax
                    ), paint
                )


                // when to save image

                if (bunchCenterX.isNaN() or bunchCenterY.isNaN())
                {
                    showBestPic = true
                }
                else if(((bunchCenterX < xFactor) or (bunchCenterY < xFactor))or((bunchCenterX>xMax)or(bunchCenterY>yMax)))
                {
                    // draw bunch center
                    paint.color = Color.BLACK
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(bunchCenterX, bunchCenterY, 100f, paint )

                    showBestPic = true
                }

//        if (arrowDistance> maxArrowDistance)
//        {
//            showBestPic = true
//            //println("best picture")
//        }

                // print best pick selected or not
                if(bestPicSelected)
                {
                    // print best picture selected
                    paint.color = Color.YELLOW
                    paint.style = Paint.Style.FILL
                    paint.textSize = 75F
                    canvas.drawText(
                        "remove coconut from box",
                        (50).toFloat(), (h*0.7).toFloat(), paint
                    )
                }

                // println("locationsL $locationsL")
                // println("locationsT $locationsT")
                // println("bunch x $bunchCenterX , bunch y $bunchCenterY")

                // clear values
                locationsL.clear()
                locationsB.clear()
                locationsT.clear()
                locationsR.clear()
                newBunchCenterX = bunchCenterX
                newBunchCenterY = bunchCenterY
                bunchCenterX = 0.0.toFloat()
                bunchCenterY = 0.0.toFloat()

                return mutable1
            }

            // tensorflow  detection
            var image = TensorImage.fromBitmap(mutable)
            image = imageProcessor.process(image)
            val outputs = model.process(image)
            val locations = outputs.locationAsTensorBuffer.floatArray
            // val classes = outputs.categoryAsTensorBuffer.floatArray
            val scores = outputs.scoreAsTensorBuffer.floatArray
            // val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray
            return drawDetectionRectangles(mutable,locations,scores)
        }
        catch (e: JSONException) {
            println("error in video activity/ auto detection: problem in detection method")
            println(e)
            e.printStackTrace()
            return mutable
        }


    }

    private fun manualDetection(mutable5:Bitmap): Bitmap {
        try {
            // auto mode detection rectangle
            fun drawManualDetectionRectangles(mutable6: Bitmap, locationsList:FloatArray, scoresList:FloatArray): Bitmap {
                val h = mutable6.height
                val w = mutable6.width

                // <----------------------- drawing  detections --------------------------------------->
                val canvas = Canvas(mutable6)
                paint.textSize = h / 20f
                paint.strokeWidth = h / 200f
                var x: Int
                if (scoresList.isNotEmpty()) {
                    scoresList.forEachIndexed { index, fl ->
                        x = index
                        x *= 4
                        // display rectangles
                        if (fl > 0.5) {

                            // add to global list
                            confidenceList.add(fl)
                            locationsL.add((locationsList[x + 1] * w))
                            locationsT.add((locationsList[x] * h))
                            locationsR.add((locationsList[x + 3] * w))
                            locationsB.add((locationsList[x + 2] * h))


                            // draw detected rectangles
                            paint.color = Color.GREEN
                            paint.style = Paint.Style.STROKE
                            canvas.drawRect(
                                RectF(
                                    locationsList[x + 1] * w,
                                    locationsList[x] * h,
                                    locationsList[x + 3] * w,
                                    locationsList[x + 2] * h
                                ), paint
                            )

                            // display name of class and confidence
//                    paint.style = Paint.Style.FILL
//                    paint.color = Color.BLUE
//                    canvas.drawText(
//                        fl.toString(),
//                        //  labels[classes[index].toInt()] + " " + fl.toString(),
//                        locationsList[x + 1] * w,
//                        locationsList[x] * h,
//                        paint
//                    )

                        }

                        // for less confidence image
                        else if((fl>0.3)&&(fl<0.5))
                        {
                            // draw detected rectangles
                            paint.color = Color.RED
                            paint.style = Paint.Style.STROKE
                            canvas.drawRect(
                                RectF(
                                    locationsList[x + 1] * w,
                                    locationsList[x] * h,
                                    locationsList[x + 3] * w,
                                    locationsList[x + 2] * h
                                ), paint
                            )

                            // display name of class and confidence
                            paint.style = Paint.Style.FILL
                            paint.color = Color.BLUE
                            canvas.drawText(
                                fl.toString(),
                                //  labels[classes[index].toInt()] + " " + fl.toString(),
                                locationsList[x + 1] * w,
                                locationsList[x] * h,
                                paint
                            )

                        }

                    }
                }

                //<-----------------------------display  detection variables ------------------------->
                present_coconut_count = locationsL.size
                // display no of coconuts
                paint.color = Color.BLUE
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    "present count: $present_coconut_count",
                    (50).toFloat(), (100).toFloat(), paint
                )

                // display total no of coconuts
                paint.color = Color.RED
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    "session coconut count: $present_session_coconut_count",
                    (50).toFloat(), (250).toFloat(), paint
                )

                // getting best picture
                best_count = present_coconut_count
                bestPicSelected = true
                bestPic = mutable6.copy(Bitmap.Config.ARGB_8888, true)

                // print best picture selected text
                paint.color = Color.YELLOW
                paint.style = Paint.Style.FILL
                paint.textSize = 75F
                canvas.drawText(
                    "best pic selected",
                    (50).toFloat(), (h*0.7).toFloat(), paint
                )

                // clear values
                locationsL.clear()
                locationsB.clear()
                locationsT.clear()
                locationsR.clear()

                return mutable6
            }

            // tensorflow  detection
            var image = TensorImage.fromBitmap(mutable5)
            image = imageProcessor.process(image)
            val outputs = model.process(image)
            val locations = outputs.locationAsTensorBuffer.floatArray
            // val classes = outputs.categoryAsTensorBuffer.floatArray
            val scores = outputs.scoreAsTensorBuffer.floatArray
            // val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray
            // println("manual detection was  completed sending best image")
            return drawManualDetectionRectangles(mutable5,locations,scores)
        }
        catch (e: JSONException) {
            println("error in video activity/ auto detection: problem in detection method")
            println(e)
            e.printStackTrace()
            return mutable5
        }
    }

    private fun storeBitmap(bitmap: Bitmap, filename:String, folderName:String){
        //https://github.com/himanshuGaur684/Scope_Storage_PG/blob/scope_storage_images/app/src/main/java/com/gaur/flowoperator/MainActivity.kt
        val contentResolver = this.contentResolver
        val imageCollection = if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }else{
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"${filename}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
                put(MediaStore.Images.Media.IS_PENDING,1)
                // save your file to a specific location ( use this below comment line )
                put(MediaStore.Images.Media.RELATIVE_PATH,"DCIM/one_root_images/$folderName")
            }
        }

        val imageUri = contentResolver.insert(imageCollection,contentValues)
        imageUri?.let {
            val outputStream = contentResolver.openOutputStream(it)
            bitmap.compress(Bitmap.CompressFormat.JPEG,90,outputStream)
            outputStream?.close()
            contentValues.clear()
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
                contentValues.put(MediaStore.Images.Media.IS_PENDING,0)
            }
            contentResolver.update(it,contentValues,null,null)
            outputStream?.close()
        }

    }

    // saving detection to folder
    private fun storeSessionDataToFolder()
    {
        val filepath = "OneRootFiles"
        val fileName  = "session.json"
        println("storing detection json data on video activity")

        fun isExternalStorageWritable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

        fun isExternalStorageReadable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

        fun saveToExternalStorage(jsonData:String) {
            val filePath = this.getExternalFilesDir(filepath)
            val myExternalFile = File(filePath, fileName)
            try {
                println("file saved in $myExternalFile")
                val fileOutputStream = FileOutputStream(myExternalFile)
                fileOutputStream.write(jsonData.toByteArray())
                fileOutputStream.close()

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun readFromExternalStorage(): JsonArray? {
            val myExternalFile = File(this.getExternalFilesDir(filepath), fileName)
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
                    val jsonArrayData = Gson().fromJson(stringBuilder.toString(),JsonArray::class.java)
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



        // profile data created and saved in folder
        val sessionJsonData = JsonObject()
        val sessionJsonArray = readFromExternalStorage()
        try {
            sessionJsonData.addProperty("sessionNo", sessionNo)
            sessionJsonData.addProperty("sessionType", sessionType)
            sessionJsonData.addProperty("detectionData",detections.toString())
            sessionJsonData.addProperty("time",getTime())
            sessionJsonData.addProperty("date",getDate())
            sessionJsonData.addProperty("sessionUser", sessionUser)

            sessionJsonArray?.add(sessionJsonData)
            //printing updated json array data
            println("json array data after updating new session data: $sessionJsonArray")
            // converting json object to json string
            val jsonString = Gson().toJson(sessionJsonArray)

            if (isExternalStorageWritable()) {
                saveToExternalStorage(jsonString)
                Toast.makeText(this, "detection json data stored on video activity", Toast.LENGTH_SHORT).show()
            }
            else
            {
                Toast.makeText(this, "External storage not available for writing detection json on video activity", Toast.LENGTH_SHORT).show()
            }
        }
        catch (e: JSONException) {
            println("error in video activity: json file cant be saved")
            e.printStackTrace()
        }
        finally {
            println("session json data stored to folder successfully")
        }
    }

    // store detection data when suddenly closed
    private fun storePausedDetectionsToFolder()
    {
        val filepath = "OneRootFiles"
        val fileName  = "PausedDetection.json"
        println("storing paused detection json data on video activity")

        fun isExternalStorageWritable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

        fun saveToExternalStorage(jsonData:String) {
            val filePath = this.getExternalFilesDir(filepath)
            val myExternalFile = File(filePath, fileName)
            try {
                val fileOutputStream = FileOutputStream(myExternalFile)
                fileOutputStream.write(jsonData.toByteArray())
                fileOutputStream.close()
                println("file saved in $myExternalFile")
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }

//        fun getTime(): String {
//            return getTimeInstance().toString()
//        }
//        fun getDate():String{
//            return getDateInstance().toString()
//        }

        // profile data created and saved in folder
        val sessionJsonData = JsonObject()
        try {
            sessionJsonData.addProperty("sessionNo", sessionNo)
            sessionJsonData.addProperty("sessionType", sessionType)
            sessionJsonData.addProperty("PauseDetectionData",detections.toString())
            sessionJsonData.addProperty("time",getTime())
            sessionJsonData.addProperty("date",getDate())

            //printing updated json array data
            println("paused detection data: $sessionJsonData")
            println("---------------------")
            // converting json object to json string
            val jsonString = Gson().toJson(sessionJsonData)

            if (isExternalStorageWritable()) {
                saveToExternalStorage(jsonString)
                Toast.makeText(this, "paused detection json data stored on video activity", Toast.LENGTH_SHORT).show()
            }
            else
            {
                Toast.makeText(this, "External storage not available for writing detection json on video activity", Toast.LENGTH_SHORT).show()
            }
        }
        catch (e: JSONException)
        {
            println("error in video activity: paused detection json file cant be saved")
            e.printStackTrace()
        }
        finally
        {
            println("paused detection json data stored in storage")
        }
    }

    private fun deletePausedDetection()
    {
        val filepath = "OneRootFiles"
        val fileName  = "PausedDetection.json"
        println("storing paused detection json data on video activity")

        fun isExternalStorageWritable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

        fun saveToExternalStorage(jsonData:String) {
            val filePath = this.getExternalFilesDir(filepath)
            val myExternalFile = File(filePath, fileName)
            try {
                val fileOutputStream = FileOutputStream(myExternalFile)
                fileOutputStream.write(jsonData.toByteArray())
                fileOutputStream.close()
                println("file deleted path: $myExternalFile")
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // profile data created and saved in folder
        val sessionJsonData = JsonObject()
        try {
            sessionJsonData.addProperty("sessionNo", "")
            sessionJsonData.addProperty("sessionType", "")
            sessionJsonData.addProperty("PauseDetectionData","")
            sessionJsonData.addProperty("time","")
            sessionJsonData.addProperty("date","")

            //printing updated json array data
            println("deleted paused detection data: $sessionJsonData")
            println("---------------------")
            // converting json object to json string
            val jsonString = Gson().toJson(sessionJsonData)
            if (isExternalStorageWritable()) {
                saveToExternalStorage(jsonString)
                //Toast.makeText(this, "deleted paused detection json ", Toast.LENGTH_SHORT).show()
            }
            else
            {
                Toast.makeText(this, "External storage not available for deleting paused detection json on video activity", Toast.LENGTH_SHORT).show()
            }
        }
        catch (e: JSONException)
        {
            println("error in video activity: paused detection json file cant be deleted")
            e.printStackTrace()
        }
        finally
        {
            println("deleted paused detection json from storage")
        }
    }

    private fun readPauseDetection(): MutableList<Int> {
        val filepath = "OneRootFiles"
        val fileName = "PausedDetection.json"
        println("retrieving paused detection json data on video activity")
        val myExternalFile = File(this.getExternalFilesDir(filepath), fileName)

        // create empty list
        val  mutableList = mutableListOf<Int>()

        fun convertToList(jsonString: String): MutableList<Int> {
            val jsonString1 = jsonString.replace("[","")
            val jsonString2 = jsonString1.replace("]","")
            val jsonString3 = jsonString2.replace(" ","")
            val charList: List<String> =  jsonString3.split(",")
            println("charList $charList")

            for (eachChar in charList)
            {
                //println(eachChar.toInt())
                if (eachChar!="")
                {
                    mutableList.add(eachChar.toInt())
                }
            }
            return mutableList
        }

        fun isExternalStorageReadable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

        fun readFromExternalStorage(): String {
            try {
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

            } catch (e: JSONException) {
                println("error in video activity: json file cant be read from external storage")
                e.printStackTrace()
            }
            return String()
        }
        if (myExternalFile.exists() and isExternalStorageReadable()) {
            println("reading pause Detection")
            val dataFromJson = readFromExternalStorage()
            println(" json data:  $dataFromJson")
            val jsonObject = JSONObject(dataFromJson)
            val detectionList = jsonObject.getString("PauseDetectionData")
            println("<---------------------------------------->")
            println("paused detectionList :$detectionList")
            println("<---------------------------------------->")
            // Add each element of the string list to the mutable int list
            return  convertToList(detectionList)
        }
        else
        {
            println("Error: no session json file exists returning empty json array")
            return mutableListOf()
        }

    }

    // store session in firebase
    private fun storeSessionDataToFirebase()
    {
        //shared preferences
        val sharedPref = this.getSharedPreferences("myPref", Context.MODE_PRIVATE)
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
                val myExternalFile = File(this.getExternalFilesDir(filepath), fileName)
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
                        val jsonArrayData = Gson().fromJson(stringBuilder.toString(),JsonArray::class.java)
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

            // creating database
            val db = Firebase.firestore
            val sessionData = hashMapOf(
                "sessionData" to sessionDataFromFolder
            )
            println("sessionData $sessionData")

            // Add a new document with a generated ID
            val documentName1 = sessionUser+"SessionData"
            db.collection("session").document(documentName1).set(sessionData)
                .addOnSuccessListener {
                    Toast.makeText(this, "firebase session updated", Toast.LENGTH_SHORT).show()
                    Log.d(MainActivity.TAG, "DocumentSnapshot added with ID: data")

                    // update session status
                    editor?.apply {
                        putBoolean("sessionDb",true)
                        apply() //asynchronously
                    }

                }
                .addOnFailureListener { e ->
                    Log.w(MainActivity.TAG, "Error adding document to firebase", e)

                    // update session status
                    editor?.apply {
                        putBoolean("sessionDb",false)
                        apply() //asynchronously
                    }
                }
        }
        catch (e: Exception)
        {
            println("error in firebase database: firebase storing session data failed")
            e.printStackTrace()

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

    private fun dispatchTakeHomeIntent() {
        val intent = Intent(this, MainActivity::class.java)
        println("home activity closed")
        startActivity(intent)
        this.finish()
    }

    private fun saveImagesToCloud()
    {
        //TODO saving images to google cloud

        // how to upload to cloud
        // https://firebase.google.com/docs/storage/android/upload-files
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
                mobileNo = jsonObject.getString("mobileNo")
                location = jsonObject.getString("location")
                role = jsonObject.getString("role")
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

    private fun updateProfileJson()
    {

        val filepath = "OneRootFiles"
        val fileName  = "profile.json"
        println("storing json data on registration")

        fun isExternalStorageWritable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

        fun saveToExternalStorage(jsonData:String) {
            val filePath = this.getExternalFilesDir(filepath)
            val myExternalFile = File(filePath, fileName)
            try {
                val fileOutputStream = FileOutputStream(myExternalFile)
                fileOutputStream.write(jsonData.toByteArray())
                fileOutputStream.close()
                println("file saved in $myExternalFile")

            } catch (e: IOException) {
                e.printStackTrace()
                println("exception in save to external storage")
            }
        }

        // profile data created and saved in folder
        readProfileFromStorage()
        val profileJson = JsonObject()
        try {
            profileJson.addProperty("username", userName)
            profileJson.addProperty("mobileNo", mobileNo)
            profileJson.addProperty("numberOfCoconuts", present_session_coconut_count )
            profileJson.addProperty("numberOfSessions", sessionNo)
            profileJson.addProperty("location", location)
            profileJson.addProperty("role",role)

            // converting json object to json string
            val gson = Gson()
            val jsonString = gson.toJson(profileJson)
            if (isExternalStorageWritable()) {
                saveToExternalStorage(jsonString)
                Toast.makeText(this, "profile json data stored on registration", Toast.LENGTH_SHORT).show()
                println("<--- profile.json file stored successfully ")
            }
            else
            {
                Toast.makeText(this, "External storage not available for writing profile json on registration", Toast.LENGTH_SHORT).show()
                println("External storage not available for storing profile.json file")
            }
        }
        catch (e: JSONException) {
            println("error in register fragment: json file cant be saved")
            e.printStackTrace()
        }

    }

}






