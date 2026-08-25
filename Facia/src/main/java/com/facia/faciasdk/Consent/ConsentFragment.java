package com.facia.faciasdk.Consent;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
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
import com.facia.faciasdk.R;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ApiConstants;
import com.facia.faciasdk.Utils.Constants.TimeConstants;
import com.facia.faciasdk.databinding.FragmentConsentBinding;

import java.util.HashMap;

public class ConsentFragment extends Fragment implements View.OnClickListener {
    protected Boolean isConsentChecked = false;
    private AppColors appColors ;
    private RequestModel requestModel;
    /**
     * to handle screen's back press click
     */
    OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            try {
                exitDialog();
            } catch (Exception e) {
                Webhooks.exceptionReport(e, "ConsentFragment/handleOnBackPressed");
            }
        }
    };
    private FragmentConsentBinding fragmentConsentBinding;
    private ClickListeners clickListeners;

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentConsentBinding = FragmentConsentBinding.inflate(inflater, container, false);
        initialization();
        return fragmentConsentBinding.getRoot();
    }

    /**
     * method to initialize instances and values
     * init callback for back press
     * invoking method for assigning click listeners
     */
    private void initialization() {
        try {
            requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);
            appColors = new AppColors(requestModel.getConfigObject(),getContext());
            fragmentConsentBinding.continueBtn.setBackgroundTintList(ColorStateList.valueOf(appColors.getButtonBgColor()));
            fragmentConsentBinding.continueBtn.setTextColor(appColors.getButtonTextColor());
            fragmentConsentBinding.headingTxt.setTextColor(appColors.getDarkTextColor());
            fragmentConsentBinding.consentText1.setTextColor(appColors.getDarkTextColor());
            fragmentConsentBinding.consentText2.setTextColor(appColors.getDarkTextColor());
            fragmentConsentBinding.consentText3.setTextColor(appColors.getDarkTextColor());
            fragmentConsentBinding.iAgreeTxt.setTextColor(appColors.getDarkTextColor());
            fragmentConsentBinding.customisedIAgreeTxt.setTextColor(appColors.getDarkTextColor());


            if (requestModel.getConfigObject().has("halaMobilityConsent")){
                if (requestModel.getConfigObject().getBoolean("halaMobilityConsent")){
                    setCustomisedConsentStrings();
                }else {
                    fragmentConsentBinding.iAgreeTxt.setVisibility(View.VISIBLE);
                    fragmentConsentBinding.customisedIAgreeTxt.setVisibility(View.GONE);
                }
            }else {
                fragmentConsentBinding.iAgreeTxt.setVisibility(View.VISIBLE);
                fragmentConsentBinding.customisedIAgreeTxt.setVisibility(View.GONE);
            }
//            setConsentCheckStr();
            fragmentConsentBinding.iAgreeTxt.setMovementMethod(LinkMovementMethod.getInstance());
            fragmentConsentBinding.customisedIAgreeTxt.setMovementMethod(LinkMovementMethod.getInstance());
            fragmentConsentBinding.iAgreeCheckBox.setMovementMethod(LinkMovementMethod.getInstance());
            clickListeners = new ClickListeners(fragmentConsentBinding, this , appColors);
            fragmentConsentBinding.iAgreeCheckBox.setChecked(false);
            isConsentChecked = false;
            requireActivity().getOnBackPressedDispatcher().addCallback(this.requireActivity(), backPressCallBack);
            assignClickListeners();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/initialization");
        }
    }

    private void setCustomisedConsentStrings() {
        try {
            fragmentConsentBinding.customisedIAgreeTxt.setVisibility(View.VISIBLE);
            fragmentConsentBinding.iAgreeTxt.setVisibility(View.GONE);
            fragmentConsentBinding.consentText1.setText(SingletonData.getInstance().getActivity().getString(R.string.customised_consent_str1));
            fragmentConsentBinding.consentText2.setText(SingletonData.getInstance().getActivity().getString(R.string.customised_consent_str2));
            fragmentConsentBinding.consentText3.setText(SingletonData.getInstance().getActivity().getString(R.string.customised_consent_str3));
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/setCustomisedConsentStrings");
        }
    }

    /**
     * assigning click listeners of ui elements
     */
    private void assignClickListeners() {
        try {
            fragmentConsentBinding.continueBtn.setOnClickListener(this);
            fragmentConsentBinding.iAgreeCheckBox.setOnClickListener(this);
            fragmentConsentBinding.iAgreeTxt.setOnClickListener(this);
            fragmentConsentBinding.customisedIAgreeTxt.setOnClickListener(this);
            fragmentConsentBinding.consentAgreeParent.setOnClickListener(this);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/assignClickListeners");
        }
    }

    /**
     * callback for UI clicks
     *
     * @param view view on which user clicked
     */
    @Override
    public void onClick(View view) {
        try {
            synchronized (view) {
                view.setEnabled(false);
                int id = view.getId();
                if (id == R.id.continueBtn) {
                    clickListeners.handleContinueClick(!requestModel.isSimilarity() && requestModel.getConfigObject()
                            .getBoolean("showVerificationType"), ServiceType.valueOf(
                            requestModel.getConfigObject().getString("serviceType")) == ServiceType.DOCUMENT_LIVENESS &&
                            requestModel.getConfigObject().getBoolean("showDocumentType"), requestModel.isSimilarity());
                } else if (id == R.id.iAgreeCheckBox) {
                    clickListeners.handleConsentCheck();
                } else if (id == R.id.iAgreeTxt || id == R.id.consentAgreeParent || id == R.id.customisedIAgreeTxt) {
                    if (fragmentConsentBinding.iAgreeCheckBox.isChecked()) {
                        fragmentConsentBinding.iAgreeCheckBox.setChecked(false);
                    } else {
                        fragmentConsentBinding.iAgreeCheckBox.setChecked(true);
                    }
                    clickListeners.handleConsentCheck();
                }
                new Handler().postDelayed(() -> view.setEnabled(true), TimeConstants.SYNCHRONIZED_CONSTANT);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/onClick");
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

            titleTxt.setTextColor(appColors.getDarkTextColor());
            subTitleTxt.setTextColor(appColors.getLightTextColor());
            negativeButton.setTextColor(appColors.getDarkTextColor());
            positiveButton.setBackgroundTintList(ColorStateList.valueOf(appColors.getButtonBgColor()));
            positiveButton.setTextColor(appColors.getDialogButtonTextColor());
            negativeButton.setOnClickListener(view -> dialog.dismiss());
            positiveButton.setOnClickListener(view -> terminateSDK(dialog));
            dialog.show();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/exitDialog");
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
            Webhooks.exceptionReport(e, "ConsentFragment/terminateSDK");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
