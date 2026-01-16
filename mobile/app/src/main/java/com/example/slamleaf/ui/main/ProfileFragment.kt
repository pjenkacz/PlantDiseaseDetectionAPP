package com.example.slamleaf.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.slamleaf.data.local.PhotoRepository
import com.example.slamleaf.data.local.TokenManager
import com.example.slamleaf.data.remote.RetrofitClient
import com.example.slamleaf.data.remote.UserEmailRequest
import com.example.slamleaf.data.remote.UserNameRequest
import com.example.slamleaf.data.remote.UserPasswordRequest
import com.example.slamleaf.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tokenManager = TokenManager(requireContext())

        val textName = view.findViewById<TextView>(R.id.textUserName)
        val textEmail = view.findViewById<TextView>(R.id.textUserEmail)

        // ===== EMAIL =====
        val btnChangeEmail = view.findViewById<Button>(R.id.btnChangeEmail)
        val layoutChangeEmail = view.findViewById<View>(R.id.layoutChangeEmail)
        val inputNewEmail = view.findViewById<EditText>(R.id.inputNewEmail)
        val inputPasswordConfirm = view.findViewById<EditText>(R.id.inputPasswordConfirm)
        val btnSubmitChangeEmail = view.findViewById<Button>(R.id.btnSubmitChangeEmail)

        // ===== NAME =====
        val btnChangeName = view.findViewById<Button>(R.id.btnChangeName)
        val layoutChangeName = view.findViewById<View>(R.id.layoutChangeName)
        val inputNewName = view.findViewById<EditText>(R.id.inputNewName)
        val inputPasswordForName = view.findViewById<EditText>(R.id.inputPasswordForName)
        val btnSubmitChangeName = view.findViewById<Button>(R.id.btnSubmitChangeName)

        val rowChangePassword = view.findViewById<View>(R.id.rowChangePassword)
        val layoutChangePassword = view.findViewById<View>(R.id.layoutChangePassword)
        val inputCurrentPassword = view.findViewById<EditText>(R.id.inputCurrentPassword)
        val inputNewPassword = view.findViewById<EditText>(R.id.inputNewPassword)
        val inputRepeatNewPassword = view.findViewById<EditText>(R.id.inputRepeatNewPassword)
        val btnSubmitChangePassword = view.findViewById<Button>(R.id.btnSubmitChangePassword)

        val btnLogout = view.findViewById<Button>(R.id.btnLogout)


        // init
        textName.text = ""
        textEmail.text = ""

        val token = tokenManager.getToken()
        if (!token.isNullOrEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val me = RetrofitClient.apiService.getMe("Bearer $token")
                    textName.text = me.name
                    textEmail.text = me.email
                } catch (_: Exception) {
                    textName.text = ""
                    textEmail.text = ""
                }
            }
        }

        // ===== TOGGLE: formularz zmiany maila =====
        btnChangeEmail.setOnClickListener {
            layoutChangeName.visibility = View.GONE
            layoutChangePassword.visibility = View.GONE
            layoutChangeEmail.visibility =
                if (layoutChangeEmail.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // ===== SUBMIT: zmiana maila =====
        btnSubmitChangeEmail.setOnClickListener {
            val newEmail = inputNewEmail.text.toString().trim()
            val password = inputPasswordConfirm.text.toString()

            if (newEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Uzupełnij email i hasło", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val t = tokenManager.getToken()
            if (t.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Brak tokena – zaloguj się ponownie", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.apiService.changeEmail(
                        token = "Bearer $t",
                        request = UserEmailRequest(email = newEmail, password = password)
                    )

                    if (resp.isSuccessful) {
                        textEmail.text = newEmail
                        layoutChangeEmail.visibility = View.GONE
                        inputNewEmail.setText("")
                        inputPasswordConfirm.setText("")
                        Toast.makeText(requireContext(), "Email zmieniony", Toast.LENGTH_SHORT).show()
                    } else {
                        val msg = when (resp.code()) {
                            401 -> "Niepoprawne hasło"
                            409 -> "Email jest już zajęty"
                            422 -> "Niepoprawny email"
                            else -> "Błąd zmiany emaila (${resp.code()})"
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "Błąd sieci", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ===== TOGGLE: formularz zmiany imienia =====
        btnChangeName.setOnClickListener {
            layoutChangeEmail.visibility = View.GONE
            layoutChangePassword.visibility = View.GONE
            layoutChangeName.visibility =
                if (layoutChangeName.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // ===== SUBMIT: zmiana imienia =====
        btnSubmitChangeName.setOnClickListener {
            val newName = inputNewName.text.toString().trim()
            val password = inputPasswordForName.text.toString()

            if (newName.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Uzupełnij imię i hasło", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val t = tokenManager.getToken()
            if (t.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Brak tokena – zaloguj się ponownie", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.apiService.changeName(
                        token = "Bearer $t",
                        request = UserNameRequest(name = newName, password = password)
                    )

                    if (resp.isSuccessful) {
                        textName.text = newName
                        layoutChangeName.visibility = View.GONE
                        inputNewName.setText("")
                        inputPasswordForName.setText("")
                        Toast.makeText(requireContext(), "Imię zmienione", Toast.LENGTH_SHORT).show()
                    } else {
                        val msg = when (resp.code()) {
                            401 -> "Niepoprawne hasło"
                            else -> "Błąd zmiany imienia (${resp.code()})"
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "Błąd sieci", Toast.LENGTH_SHORT).show()
                }
            }
        }

        rowChangePassword.setOnClickListener {
            layoutChangeEmail.visibility = View.GONE
            layoutChangeName.visibility = View.GONE
            layoutChangePassword.visibility =
                if (layoutChangePassword.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        btnSubmitChangePassword.setOnClickListener {
            val currentPassword = inputCurrentPassword.text.toString()
            val newPassword = inputNewPassword.text.toString()
            val repeatNewPassword = inputRepeatNewPassword.text.toString()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || repeatNewPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Uzupełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != repeatNewPassword) {
                Toast.makeText(requireContext(), "Nowe hasła nie są takie same", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val t = tokenManager.getToken()
            if (t.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Brak tokena – zaloguj się ponownie", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.apiService.changePassword(
                        token = "Bearer $t",
                        request = UserPasswordRequest(
                            password = currentPassword,
                            newPassword = newPassword
                        )
                    )

                    if (resp.isSuccessful) {
                        layoutChangePassword.visibility = View.GONE
                        inputCurrentPassword.setText("")
                        inputNewPassword.setText("")
                        inputRepeatNewPassword.setText("")
                        Toast.makeText(requireContext(), "Hasło zmienione", Toast.LENGTH_SHORT).show()
                    } else {
                        val msg = when (resp.code()) {
                            401 -> "Niepoprawne obecne hasło"
                            else -> "Błąd zmiany hasła (${resp.code()})"
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "Błąd sieci", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnLogout.setOnClickListener {
            tokenManager.clearAuthData()
            PhotoRepository.clear()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

    }
}