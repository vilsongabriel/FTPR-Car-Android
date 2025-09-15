package com.example.myapitest.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.R
import com.example.myapitest.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var loadingTimeoutHandler: Handler? = null
    private var loadingTimeoutRunnable: Runnable? = null

    companion object {
        private const val LOADING_TIMEOUT_MS = 30000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_login)
    }

    private fun setupClickListeners() {
        binding.sendCodeButton.setOnClickListener {
            val phoneNumber = binding.phoneEditText.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                if (resendToken != null) {
                    resendVerificationCode(phoneNumber)
                } else {
                    sendVerificationCode(phoneNumber)
                }
            } else {
                showToast(getString(R.string.error_invalid_phone))
            }
        }

        binding.verifyCodeButton.setOnClickListener {
            val code = binding.verificationCodeEditText.text.toString().trim()
            if (code.isNotEmpty() && verificationId != null) {
                verifyCode(code)
            } else {
                showToast(getString(R.string.error_invalid_verification_code))
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        showLoading(true)

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    showLoading(false)
                    showToast(getString(R.string.error_verification_failed, e.message))
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    showLoading(false)
                    this@LoginActivity.verificationId = verificationId
                    this@LoginActivity.resendToken = token
                    
                    binding.verificationCodeInputLayout.visibility = View.VISIBLE
                    binding.verifyCodeButton.visibility = View.VISIBLE
                    binding.sendCodeButton.text = getString(R.string.button_resend_code)
                    
                    showToast(getString(R.string.code_sent_success))
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendVerificationCode(phoneNumber: String) {
        showLoading(true)

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setForceResendingToken(resendToken!!)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    showLoading(false)
                    showToast(getString(R.string.error_verification_failed, e.message))
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    showLoading(false)
                    this@LoginActivity.verificationId = verificationId
                    this@LoginActivity.resendToken = token
                    
                    showToast(getString(R.string.code_sent_success))
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode(code: String) {
        verificationId?.let { id ->
            showLoading(true)
            val credential = PhoneAuthProvider.getCredential(id, code)
            signInWithPhoneAuthCredential(credential)
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    showToast(getString(R.string.login_success))
                    navigateToMain()
                } else {
                    val exception = task.exception
                    val errorMessage = when {
                        exception?.message?.contains("invalid verification code", ignoreCase = true) == true ||
                        exception?.message?.contains("invalid code", ignoreCase = true) == true ||
                        exception?.message?.contains("verification code", ignoreCase = true) == true -> {
                            getString(R.string.error_invalid_verification_code_auth)
                        }
                        else -> getString(R.string.login_failed, exception?.message)
                    }
                    showToast(errorMessage)
                }
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.sendCodeButton.isEnabled = !show
        binding.verifyCodeButton.isEnabled = !show
        
        loadingTimeoutHandler?.removeCallbacks(loadingTimeoutRunnable ?: return)
        
        if (show) {
            loadingTimeoutHandler = Handler(Looper.getMainLooper())
            loadingTimeoutRunnable = Runnable {
                showLoading(false)
                showToast("Timeout: Tente novamente")
            }
            loadingTimeoutHandler?.postDelayed(loadingTimeoutRunnable!!, LOADING_TIMEOUT_MS)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingTimeoutHandler?.removeCallbacks(loadingTimeoutRunnable ?: return)
    }
}