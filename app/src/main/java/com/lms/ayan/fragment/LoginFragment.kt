package com.lms.ayan.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lms.ayan.R
import com.lms.ayan.activity.HomeActivity
import com.lms.ayan.common.isValidEmail
import com.lms.ayan.controller.AuthController
import com.lms.ayan.databinding.FragmentLoginBinding
import com.lms.ayan.util.DialogUtil.initLoadingDialog
import com.lms.ayan.util.isInternetAvailable
import com.lms.ayan.util.replaceFragment
import com.lms.ayan.util.showShortToast

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private lateinit var context: Context
    private lateinit var activity: Activity

    private lateinit var authController: AuthController
    private lateinit var dialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        context = requireContext()
        activity = requireActivity()
        authController = AuthController(context)
        dialog = initLoadingDialog(context)
        binding.registerHereTV.setOnClickListener {
            replaceFragment(parentFragmentManager, RegistrationFragment(), R.id.container, true)
        }

        binding.loginButton.setOnClickListener {
            if (isInternetAvailable(context)) {
                if (validate()) {
                    showDialog()
                    authController.login(
                        binding.emailET.text.toString(),
                        binding.passwordET.text.toString()
                    ) { response ->
                        dismissDialog()
                        if (response) {
                            val intent = Intent(context, HomeActivity::class.java)
                            startActivity(intent)
                            activity.finish()
                        }
                    }
                }
            } else {
                showShortToast(context, context.getString(R.string.internet_not_available))
            }
        }
    }

    private fun validate(): Boolean {
        if (binding.emailET.text.isNullOrEmpty()) {
            binding.emailTiL.error = getString(R.string.enter_email)
            return false
        } else {
            if (isValidEmail((binding.emailET.text ?: "").toString())) {
                binding.emailTiL.error = null
            } else {
                binding.emailTiL.error = getString(R.string.enter_valid_email)
                return false
            }
        }

        if (binding.passwordET.text.isNullOrEmpty()) {
            binding.passwordTiL.error = getString(R.string.enter_password)
            return false
        } else {
            binding.passwordTiL.error = null
        }
        return true
    }

    private fun showDialog() {
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    private fun dismissDialog() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    override fun onStop() {
        super.onStop()
        dismissDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissDialog()
    }

}