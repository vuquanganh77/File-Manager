package com.example.filemanager

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var emptyTextView: TextView
    private lateinit var currentDirectory: File
    private lateinit var adapter_old: ArrayAdapter<String>
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }

        listView = findViewById(R.id.listView)
        emptyTextView = findViewById(R.id.emptyTextView)

        registerForContextMenu(listView)
        openOptionsMenu()
        currentDirectory = Environment.getExternalStorageDirectory()

        if (savedInstanceState != null) {
            val savedPath = savedInstanceState.getString("currentDirectory")
            if (!savedPath.isNullOrBlank()) {
                currentDirectory = File(savedPath)
            }
        }

        loadFiles(currentDirectory)
    }

    private fun createNewDirectory() {
        val input = EditText(this)

        val dialog = AlertDialog.Builder(this)
            .setTitle("New Folder")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val folderName = input.text.toString()
                val newDirectory = File(currentDirectory, folderName)

                if (newDirectory.mkdir()) {
                    loadFiles(currentDirectory)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun createNewTextFile() {
        val input = EditText(this)

        val dialog = AlertDialog.Builder(this)
            .setTitle("New Text File")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val fileName = input.text.toString()
                val newFile = File(currentDirectory, "$fileName.txt")

                try {
                    if (newFile.createNewFile()) {
                        loadFiles(currentDirectory)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.newFolderItem -> createNewDirectory()
            R.id.newTextFileItem -> createNewTextFile()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    private fun loadFiles(directory: File) {
        title = directory.path
        val files = directory.listFiles()
        val fileList = mutableListOf<String>()

        if (files != null) {
            for (file in files) {
                fileList.add(file.name)
            }
        }

        if (fileList.isEmpty()) {
            emptyTextView.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            emptyTextView.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedFile = File(directory, fileList[position])
            if (selectedFile.isDirectory) {
                adapter_old = adapter
                loadFiles(selectedFile)
            } else {
                openFile(selectedFile)
            }
        }
    }

    private fun openFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val mimeType = if (file.extension.equals("txt", ignoreCase = true)) {
            "text/plain"
        } else {
            "image/*"
        }

        val openIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(openIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app to handle this type of file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = menuInflater
        inflater.inflate(R.menu.context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val selectedFile = File(currentDirectory, adapter.getItem(info.position)!!)

        when (item.itemId) {
            R.id.renameItem -> showRenameDialog(selectedFile)
            R.id.deleteItem -> showDeleteDialog(selectedFile)
            R.id.copyItem -> showCopyDialog(selectedFile)
        }

        return true
    }

    private fun showCopyDialog(file: File) {
        val input = EditText(this)
        input.setText(file.name)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Copy File/Folder")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString()
                val destinationDirectory = getDestinationDirectory()

                val newFile = File(destinationDirectory, newName)

                if (file.copyTo(newFile, true) != null) {
                    loadFiles(currentDirectory)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showRenameDialog(file: File) {
        val input = EditText(this)
        input.setText(file.name)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Rename File/Folder")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString()
                val newFile = File(file.parentFile, newName)

                if (file.renameTo(newFile)) {
                    loadFiles(currentDirectory)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showDeleteDialog(file: File) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete File/Folder")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Yes") { _, _ ->
                if (file.deleteRecursively()) {
                    loadFiles(currentDirectory)
                }
            }
            .setNegativeButton("No", null)
            .create()

        dialog.show()
    }

    private fun getDestinationDirectory(): File {
        return currentDirectory
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (currentDirectory.path == Environment.getExternalStorageDirectory().path) {
            loadFiles(currentDirectory.parentFile)
        } else {
            loadFiles(currentDirectory.parentFile)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("currentDirectory", currentDirectory.path)
        super.onSaveInstanceState(outState)
    }
}
