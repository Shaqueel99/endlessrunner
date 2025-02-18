package com.example.endlessrunner

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class LoginRegisterDialog(private val listener: LoginRegisterListener) : DialogFragment() {

    interface LoginRegisterListener {
        fun onLogin(username: String, password: String)
        fun onRegister(username: String, password: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_login_register, null)

        val usernameInput = view.findViewById<EditText>(R.id.usernameInput)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val loginButton = view.findViewById<Button>(R.id.loginButton)
        val registerButton = view.findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            listener.onLogin(usernameInput.text.toString(), passwordInput.text.toString())
            dismiss()
        }

        registerButton.setOnClickListener {
            listener.onRegister(usernameInput.text.toString(), passwordInput.text.toString())
            dismiss()
        }

        builder.setView(view)
        return builder.create()
    }
}
