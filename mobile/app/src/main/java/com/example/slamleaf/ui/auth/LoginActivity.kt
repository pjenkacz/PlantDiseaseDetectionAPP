package com.example.slamleaf.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.slamleaf.ui.main.MainActivity
import com.example.slamleaf.ui.auth.RegisterActivity
import com.example.slamleaf.data.local.TokenManager
import com.example.slamleaf.data.remote.LoginRequest
import com.example.slamleaf.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var linkRegister: TextView
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tokenManager = TokenManager(this)

        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.btnLogin)
        linkRegister = findViewById(R.id.linkRegister)

        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Podaj email i hasło", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // wywołanie API w coroutine
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.login(
                        LoginRequest(email, password)
                    )

                    // zapis tokena i userId
                    tokenManager.saveAuthData(response.token, response.userId)

                    Toast.makeText(this@LoginActivity, "Zalogowano!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@LoginActivity, "Błąd logowania", Toast.LENGTH_SHORT).show()
                }
            }
        }

        linkRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}