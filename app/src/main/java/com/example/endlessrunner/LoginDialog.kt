package com.example.endlessrunner

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class LoginDialog(private val listener: LoginListener) : DialogFragment() {

    interface LoginListener {
        fun onLogin(username: String, password: String)
        fun onSwitchToRegister()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_login, null)

        val usernameInput = view.findViewById<EditText>(R.id.usernameInput)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val loginButton = view.findViewById<Button>(R.id.loginButton)
        val registerButton = view.findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Basic validation: ensure username and password are not empty
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter both username and password.", Toast.LENGTH_SHORT).show()
            } else {
                // If valid, pass the data to the listener and dismiss
                listener.onLogin(username, password)
                dismiss()
            }
        }

        registerButton.setOnClickListener {
            listener.onSwitchToRegister()
            dismiss()
        }

        builder.setView(view)
        return builder.create()
    }
}
