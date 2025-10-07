package com.lms.ayan.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import com.lms.ayan.R
import com.lms.ayan.common.isValidEmail
import com.lms.ayan.controller.AuthController
import com.lms.ayan.databinding.FragmentRegistrationBinding
import com.lms.ayan.model.UserModel
import com.lms.ayan.util.DialogUtil.initLoadingDialog
import com.lms.ayan.util.isInternetAvailable
import com.lms.ayan.util.showShortToast

class RegistrationFragment : Fragment() {
    private lateinit var authController: AuthController
    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var binding: FragmentRegistrationBinding

    private lateinit var dialog: Dialog
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRegistrationBinding.inflate(layoutInflater, container, false)
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
        initBackDispatcher()
        binding.registerButton.setOnClickListener {
            if (isInternetAvailable(context)) {
                if (validate()) {
                    val student = UserModel(
                        fullName = binding.fullNameET.text.toString(),
                        email = binding.emailET.text.toString(),
                        password = binding.passwordET.text.toString()
                    )
                    showDialog()
                    authController.register(student) {
                        if (it) {
                            dismissDialog()
                            if (parentFragmentManager.backStackEntryCount > 0)
                                parentFragmentManager.popBackStack()
                        }else{
                            dismissDialog()
                        }
                    }
                }
            } else {
                showShortToast(context, context.getString(R.string.internet_not_available))
            }
        }

        binding.loginHereTV.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0)
                parentFragmentManager.popBackStack()
        }
    }

    private fun initBackDispatcher() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) { // Enable this callback
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            })
    }

    private fun validate(): Boolean {
        if (binding.fullNameET.text.isNullOrEmpty()) {
            binding.fullNameTil.error = getString(R.string.enter_full_name)
            return false
        }else{
            binding.fullNameTil.error = null
        }
        if (binding.emailET.text.isNullOrEmpty()) {
            binding.emailTiL.error = getString(R.string.enter_email)
            return false
        } else {
            if (!isValidEmail((binding.emailET.text ?: "").toString())) {
                binding.emailTiL.error = getString(R.string.enter_valid_email)
                return false
            }else{
                binding.emailTiL.error = null
            }
        }
        if (binding.passwordET.text.isNullOrEmpty()) {
            binding.passwordTiL.error = getString(R.string.enter_password)
            return false
        } else {
            if (binding.passwordET.text.toString().length < 6) {
                binding.passwordTiL.error = getString(R.string.password_length_error)
                return false
            }else{
                binding.passwordTiL.error = null
            }
        }

        if (binding.confirmPasswordET.text.isNullOrEmpty()) {
            binding.confirmPasswordTiL.error = getString(R.string.enter_confirm_password)
            return false

        } else {
            if (binding.confirmPasswordET.text.toString() != binding.passwordET.text.toString()) {
                binding.confirmPasswordTiL.error = getString(R.string.password_not_match)
                return false
            }else{
                binding.confirmPasswordTiL.error = null
            }

        }
        return true
    }

    private fun showDialog(){
        if(!dialog.isShowing) {
            dialog.show()
        }
    }

    private fun dismissDialog(){
        if(dialog.isShowing) {
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