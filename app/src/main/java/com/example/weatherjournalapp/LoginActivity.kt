package com.example.weatherjournalapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

// activity buat ngurusin user login
class LoginActivity : AppCompatActivity() {
    private lateinit var auth : FirebaseAuth
    private lateinit var edit_email : EditText
    private lateinit var  edit_password : EditText
    private lateinit var btn_login : Button
    private lateinit var tv_register : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inisialisasi satu-satu per komponen view
        auth = Firebase.auth
        edit_email = findViewById(R.id.edit_login_email)
        edit_password = findViewById(R.id.edit_login_password)
        btn_login = findViewById(R.id.btn_login)
        tv_register = findViewById(R.id.tv_register)

        // Kasih onClickListener ke btn_login
        btn_login.setOnClickListener {
            login()
        }

        // Kasih onClickListener ke tv_register
        tv_register.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // fungsi buat ngejalanin logic login
    private fun login() {
        val email = edit_email.text.toString()
        val password = edit_password.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(baseContext, "Email atau password gaboleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d("LOGIN SUCCESS", "signInWithEmail:success")

                    // Kasih intent yang mengarahkan ke HomeActivity
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // If sign in fails
                    Log.w("LOGIN FAIL", "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }
}