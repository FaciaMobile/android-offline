package com.facia.faciasdk.DocumentType;

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

import com.facia.faciasdk.Activity.Helpers.Enums.DocumentType;
import com.facia.faciasdk.Activity.Helpers.IntentHelper;
import com.facia.faciasdk.Activity.Helpers.RequestModel;
import com.facia.faciasdk.Utils.AppColors;
import com.facia.faciasdk.Camera.CameraFragment;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ApiConstants;
import com.facia.faciasdk.Utils.Constants.TimeConstants;
import com.facia.faciasdk.Utils.Utilities;
import com.facia.faciasdk.databinding.FragmentKycDocTypeBinding;

import java.util.HashMap;

public class KycDocTypeFragment extends Fragment implements View.OnClickListener {
    private RequestModel requestModel;
    private AppColors appColors;
    private FragmentKycDocTypeBinding binding;

    OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            try {
                exitDialog();
            } catch (Exception e) {
                Webhooks.exceptionReport(e, "KycDocTypeFragment/backPressCallBack");
            }
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentKycDocTypeBinding.inflate(inflater, container, false);
        initialization();
        return binding.getRoot();
    }

    private void initialization() {
        try {
            requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);
            appColors = new AppColors(requestModel.getConfigObject(), getContext());
            requireActivity().getOnBackPressedDispatcher().addCallback(this.requireActivity(), backPressCallBack);
            binding.passportKycLayout.setOnClickListener(this);
            binding.drivingLicenseKycLayout.setOnClickListener(this);
            binding.idCardKycLayout.setOnClickListener(this);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "KycDocTypeFragment/initialization");
        }
    }

    @Override
    public void onClick(View view) {
        try {
            synchronized (view) {
                view.setEnabled(false);
                int id = view.getId();
                if (id == R.id.passportKycLayout) {
                    if (Utilities.SimilarMethods.isConnected()) {
                        if (Utilities.SimilarMethods.checkCameraPermission("passportDoc")) {
                            requestModel.getConfigObject().put("documentType", DocumentType.PASSPORT);
                            showCameraFragment();
                        }
                    } else {
                        Utilities.SimilarMethods.internetDialog(appColors);
                    }
                } else if (id == R.id.drivingLicenseKycLayout) {
                    if (Utilities.SimilarMethods.isConnected()) {
                        if (Utilities.SimilarMethods.checkCameraPermission("idCardDoc")) {
                            requestModel.getConfigObject().put("documentType", DocumentType.DRIVING_LICENSE);
                            showCameraFragment();
                        }
                    } else {
                        Utilities.SimilarMethods.internetDialog(appColors);
                    }
                } else if (id == R.id.idCardKycLayout) {
                    if (Utilities.SimilarMethods.isConnected()) {
                        if (Utilities.SimilarMethods.checkCameraPermission("idCardDoc")) {
                            requestModel.getConfigObject().put("documentType", DocumentType.ID_CARD);
                            showCameraFragment();
                        }
                    } else {
                        Utilities.SimilarMethods.internetDialog(appColors);
                    }
                }
            }
            new Handler().postDelayed(() -> view.setEnabled(true), TimeConstants.SYNCHRONIZED_CONSTANT);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "KycDocTypeFragment/onClick");
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
            Webhooks.exceptionReport(e, "KycDocTypeFragment/showCameraFragment");
        }
    }

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

            titleTxt.setTextColor(appColors.getDarkTextColor());
            subTitleTxt.setTextColor(appColors.getLightTextColor());
            negativeButton.setTextColor(appColors.getDarkTextColor());
            positiveButton.setBackgroundTintList(ColorStateList.valueOf(appColors.getButtonBgColor()));
            positiveButton.setTextColor(appColors.getDialogButtonTextColor());
            negativeButton.setOnClickListener(v -> dialog.dismiss());
            positiveButton.setOnClickListener(v -> terminateSDK(dialog));
            dialog.show();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "KycDocTypeFragment/exitDialog");
        }
    }

    private void terminateSDK(Dialog dialog) {
        try {
            dialog.dismiss();
            HashMap<String, String> requestResponseObj = new HashMap<>();
            requestResponseObj.put("reference_id", "");
            requestResponseObj.put("event", "request.cancelled");
            requestModel.getRequestListener().requestStatus(requestResponseObj);
            SingletonData.getInstance().getActivity().finish();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "KycDocTypeFragment/terminateSDK");
        }
    }
}
