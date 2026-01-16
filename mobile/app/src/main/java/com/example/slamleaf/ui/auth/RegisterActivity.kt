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
import com.example.slamleaf.data.local.TokenManager
import com.example.slamleaf.data.remote.RegisterRequest
import com.example.slamleaf.data.remote.RetrofitClient
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var inputName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var linkLogin: TextView

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        tokenManager = TokenManager(this)

        inputName = findViewById(R.id.inputName)
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnRegister = findViewById(R.id.btnRegister)
        linkLogin = findViewById(R.id.linkLogin)

        btnRegister.setOnClickListener {
            val name = inputName.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.register(
                        RegisterRequest(email = email, password = password, name = name)
                    )

                    tokenManager.saveAuthData(response.token, response.userId)

                    Toast.makeText(
                        this@RegisterActivity,
                        "Rejestracja OK, zalogowano",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@RegisterActivity,
                        "Błąd rejestracji",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        linkLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}