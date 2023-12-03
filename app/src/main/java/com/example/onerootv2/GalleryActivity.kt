package com.example.onerootv2

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

// gridview images
//https://www.youtube.com/watch?v=ysXd-CupSa0
class GalleryActivity : AppCompatActivity() {
    private var imageFolderPath = "/DCIM/one_root_images%"
    lateinit var rs:Cursor
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val bundle = intent.extras
            if (bundle != null) {
                imageFolderPath = "/DCIM/one_root_images%"
                imageFolderPath = bundle.getString("path").toString()+"%"
                println("GalleryActivity/ getting images from $imageFolderPath")
                bundle.clear()
            }
            else
            {
                println("Error GalleryActivity/ check $imageFolderPath again ")
            }
        }
        catch (e: Exception)
        {
            println("error in GalleryActivity/reading from bundle(): failed to get path from another activity ")
            e.printStackTrace()
            val errorString = "\n error in GalleryActivity/reading from bundle(): failed to get path from another activity  \n${e.message}\n $e"
            saveErrorsInTextToStorage(errorString)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        listImages()
    }
    @SuppressLint("Recycle")
    private fun listImages(){
        val gridview: GridView = findViewById(R.id.gridview)

        val cols = listOf(MediaStore.Images.Thumbnails.DATA).toTypedArray()
//        rs = contentResolver.query(
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            cols,
//            null,
//            null,
//            null)!!

        // getting images from specified folder
        rs = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            cols,
            MediaStore.Images.Media.DATA + " like ?",
            arrayOf("%" + Environment.getExternalStorageDirectory().absolutePath + imageFolderPath),
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")!!

        gridview.adapter =ImageAdaptor(applicationContext)
        gridview.setOnItemClickListener { parent, view, position, id ->
            rs.moveToPosition(position)
            val path = rs.getString(0)
            val i = Intent(applicationContext, DisplayImageActivity::class.java)
            i.putExtra("path",path)
            startActivity(i)
        }
    }
    inner class ImageAdaptor(var context: Context) : BaseAdapter() {
        override fun getCount(): Int {
            return rs.count
        }

        override fun getItem(position: Int): Any {
            return position
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val iv = ImageView(context)
            rs.moveToPosition(position)
            val path = rs.getString(0)
            val bitmap= BitmapFactory.decodeFile(path)
            iv.setImageBitmap(bitmap)
            iv.layoutParams = AbsListView.LayoutParams(300,300)
            return iv
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
}
