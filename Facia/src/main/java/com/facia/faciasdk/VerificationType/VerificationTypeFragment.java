package com.facia.faciasdk.VerificationType;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.facia.faciasdk.Activity.Helpers.Enums.ServiceType;
import com.facia.faciasdk.Activity.Helpers.IntentHelper;
import com.facia.faciasdk.Activity.Helpers.RequestModel;
import com.facia.faciasdk.Utils.AppColors;
import com.facia.faciasdk.Camera.CameraFragment;
import com.facia.faciasdk.DocumentType.DocTypeFragment;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ApiConstants;
import com.facia.faciasdk.Utils.Constants.TimeConstants;
import com.facia.faciasdk.Utils.Utilities;
import com.facia.faciasdk.databinding.FragmentVerificationTypeBinding;

import java.util.HashMap;

public class VerificationTypeFragment extends Fragment implements View.OnClickListener {
    private RequestModel requestModel;
    private AppColors appColors;

    /**
     * method to handle screen's back press click
     */
    OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            try {
                exitDialog();
            } catch (Exception e) {
                Webhooks.exceptionReport(e, "VerificationTypeFragment/backPressCallBack");
            }
        }
    };
    private FragmentVerificationTypeBinding fragmentVerificationTypeBinding;

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentVerificationTypeBinding = FragmentVerificationTypeBinding.inflate(inflater, container, false);
        initialization();
        return fragmentVerificationTypeBinding.getRoot();
    }

    /**
     * method to initialize instances
     * setting listeners, callbacks
     */
    private void initialization() {
        try {
            requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);
            appColors = new AppColors(requestModel.getConfigObject(), getContext());
            fragmentVerificationTypeBinding.docLivenessTypeTxt.setTextColor(appColors.getDarkTextColor());
            fragmentVerificationTypeBinding.verificationTypeHeadingTxt.setTextColor(appColors.getDarkTextColor());
            fragmentVerificationTypeBinding.faceDocTypeTxt.setTextColor(appColors.getDarkTextColor());
            fragmentVerificationTypeBinding.faceTypeTxt.setTextColor(appColors.getDarkTextColor());
            requireActivity().getOnBackPressedDispatcher().addCallback(this.requireActivity(), backPressCallBack);
            if (requestModel.getConfigObject().getBoolean("docLivenessOpt")) {
                fragmentVerificationTypeBinding.docLivenessParentLayout.setVisibility(View.VISIBLE);
            } else {
                fragmentVerificationTypeBinding.docLivenessParentLayout.setVisibility(View.GONE);
            }
            assignClickListeners();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "VerificationTypeFragment/initialization");
        }
    }

    /**
     * assigning UI click listener
     */
    private void assignClickListeners() {
        try {
            fragmentVerificationTypeBinding.faceLivenessParentLayout.setOnClickListener(this);
            fragmentVerificationTypeBinding.matchIdParentLayout.setOnClickListener(this);
            fragmentVerificationTypeBinding.docLivenessParentLayout.setOnClickListener(this);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "VerificationTypeFragment/assignClickListeners");
        }
    }

    /**
     * callback for UI clicks
     *
     * @param view view instance on which user clicked
     */
    @Override
    public void onClick(View view) {
        try {
            synchronized (view) {
                view.setEnabled(false);
                int id = view.getId();
                if (id == R.id.faceLivenessParentLayout) {
                    if (Utilities.SimilarMethods.isConnected()) {
                        if (Utilities.SimilarMethods.checkCameraPermission("faceLiveness")) {
                            requestModel.getConfigObject().put("serviceType", ServiceType.FACE_LIVENESS);
                            showCameraFragment();
                        }
                    } else {
                        Utilities.SimilarMethods.internetDialog(appColors);
                    }
                } else if (id == R.id.matchIdParentLayout) {
                    if (Utilities.SimilarMethods.isConnected()) {
                        if (Utilities.SimilarMethods.checkCameraPermission("matchId")) {
                            requestModel.getConfigObject().put("serviceType", ServiceType.MATCH_TO_PHOTO_ID);
                            showCameraFragment();
                        }
                    } else {
                        Utilities.SimilarMethods.internetDialog(appColors);
                    }
                } else if (id == R.id.docLivenessParentLayout) {
                    if (Utilities.SimilarMethods.isConnected()) {
                        requestModel.getConfigObject().put("serviceType", ServiceType.DOCUMENT_LIVENESS);
                        if (requestModel.getConfigObject().getBoolean("showDocumentType")) {
                            showDocTypeFragment();
                        } else if (Utilities.SimilarMethods.checkCameraPermission("documentLiveness")) {
                            showCameraFragment();
                        }
                    } else {
                        Utilities.SimilarMethods.internetDialog(appColors);
                    }
                }
            }
            new Handler().postDelayed(() -> view.setEnabled(true), TimeConstants.SYNCHRONIZED_CONSTANT);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "VerificationTypeFragment/onClick");
        }
    }

    private void showCameraFragment() {
        try {
            Fragment cameraFragment = new CameraFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, cameraFragment, cameraFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "VerificationTypeFragment/showCameraFragment");
        }
    }

    private void showDocTypeFragment() {
        try {
            Fragment docTypeFragment = new DocTypeFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, docTypeFragment, docTypeFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "VerificationTypeFragment/showDocTypeFragment");
        }
    }

    /**
     * method to show exit Dialog on back press
     */
    private void exitDialog() {
        try {
            Dialog dialog = new Dialog(SingletonData.getInstance().getContext());
            dialog.setContentView(R.layout.exit_dialog_box);
            dialog.setCancelable(false);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            Button negativeButton = dialog.findViewById(R.id.reject);
            Button positiveButton = dialog.findViewById(R.id.accept);
            TextView titleTxt = dialog.findViewById(R.id.title);
            TextView subTitleTxt = dialog.findViewById(R.id.sub_title);
            // Apply colors
            titleTxt.setTextColor(appColors.getDarkTextColor());
            subTitleTxt.setTextColor(appColors.getLightTextColor());
            negativeButton.setTextColor(appColors.getDarkTextColor());
            positiveButton.setBackgroundTintList(ColorStateList.valueOf(appColors.getButtonBgColor()));
            positiveButton.setTextColor(appColors.getDialogButtonTextColor());
            negativeButton.setOnClickListener(view -> dialog.dismiss());
            positiveButton.setOnClickListener(view -> terminateSDK(dialog));
            dialog.show();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "VerificationTypeFragment/exitDialog");
        }
    }


    /**
     * method to terminate sdk in case of back press (request cancellation)
     */
    private void terminateSDK(Dialog dialog) {
        try {
            dialog.dismiss();
            HashMap<String, String> requestResponseObj = new HashMap<>();
            requestResponseObj.put("reference_id", "");
            requestResponseObj.put("event", "request.cancelled");
            requestModel.getRequestListener().requestStatus(requestResponseObj);
            SingletonData.getInstance().getActivity().finish();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "VerificationTypeFragment/terminateSDK");
        }
    }
}
