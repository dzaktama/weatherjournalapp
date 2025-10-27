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

// activity buat ngurusin user daftar
class RegisterActivity : AppCompatActivity() {
    private lateinit var auth : FirebaseAuth
    private lateinit var edit_email : EditText
    private lateinit var  edit_password : EditText
    private lateinit var btn_register : Button
    private lateinit var tv_login : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inisialisasi satu-satu per komponen view
        auth = Firebase.auth
        edit_email = findViewById(R.id.edit_register_email)
        edit_password = findViewById(R.id.edit_register_password)
        btn_register = findViewById(R.id.btn_register)
        tv_login = findViewById(R.id.tv_login)

        // Kasih onClickListener ke btn_login
        btn_register.setOnClickListener {
            register()
        }

        // Kasih onClickListener ke tv_register
        tv_login.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // fungsi buat ngejalanin logic register
    private fun register() {
        val email = edit_email.text.toString()
        val password = edit_password.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(baseContext, "Email atau password gaboleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d("REGISTER SUCCESS", "createUserWithEmail:success")

                    // Kasih intent yang mengarahkan ke HomeActivity
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // If sign in fails
                    Log.w("REGISTER FAILED", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
    }
}