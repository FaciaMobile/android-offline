package com.facia.faciademo.Login;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.facia.faciademo.ErrorLogs;
import com.facia.faciademo.MainSetAndGetData;
import com.facia.faciademo.databinding.FragmentLoginBinding;
import com.facia.faciasdk.Singleton.SingletonData;

public class LoginFragment extends Fragment {
    /**
     * to handle screen's back press click
     */
    OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            try {
                MainSetAndGetData.getInstance().getActivity().finish();
            } catch (Exception e) {
                ErrorLogs.exceptionReport(e, "LoginFragment/handleOnBackPressed");
            }
        }
    };
    private FragmentLoginBinding fragmentloginBinding;
    private ApiHelper apiHelper;

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentloginBinding = FragmentLoginBinding.inflate(inflater, container, false);
        initialization();
        return fragmentloginBinding.getRoot();
    }

    /**
     * method to initialize instances, callbacks & click listeners
     */
    private void initialization() {
        try {
            apiHelper = new ApiHelper(fragmentloginBinding);
            requireActivity().getOnBackPressedDispatcher().addCallback(this.requireActivity(), backPressCallBack);
            fragmentloginBinding.loginBtn.setOnClickListener(view -> setValuesToGetToken());
            fragmentloginBinding.loginMainLayout.setOnClickListener(view -> hideKeyboard());
            fragmentloginBinding.isDemoAppSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    //flow for demo app
                    fragmentloginBinding.showConsentSwitch.setChecked(true);
                    fragmentloginBinding.showVerificationSwitch.setChecked(true);
                    fragmentloginBinding.showDocTypeSwitch.setChecked(false);
                    fragmentloginBinding.showAddVerifSwitch.setChecked(false);
                    fragmentloginBinding.showResult.setChecked(true);
                    fragmentloginBinding.showFeedbackSwitch.setChecked(true);
                    fragmentloginBinding.showGreetingSwitch.setChecked(true);
                    fragmentloginBinding.qlTimeSwitch.setChecked(true);
                    fragmentloginBinding.dlTimeSwitch.setChecked(true);
                    fragmentloginBinding.blinkTimeSwitch.setChecked(true);
                    fragmentloginBinding.qlLivenessRetrySwitch.setChecked(true);
                    fragmentloginBinding.qlIdMatchRetrySwitch.setChecked(true);
                    fragmentloginBinding.qlBottomInstSwitch.setChecked(false);
                    fragmentloginBinding.ql3dLivenessRetryInstSwitch.setChecked(true);
                    fragmentloginBinding.qlIdMatchRetryInstSwitch.setChecked(true);
                    fragmentloginBinding.ql3dLivenessRetryCounterEdt.setText("3");
                    fragmentloginBinding.qlIdMatchRetryCounterEdt.setText("3");
                    fragmentloginBinding.isDemoAppSwitch.setChecked(true);
                    fragmentloginBinding.emulatorDetectionSwitch.setChecked(true);
                    fragmentloginBinding.faceLivenessOpt.setChecked(true);
                    fragmentloginBinding.defaultOpt.setChecked(true);
                    fragmentloginBinding.thresholdMediumOpt.setChecked(true);
                    fragmentloginBinding.detectLightSwitch.setChecked(true);
                    fragmentloginBinding.largeOpt.setChecked(true);
                    fragmentloginBinding.docLivenessOptSwitch.setChecked(false);
                }
            });

            fragmentloginBinding.serviceTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == fragmentloginBinding.faceLivenessOpt.getId()){
                    fragmentloginBinding.livenessTypeRadioGroup.setVisibility(View.VISIBLE);
                    fragmentloginBinding.matchIdThroughCam.setVisibility(View.GONE);
                    fragmentloginBinding.docTypeRadioGroup.setVisibility(View.GONE);
                } else if (checkedId == fragmentloginBinding.matchIdOpt.getId()){
                    fragmentloginBinding.livenessTypeRadioGroup.setVisibility(View.GONE);
                    fragmentloginBinding.matchIdThroughCam.setVisibility(View.GONE);
                    fragmentloginBinding.docTypeRadioGroup.setVisibility(View.GONE);
                } else if (checkedId == fragmentloginBinding.docLivenessOpt.getId()){
                    fragmentloginBinding.livenessTypeRadioGroup.setVisibility(View.GONE);
                    fragmentloginBinding.matchIdThroughCam.setVisibility(View.GONE);
                    fragmentloginBinding.docTypeRadioGroup.setVisibility(View.VISIBLE);
                }
            });
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "LoginFragment/initialization");
        }
    }

    /**
     * Method to hide keyboard forcefully
     */
    private void hideKeyboard() {
        try {
            if (MainSetAndGetData.getInstance().getActivity().getWindow().getDecorView().getRootView() != null) {
                InputMethodManager v = (InputMethodManager) MainSetAndGetData.getInstance().getActivity().getWindow().getDecorView().getRootView().getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                v.hideSoftInputFromWindow(MainSetAndGetData.getInstance().getActivity().getWindow().getDecorView().getRootView().getWindowToken(), 0);
            }
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "LoginFragment/hideKeyboard");
        }
    }

    /**
     * method to set singleton values
     * invoking further method to set JsonObject to get token
     */
    private void setValuesToGetToken() {
        try {
            SingletonData.getInstance().setQuickLivenessFrameCount(0);
            apiHelper.getTokenJsonObject(MainSetAndGetData.getInstance().getActivity(),
                    MainSetAndGetData.getInstance().getContext(), fragmentloginBinding.emailTxt.getText().toString().trim(),
                    fragmentloginBinding.pwdTxt.getText().toString().trim());
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "LoginFragment/setValuesToGetToken");
        }
    }
}