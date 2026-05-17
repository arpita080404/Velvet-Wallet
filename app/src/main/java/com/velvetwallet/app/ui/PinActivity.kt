package com.velvetwallet.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.velvetwallet.app.databinding.ActivityPinBinding

class PinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinBinding
    private val enteredPin = StringBuilder()
    private val PIN_LENGTH = 4
    private var isSettingPin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("velvet_prefs", MODE_PRIVATE)
        val storedPin = prefs.getString("user_pin", null)
        isSettingPin = storedPin == null

        binding.tvPinTitle.text = if (isSettingPin) "Set a PIN" else "Enter PIN"
        binding.tvPinSubtitle.text = if (isSettingPin) "Choose a 4-digit PIN" else "Enter your PIN to continue"

        setupNumpad()
        updateDots()

        if (!isSettingPin && prefs.getBoolean("biometric_enabled", false)) {
            tryBiometric()
        }
    }

    private fun setupNumpad() {
        val buttons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )
        buttons.forEachIndexed { index, btn ->
            btn.setOnClickListener { onDigit(index.toString()) }
        }
        binding.btnDel.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updateDots()
            }
        }
        binding.btnFingerprint.setOnClickListener { tryBiometric() }
    }

    private fun onDigit(digit: String) {
        if (enteredPin.length >= PIN_LENGTH) return
        enteredPin.append(digit)
        updateDots()
        if (enteredPin.length == PIN_LENGTH) {
            verifyOrSetPin(enteredPin.toString())
        }
    }

    private fun verifyOrSetPin(pin: String) {
        val prefs = getSharedPreferences("velvet_prefs", MODE_PRIVATE)
        if (isSettingPin) {
            prefs.edit().putString("user_pin", pin).putBoolean("pin_enabled", true).apply()
            Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show()
            navigateToMain()
        } else {
            val storedPin = prefs.getString("user_pin", "")
            if (pin == storedPin) {
                navigateToMain()
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                enteredPin.clear()
                updateDots()
            }
        }
    }

    private fun updateDots() {
        val filled = enteredPin.length
        listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4).forEachIndexed { i, dot ->
            dot.alpha = if (i < filled) 1f else 0.3f
        }
    }

    private fun tryBiometric() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) !=
            BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometric not available", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                navigateToMain()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(this@PinActivity, errString.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        BiometricPrompt(this, executor, callback).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Velvet Wallet")
                .setSubtitle("Authenticate to continue")
                .setNegativeButtonText("Use PIN")
                .build()
        )
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
