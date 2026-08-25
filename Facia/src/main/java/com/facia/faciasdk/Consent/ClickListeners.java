package com.facia.faciasdk.Consent;

import android.annotation.SuppressLint;

import androidx.fragment.app.Fragment;

import com.facia.faciasdk.Utils.AppColors;
import com.facia.faciasdk.Camera.CameraFragment;
import com.facia.faciasdk.DocumentType.DocTypeFragment;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Utilities;
import com.facia.faciasdk.VerificationType.VerificationTypeFragment;
import com.facia.faciasdk.databinding.FragmentConsentBinding;

public class ClickListeners {
    private final FragmentConsentBinding fragmentConsentBinding;
    private final AppColors appColors;
    private final ConsentFragment consentFragment;

    protected ClickListeners(FragmentConsentBinding fragmentConsentBinding, ConsentFragment consentFragment, AppColors appColors) {
        this.fragmentConsentBinding = fragmentConsentBinding;
        this.consentFragment = consentFragment;
        this.appColors = appColors;
    }

    /**
     * method to handle consent agreement' check
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    protected void handleConsentCheck() {
        try {
            if (!consentFragment.isConsentChecked) {
                handleCheckedConsent();
            } else {
                handleUnCheckedConsent();
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/ClickListeners/handleConsentCheck");
        }
    }

    /**
     * method to handle ui if consent is checked
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void handleCheckedConsent() {
        try {
            consentFragment.isConsentChecked = true;
            fragmentConsentBinding.continueBtn.setEnabled(true);
            fragmentConsentBinding.continueBtn.setClickable(true);
            fragmentConsentBinding.continueBtn.setAlpha(1);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/ClickListeners/handleCheckedConsent");
        }
    }

    /**
     * method to handle ui if consent is unChecked
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void handleUnCheckedConsent() {
        try {
            consentFragment.isConsentChecked = false;
            fragmentConsentBinding.continueBtn.setAlpha(0.33f);
            fragmentConsentBinding.continueBtn.setEnabled(false);
            fragmentConsentBinding.continueBtn.setClickable(false);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/ClickListeners/handleUnCheckedConsent");
        }
    }

    /**
     * method to handle continue button's click
     * if consent is checked, it will call next screen
     *
     * @param showVerificationType  is to show verification type fragment
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    protected void handleContinueClick(boolean showVerificationType, Boolean showDocumentType, Boolean isSimilarity) {
        try {
            if (consentFragment.isConsentChecked) {
                if (Utilities.SimilarMethods.isConnected()) {
                    if (showVerificationType) {
                        showVerificationTypeFragment();
                    } else if (isSimilarity){
                            showCameraFragment();
                    } else if (showDocumentType){
                        showDocTypeFragment();
                    } else if (Utilities.SimilarMethods.checkCameraPermission("consent")) {
                        showCameraFragment();
                    }
                } else {
                    Utilities.SimilarMethods.internetDialog(appColors);
                }
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/ClickListeners/handleContinueClick");
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
            Webhooks.exceptionReport(e, "Consent/ClickListeners/showDocTypeFragment");
        }
    }

    /**
     * method to open camera screen
     */
    private void showCameraFragment() {
        try {
            Fragment cameraFragment = new CameraFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, cameraFragment, cameraFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/ClickListeners/showCameraFragment");
        }
    }

    /**
     * method to open verification type screen
     */
    private void showVerificationTypeFragment() {
        try {
            Fragment verificationTypeFragment = new VerificationTypeFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, verificationTypeFragment, verificationTypeFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ConsentFragment/ClickListeners/showVerificationTypeFragment");
        }
    }
}
