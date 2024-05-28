package com.example.broadcastpractice

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MyViewModel>()
    private val broadcastReciver = MyBroadcastReceiver()
    private lateinit var Broadtext: TextView


    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_MY_BROADCAST -> {showBroadcast(ACTION_MY_BROADCAST)}
                else -> { showBroadcast(intent?.action?: "NO_ACTION") }
            }
            //println("onReceive ############ ${intent?.action}")
        }
        private fun showBroadcast(msg: String) {
            println(msg)
            Broadtext.text = msg
        }
    }

    companion object {
        const val ACTION_MY_BROADCAST = "ACTION_MY_BROADCAST"
    }


    //private val textViewCallLog : TextView by lazy {findViewById(R.id.textView)}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Broadtext = findViewById<TextView>(R.id.textViewBroadcast)
        Broadtext.text = "NO_ACTION"
        //Broadtext=findViewById(R.id.textViewBroadcast)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        //lateinit var callLogAdapter: CustomAdapter
        val adapter = CustomAdapter(viewModel)

        recyclerView.adapter = adapter //연결
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        viewModel.itemListData.observe(this) {
            adapter.notifyDataSetChanged()
        }

        //requestSinglePermission(android.Manifest.permission.READ_SMS)
        //requestSinglePermission(android.Manifest.permission.READ_CALL_LOG)
        requestMultiplePermission(arrayOf(android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.READ_CALL_LOG))
    }

    private fun startBroadcastReceiver() {
        IntentFilter().also{
            it.addAction(ACTION_MY_BROADCAST)
            ContextCompat.registerReceiver(this, broadcastReciver, it, ContextCompat.RECEIVER_EXPORTED)

        }
    }

    override fun onStart() {
        super.onStart()
        startBroadcastReceiver()
        readCallLog()
    }

    override fun onStop(){
        super.onStop()
        unregisterReceiver(broadcastReciver)
    }


    private fun readCallLog(){
        if(checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            return

        val adapter = CustomAdapter(viewModel)
        val projection = arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE)
        val cursor = contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, null)
        val str = StringBuilder()

        cursor?.use {
            val idx = it.getColumnIndex(CallLog.Calls.NUMBER)
            var idxType = it.getColumnIndex(CallLog.Calls.TYPE)

            if (idx == -1 || idxType == -1) {
                // Invalid column index, handle the error appropriately
                Log.e("MainActivity", "Invalid column name")
                return@use
            }

            while (it.moveToNext()) {
                val number = it.getString(idx)
                //var columnIndex = cursor.getColumnIndex("CallLog.Calls.TYPE");
                //var callType = "Unknown"


                val type = it.getInt(idxType)
                val callType = when (type) {
                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                    else -> "Unknown"
                }
                /*str.append(number, callType)
                str.append("\n")*/
                println("number : $number, $callType")
                viewModel.addItem(Item(number, callType))
            }
        }
        viewModel.itemListData.value = viewModel.items
        //textViewCallLog.text = str
        adapter.notifyDataSetChanged()
    }

    private fun requestSinglePermission(permission: String) {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
            return

        val requestPermLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it == false) { // permission is not granted!
                AlertDialog.Builder(this).apply {
                    setTitle("Warning")
                    setMessage("Warning")
                }.show()
            }
        }

        if (shouldShowRequestPermissionRationale(permission)) {
            // you should explain the reason why this app needs the permission.
            AlertDialog.Builder(this).apply {
                setTitle("Reason")
                setMessage("Reason")
                setPositiveButton("Allow") { _, _ -> requestPermLauncher.launch(permission) }
                setNegativeButton("Deny") { _, _ -> }
            }.show()
        } else {
            // should be called in onCreate()
            requestPermLauncher.launch(permission)
        }
    }

    private fun requestMultiplePermission(perms: Array<String>) {
        val requestPerms = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (requestPerms.isEmpty())
            return

        val requestPermLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val noPerms = it.filter { item -> item.value == false }.keys.toMutableSet()
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
                if(it.contains(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)||it.contains(android.Manifest.permission.READ_MEDIA_IMAGES)){
                    noPerms.remove(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                    noPerms.remove(android.Manifest.permission.READ_MEDIA_IMAGES)

                }
            }
            if (noPerms.isNotEmpty()) { // there is a permission which is not granted!
                AlertDialog.Builder(this).apply {
                    setTitle("Warning")
                    setMessage("Warning")
                }.show()
            }
        }

        val showRationalePerms = requestPerms.filter { shouldShowRequestPermissionRationale(it) }
        if (showRationalePerms.isNotEmpty()) {
            // you should explain the reason why this app needs the permission.
            AlertDialog.Builder(this).apply {
                setTitle("Reason")
                setMessage("Reason")
                setPositiveButton("Allow") { _, _ -> requestPermLauncher.launch(requestPerms.toTypedArray()) }
                setNegativeButton("Deny") { _, _ -> }
            }.show()
        } else {
            // should be called in onCreate()
            requestPermLauncher.launch(requestPerms.toTypedArray())
        }

    }
}

data class Item (val number: String, val state: String)
class MyViewModel : ViewModel() {
    val itemListData = MutableLiveData<ArrayList<Item>>()
    val items = ArrayList<Item>()

    val item = MutableLiveData<Int>()

    fun addItem(item: Item){
        items.add(item)
        itemListData.value = items
    }
}