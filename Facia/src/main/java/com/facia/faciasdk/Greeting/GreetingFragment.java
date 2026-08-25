package com.facia.faciasdk.Greeting;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.facia.faciasdk.Activity.Helpers.IntentHelper;
import com.facia.faciasdk.Activity.Helpers.RequestModel;
import com.facia.faciasdk.Camera.CameraFragment;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ApiConstants;
import com.facia.faciasdk.Utils.Constants.TimeConstants;
import com.facia.faciasdk.VerificationType.VerificationTypeFragment;
import com.facia.faciasdk.databinding.FragmentGreetingBinding;

import java.util.HashMap;

public class GreetingFragment extends Fragment implements View.OnClickListener{
    private RequestModel requestModel;

    /**
     * method to handle screen's back press click
     */
    OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
        }
    };
    private final HashMap<String, String> requestResponseObj;
    private FragmentGreetingBinding fragmentGreetingBinding;

    public GreetingFragment(HashMap<String, String> requestResponseObj) {
        this.requestResponseObj = requestResponseObj;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentGreetingBinding = FragmentGreetingBinding.inflate(inflater, container, false);
        initialization();
        return fragmentGreetingBinding.getRoot();
    }

    /**
     * method to initialize instances
     * setting listeners, callbacks
     */
    private void initialization() {
        try {
            requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);
            requireActivity().getOnBackPressedDispatcher().addCallback(this.requireActivity(), backPressCallBack);
            fragmentGreetingBinding.continueBtn.setOnClickListener(this);
            fragmentGreetingBinding.tryAgainBtn.setOnClickListener(this);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "GreetingFragment/initialization");
        }
    }

    /**
     * method to navigate to camera screen
     */
    private void navigateToCamera() {
        try {
            Fragment cameraFragment = new CameraFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, cameraFragment, cameraFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "GreetingFragment/navigateToCameraScreen");
        }
    }

    /**
     * method to navigate to verificationType screen
     */
    private void navigateToVerificationType() {
        try {
            Fragment verificationTypeFragment = new VerificationTypeFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, verificationTypeFragment, verificationTypeFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "GreetingFragment/verificationTypeFragment");
        }
    }

    @Override
    public void onClick(View view) {
        synchronized (view) {
            view.setEnabled(false);
            int id = view.getId();
            if (id == R.id.tryAgainBtn) {
                try {
//                    requestModel.getRequestListener().requestStatus(requestResponseObj);
                    SingletonData.getInstance().setReferenceId("");
                    if (!requestModel.isSimilarity() && requestModel.getConfigObject().getBoolean("showVerificationType")) {
                        navigateToVerificationType();
                    }else {
                        navigateToCamera();
                    }
                }catch (Exception e){
                    Webhooks.exceptionReport(e, "GreetingFragment/onClick/tryAgainBtn clicked");
                }
            } else if (id == R.id.continueBtn) {
                requestModel.getRequestListener().requestStatus(requestResponseObj);
                SingletonData.getInstance().getActivity().finish();
            }
        }
        new Handler().postDelayed(() -> view.setEnabled(true), TimeConstants.SYNCHRONIZED_CONSTANT);
    }
}
