package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMydataBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ChangePasswordBinding
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs.confirmOk
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Token
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PreferencesService
import org.apache.commons.lang3.StringUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.NoSuchAlgorithmException
import javax.inject.Inject

@AndroidEntryPoint
class MyDataActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesService: PreferencesService

    @Inject
    lateinit var rsapiClient: RSAPIClient

    private var license: License? = null
    private lateinit var profile: Profile
    private lateinit var binding: ActivityMydataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMydataBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setTitle(R.string.login)
        binding.myData.profileForm.visibility = View.INVISIBLE
        setProfileToUI(preferencesService.profile)
        oauthAuthorizationCallback(intent)
        if (isLoginDataAvailable) {
            loadRemoteProfile()
        }
        binding.myData.btLogin.setOnClickListener { login() }
        binding.myData.tvEmailVerification.setOnClickListener { requestEmailVerification() }
        binding.myData.cbLicenseCC0.setOnClickListener { selectLicense() }
        binding.myData.cbAnonymous.setOnClickListener { onAnonymousChecked() }
        binding.myData.btProfileSave.setOnClickListener { save() }
        binding.myData.btLogout.setOnClickListener { logout() }
        binding.myData.btChangePassword.setOnClickListener { changePassword() }
        binding.myData.btDeleteAccount.setOnClickListener { deleteAccount() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@MyDataActivity, MainActivity::class.java))
                finish()
            }
        })
    }

    private fun setProfileToUI(profile: Profile) {
        binding.myData.etNickname.setText(profile.nickname)
        binding.myData.etEmail.setText(profile.email)
        binding.myData.etLinking.setText(profile.link)
        license = profile.license
        binding.myData.cbLicenseCC0.isChecked = license === License.CC0
        binding.myData.cbOwnPhoto.isChecked = profile.photoOwner
        binding.myData.cbAnonymous.isChecked = profile.anonymous
        onAnonymousChecked()
        if (profile.emailVerified) {
            binding.myData.tvEmailVerification.setText(R.string.emailVerified)
            binding.myData.tvEmailVerification.setTextColor(
                resources.getColor(
                    R.color.emailVerified,
                    null
                )
            )
        } else {
            binding.myData.tvEmailVerification.setText(R.string.emailUnverified)
            binding.myData.tvEmailVerification.setTextColor(
                resources.getColor(
                    R.color.emailUnverified,
                    null
                )
            )
        }
        this.profile = profile
    }

    private fun loadRemoteProfile() {
        binding.myData.loginForm.visibility = View.VISIBLE
        binding.myData.profileForm.visibility = View.GONE
        binding.myData.progressBar.visibility = View.VISIBLE
        rsapiClient.getProfile().enqueue(object : Callback<Profile> {
            override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
                binding.myData.progressBar.visibility = View.GONE
                when (response.code()) {
                    200 -> {
                        Log.i(TAG, "Successfully loaded profile")
                        val remoteProfile = response.body()
                        if (remoteProfile != null) {
                            saveLocalProfile(remoteProfile)
                            showProfileView()
                        }
                    }

                    401 -> {
                        logout()
                        confirmOk(this@MyDataActivity, R.string.authorization_failed)
                    }

                    else -> confirmOk(
                        this@MyDataActivity,
                        getString(R.string.read_profile_failed, response.code().toString())
                    )
                }
            }

            override fun onFailure(call: Call<Profile?>, t: Throwable) {
                binding.myData.progressBar.visibility = View.GONE
                confirmOk(
                    this@MyDataActivity,
                    getString(R.string.read_profile_failed, t.message)
                )
            }
        })
    }

    private fun showProfileView() {
        binding.myData.loginForm.visibility = View.GONE
        binding.myData.profileForm.visibility = View.VISIBLE
        supportActionBar?.setTitle(R.string.tvProfile)
        binding.myData.btProfileSave.setText(R.string.bt_mydata_commit)
        binding.myData.btLogout.visibility = View.VISIBLE
        binding.myData.btChangePassword.visibility = View.VISIBLE
    }

    private fun oauthAuthorizationCallback(intent: Intent?) {
        if (intent != null && Intent.ACTION_VIEW == intent.action) {
            val data = intent.data
            if (data != null) {
                if (data.toString().startsWith(rsapiClient.redirectUri)) {
                    val code = data.getQueryParameter("code")
                    if (code != null) {
                        rsapiClient.requestAccessToken(code)
                            .enqueue(object : Callback<Token> {
                                override fun onResponse(
                                    call: Call<Token>,
                                    response: Response<Token>
                                ) {
                                    val token = response.body()
                                    if (token != null) {
                                        rsapiClient.setToken(token)
                                        loadRemoteProfile()
                                    } else {
                                        Toast.makeText(
                                            this@MyDataActivity,
                                            getString(R.string.authorization_error, "no token"),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }

                                override fun onFailure(call: Call<Token?>, t: Throwable) {
                                    Toast.makeText(
                                        this@MyDataActivity,
                                        getString(R.string.authorization_error, t.message),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            })
                    } else {
                        val error = data.getQueryParameter("error")
                        if (error != null) {
                            Toast.makeText(
                                this,
                                getString(R.string.authorization_error, error),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    if (isLoginDataAvailable) {
                        loadRemoteProfile()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        oauthAuthorizationCallback(intent)
    }

    private fun selectLicense() {
        license = if (binding.myData.cbLicenseCC0.isChecked) License.CC0 else License.UNKNOWN
        if (license !== License.CC0) {
            confirmOk(this, R.string.cc0_needed)
        }
    }

    private fun save() {
        profile = createProfileFromUI()
        if (!isValid(profile)) {
            return
        }
        if (rsapiClient.hasToken()) {
            binding.myData.progressBar.visibility = View.VISIBLE
            rsapiClient.saveProfile(profile).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    binding.myData.progressBar.visibility = View.GONE
                    when (response.code()) {
                        200 -> Log.i(TAG, "Successfully saved profile")
                        202 -> confirmOk(this@MyDataActivity, R.string.password_email)
                        400 -> confirmOk(this@MyDataActivity, R.string.profile_wrong_data)
                        401 -> {
                            logout()
                            confirmOk(this@MyDataActivity, R.string.authorization_failed)
                        }

                        409 -> confirmOk(this@MyDataActivity, R.string.profile_conflict)
                        else -> confirmOk(
                            this@MyDataActivity,
                            getString(R.string.save_profile_failed, response.code().toString())
                        )
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    binding.myData.progressBar.visibility = View.GONE
                    Log.e(TAG, "Error uploading profile", t)
                    confirmOk(
                        this@MyDataActivity,
                        getString(R.string.save_profile_failed, t.message)
                    )
                }
            })
        }
        saveLocalProfile(profile)
        Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_LONG).show()
    }

    private fun createProfileFromUI(): Profile {
        val newProfile = Profile(
            binding.myData.etNickname.text.toString().trim { it <= ' ' },
            license,
            binding.myData.cbOwnPhoto.isChecked,
            binding.myData.cbAnonymous.isChecked,
            binding.myData.etLinking.text.toString().trim { it <= ' ' },
            binding.myData.etEmail.text.toString().trim { it <= ' ' }
        )
        newProfile.emailVerified = profile.emailVerified
        return newProfile
    }

    private fun saveLocalProfile(profile: Profile) {
        preferencesService.profile = profile
        setProfileToUI(profile)
    }

    private val isLoginDataAvailable: Boolean
        get() = preferencesService.accessToken != null

    private fun isValid(profile: Profile?): Boolean {
        if (StringUtils.isBlank(profile!!.nickname)) {
            confirmOk(this, R.string.missing_nickname)
            return false
        }
        if (!isValidEmail(profile.email)) {
            confirmOk(this, R.string.missing_email_address)
            return false
        }
        val url = profile.link
        if (StringUtils.isNotBlank(url) && !isValidHTTPURL(url)) {
            confirmOk(this, R.string.missing_link)
            return false
        }
        return true
    }

    private fun isValidHTTPURL(urlString: String?): Boolean {
        try {
            val url = URL(urlString)
            if ("http" != url.protocol && "https" != url.protocol) {
                return false
            }
        } catch (e: MalformedURLException) {
            return false
        }
        return true
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        return target != null && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    private fun onAnonymousChecked() {
        if (binding.myData.cbAnonymous.isChecked) {
            binding.myData.etLinking.visibility = View.GONE
            binding.myData.tvLinking.visibility = View.GONE
        } else {
            binding.myData.etLinking.visibility = View.VISIBLE
            binding.myData.tvLinking.visibility = View.VISIBLE
        }
    }

    @Throws(NoSuchAlgorithmException::class)
    fun login() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            rsapiClient.createAuthorizeUri()
        )
        startActivity(intent)
        finish()
    }

    fun logout() {
        rsapiClient.clearToken()
        profile = Profile()
        saveLocalProfile(profile)
        binding.myData.profileForm.visibility = View.GONE
        binding.myData.loginForm.visibility = View.VISIBLE
        supportActionBar?.setTitle(R.string.login)
    }

    private fun deleteAccount() {
        SimpleDialogs.confirmOkCancel(
            this,
            R.string.deleteAccountConfirmation,
        ) { _: DialogInterface?, _: Int ->
            rsapiClient.deleteAccount()
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        when (response.code()) {
                            204 -> {
                                Log.i(TAG, "Successfully deleted account")
                                logout()
                                confirmOk(this@MyDataActivity, R.string.account_deleted)
                            }

                            401 -> {
                                confirmOk(this@MyDataActivity, R.string.authorization_failed)
                                logout()
                            }

                            else -> confirmOk(
                                this@MyDataActivity,
                                getString(
                                    R.string.account_deletion_failed,
                                    response.code().toString()
                                )
                            )
                        }
                    }

                    override fun onFailure(call: Call<Void?>, t: Throwable) {
                        Log.e(TAG, "Error deleting account", t)
                        confirmOk(
                            this@MyDataActivity,
                            getString(R.string.account_deletion_failed, t.message)
                        )
                    }
                })
        }
    }

    private fun changePassword() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
        val passwordBinding = ChangePasswordBinding.inflate(
            layoutInflater
        )
        builder.setTitle(R.string.bt_change_password)
            .setView(passwordBinding.root)
            .setIcon(R.mipmap.ic_launcher)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        val alertDialog = builder.create()
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            var newPassword: String =
                getValidPassword(passwordBinding.password, passwordBinding.passwordRepeat)
                    ?: return@setOnClickListener
            alertDialog.dismiss()
            try {
                newPassword = URLEncoder.encode(newPassword, StandardCharsets.UTF_8.toString())
            } catch (e: UnsupportedEncodingException) {
                Log.e(TAG, "Error encoding new password", e)
            }
            rsapiClient.changePassword(newPassword).enqueue(object : Callback<Void?> {
                override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                    when (response.code()) {
                        200 -> {
                            Log.i(TAG, "Successfully changed password")
                            logout()
                            confirmOk(this@MyDataActivity, R.string.password_changed)
                        }

                        401 -> {
                            confirmOk(this@MyDataActivity, R.string.authorization_failed)
                            logout()
                        }

                        else -> confirmOk(
                            this@MyDataActivity,
                            getString(R.string.change_password_failed, response.code().toString())
                        )
                    }
                }

                override fun onFailure(call: Call<Void?>, t: Throwable) {
                    Log.e(TAG, "Error changing password", t)
                    confirmOk(
                        this@MyDataActivity,
                        getString(R.string.change_password_failed, t.message)
                    )
                }
            })
        }
    }

    private fun getValidPassword(etNewPassword: EditText, etPasswordRepeat: EditText): String? {
        val newPassword = etNewPassword.text.toString().trim { it <= ' ' }
        if (newPassword.length < 8) {
            Toast.makeText(this@MyDataActivity, R.string.password_too_short, Toast.LENGTH_LONG)
                .show()
            return null
        }
        if (newPassword != etPasswordRepeat.text.toString().trim { it <= ' ' }) {
            Toast.makeText(this@MyDataActivity, R.string.password_repeat_fail, Toast.LENGTH_LONG)
                .show()
            return null
        }
        return newPassword
    }

    fun requestEmailVerification() {
        SimpleDialogs.confirmOkCancel(
            this,
            R.string.requestEmailVerification
        ) { _: DialogInterface?, _: Int ->
            rsapiClient.resendEmailVerification()
                .enqueue(object : Callback<Void?> {
                    override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                        if (response.code() == 200) {
                            Log.i(TAG, "Successfully requested email verification")
                            Toast.makeText(
                                this@MyDataActivity,
                                R.string.emailVerificationRequested,
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MyDataActivity,
                                R.string.emailVerificationRequestFailed,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<Void?>, t: Throwable) {
                        Log.e(TAG, "Error requesting email verification", t)
                        Toast.makeText(
                            this@MyDataActivity,
                            R.string.emailVerificationRequestFailed,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }

    companion object {
        private val TAG = MyDataActivity::class.java.simpleName
    }
}