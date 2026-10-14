package dev.whistlekeyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Setup and diagnostic activity.
 * Explains how to enable the keyboard, select it, and test the server connection.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val PERMISSION_REQUEST_CODE = 1001

    private lateinit var statusText: TextView
    private lateinit var testConnectionButton: Button
    private lateinit var grantPermissionButton: Button
    private lateinit var openSettingsButton: Button

    private val transcriptionClient = TranscriptionClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        testConnectionButton = findViewById(R.id.testConnectionButton)
        grantPermissionButton = findViewById(R.id.grantPermissionButton)
        openSettingsButton = findViewById(R.id.openSettingsButton)

        setupUI()
        updateStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("request_permission", false)) {
            requestRecordingPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun setupUI() {
        testConnectionButton.setOnClickListener {
            testServerConnection()
        }

        grantPermissionButton.setOnClickListener {
            requestRecordingPermission()
        }

        openSettingsButton.setOnClickListener {
            openKeyboardSettings()
        }
    }

    private fun updateStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED

        val sb = StringBuilder()
        sb.append(getString(R.string.setup_title)).append("\n\n")
        sb.append(getString(R.string.setup_step_1)).append("\n\n")
        sb.append(getString(R.string.setup_step_2)).append("\n\n")
        sb.append(getString(R.string.setup_step_3)).append("\n\n")
        sb.append(getString(R.string.setup_step_4)).append("\n\n")
        sb.append(getString(R.string.setup_server_title)).append("\n")
        sb.append(getString(R.string.setup_server_instruction)).append("\n")
        sb.append(getString(R.string.setup_server_port)).append("\n\n")

        statusText.text = sb.toString()

        // Update permission button state
        if (hasPermission) {
            grantPermissionButton.setText(getString(R.string.setup_permission_granted))
            grantPermissionButton.isEnabled = false
        } else {
            grantPermissionButton.setText(getString(R.string.setup_grant_permission))
            grantPermissionButton.isEnabled = true
        }
    }

    private fun requestRecordingPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.setup_permission_granted, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.setup_permission_denied, Toast.LENGTH_LONG).show()
            }
            updateStatus()
        }
    }

    private fun testServerConnection() {
        testConnectionButton.isEnabled = false
        testConnectionButton.setText("Testing...")

        transcriptionClient.checkHealth(object : TranscriptionClient.HealthCheckCallback {
            override fun onHealthy() {
                runOnUiThread {
                    testConnectionButton.setText(getString(R.string.setup_test_connection))
                    testConnectionButton.isEnabled = true
                    Toast.makeText(this@MainActivity, R.string.setup_connection_ok, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Health check passed")
                }
            }

            override fun onUnhealthy(error: String) {
                runOnUiThread {
                    testConnectionButton.setText(getString(R.string.setup_test_connection))
                    testConnectionButton.isEnabled = true
                    val msg = getString(R.string.setup_connection_failed, error)
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                    Log.w(TAG, "Health check failed: $error")
                }
            }
        })
    }

    private fun openKeyboardSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}