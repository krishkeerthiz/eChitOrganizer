package com.yourapp.seetuorganizer.ui.signIn

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.yourapp.seetuorganizer.MainActivity
import com.yourapp.seetuorganizer.R
import com.yourapp.seetuorganizer.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySignInBinding

    private val signIn : ActivityResultLauncher<Intent> =
        registerForActivityResult(FirebaseAuthUIActivityResultContract(), this::onSignInResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        if(Firebase.auth.currentUser == null){
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(listOf(
                    AuthUI.IdpConfig.PhoneBuilder().build()
                ))
                .build()

            signIn.launch(signInIntent)
        }
        else{
            goToMainActivity()
        }
    }

    private fun onSignInResult(result : FirebaseAuthUIAuthenticationResult){
        if(result.resultCode == RESULT_OK){
            goToMainActivity()
        }
        else{
            Toast.makeText(
                this,
                "There was an error signing in",
                Toast.LENGTH_LONG).show()

            val response = result.idpResponse
            if (response == null) {
                Log.w(TAG, "Sign in canceled")
            } else {
                Log.w(TAG, "Sign in error", response.error)
            }
        }

    }

    private fun goToMainActivity(){
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object{
        private const val TAG = "SignInActivity"
    }

}