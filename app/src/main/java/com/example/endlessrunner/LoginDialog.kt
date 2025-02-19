package com.example.endlessrunner

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class LoginDialog(private val listener: LoginListener) : DialogFragment() {

    interface LoginListener {
        fun onLogin(username: String, password: String)
        fun onSwitchToRegister()  // Called when user wants to register instead
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_login, null)

        val usernameInput = view.findViewById<EditText>(R.id.usernameInput)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val loginButton = view.findViewById<Button>(R.id.loginButton)
        val registerButton = view.findViewById<Button>(R.id.registerButton) // Button to switch to register

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            listener.onLogin(username, password)
            dismiss()
        }

        registerButton.setOnClickListener {
            listener.onSwitchToRegister()  // Trigger switch
            dismiss()
        }

        builder.setView(view)
        return builder.create()
    }
}

