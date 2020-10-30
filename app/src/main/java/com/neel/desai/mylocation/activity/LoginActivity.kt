package com.neel.desai.mylocation.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.databinding.ActivityLoginBinding
import com.neel.desai.mylocation.util.SharedPreference
import com.neel.desai.mylocation.viewmodel.LoginViewModel


class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var loginBinding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginViewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)
        loginBinding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        loginBinding.logininput = loginViewModel
        loginBinding.lifecycleOwner = this

        SharedPreference.getInstance(applicationContext)

        if (!SharedPreference.getDecryptedStringValue(SharedPreference.PREF_APP_KEY_USER_NAME)
                .equals("") && !SharedPreference.getDecryptedStringValue(SharedPreference.PREF_APP_KEY_PASSWORD)
                .equals("")
        ) {
            var intent: Intent
            intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        loginViewModel.getUser().observe(this, Observer { loginUser ->
            var isValid: Boolean = false
            if (TextUtils.isEmpty(loginUser.strEmailAddress)) {
                loginBinding.tilUseId.setError("Enter an Username")
                loginBinding.tilUseId.requestFocus()
                isValid = true
            } else {
                loginBinding.tilUseId.setError("")

            }

            if (TextUtils.isEmpty(loginUser.strPassword)) {
                loginBinding.tilPassword.setError("Enter a Password")
                loginBinding.tilPassword.requestFocus()
                isValid = true
            } else {
                loginBinding.tilPassword.setError("")
            }

            if (loginUser.strPassword!!.length < 3) {
                loginBinding.tilPassword.setError("Enter at least 6 Digit password")
                loginBinding.tilPassword.requestFocus()
                isValid = true
            } else {
                loginBinding.tilPassword.setError("")
            }

            if (!isValid) {

                if (loginUser.strEmailAddress.equals(
                        "neel",
                        true
                    ) && loginUser.strPassword.equals("neel", true)
                ) {

                    SharedPreference.getInstance(applicationContext)
                    SharedPreference.setEncryptedStringValue(
                        SharedPreference.PREF_APP_KEY_USER_NAME,
                        loginUser.strEmailAddress!!
                    )
                    SharedPreference.setEncryptedStringValue(
                        SharedPreference.PREF_APP_KEY_PASSWORD,
                        loginUser.strPassword!!
                    )
                    var intent: Intent
                    intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Wrong username or Password ",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }

        })

    }
}


