package com.facia.faciasdk.TrialEnd;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.AppColors;
import com.facia.faciasdk.Utils.Constants.TimeConstants;
import com.facia.faciasdk.databinding.FragmentTrialEndBinding;

public class TrialEndFragment extends Fragment implements View.OnClickListener {
    /**
     * method to handle screen's back press click
     */
    OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            exitDialog();
        }
    };
    private FragmentTrialEndBinding fragmentTrialEndBinding;
    private AppColors appColors;

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentTrialEndBinding = FragmentTrialEndBinding.inflate(inflater, container, false);
        initialization();
        return fragmentTrialEndBinding.getRoot();
    }

    /**
     * method to initialize instances
     * setting listeners, callbacks
     */
    private void initialization() {
        try {
            requireActivity().getOnBackPressedDispatcher().addCallback(this.requireActivity(), backPressCallBack);
            fragmentTrialEndBinding.emailParentLayout.setOnClickListener(this);
            fragmentTrialEndBinding.meetingParentLayout.setOnClickListener(this);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "TrialEndFragment/initialization");
        }
    }

    @Override
    public void onClick(View view) {
        synchronized (view) {
            view.setEnabled(false);
            int id = view.getId();
            if (id == R.id.emailParentLayout) {
                fragmentTrialEndBinding.meetingParentLayout.setEnabled(false);
                new Handler().postDelayed(() -> fragmentTrialEndBinding.meetingParentLayout.setEnabled(true), 750);
                sendEmail();
            } else if (id == R.id.meetingParentLayout) {
                fragmentTrialEndBinding.emailParentLayout.setEnabled(false);
                new Handler().postDelayed(() -> fragmentTrialEndBinding.emailParentLayout.setEnabled(true), 750);
                openExternalBrowser();
            }
        }
        new Handler().postDelayed(() -> view.setEnabled(true), TimeConstants.SYNCHRONIZED_CONSTANT);
    }

    /**
     * to open email app to send email to specified email address
     */
    private void sendEmail() {
        String mailTo = getResources().getString(R.string.sales_email);
        Intent email_intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", mailTo, null));
        try {
            startActivity(Intent.createChooser(email_intent, getResources().getString(R.string.send_email_title)));
        } catch (Exception e) {
            Toast.makeText(SingletonData.getInstance().getContext(), R.string.went_wrong, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * to open specified link
     * to book a meeting
     */
    private void openExternalBrowser() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.meeting_link)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(SingletonData.getInstance().getContext(), R.string.went_wrong, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * method to show exit Dialog on back press
     */
    protected void exitDialog() {
        try {
            Dialog dialog = new Dialog(SingletonData.getInstance().getContext());
            dialog.setContentView(R.layout.exit_dialog_box);
            dialog.setCancelable(false);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            TextView exitTitle = dialog.findViewById(R.id.title);
            TextView exitSubTitle = dialog.findViewById(R.id.sub_title);
            Button negativeButton = dialog.findViewById(R.id.reject);
            Button positiveButton = dialog.findViewById(R.id.accept);
            exitTitle.setText(getResources().getString(R.string.confirmation));
            exitSubTitle.setText(getResources().getString(R.string.exitTxt));
            exitTitle.setTextColor(appColors.getDarkTextColor());
            exitSubTitle.setTextColor(appColors.getLightTextColor());
            negativeButton.setTextColor(appColors.getDarkTextColor());
            positiveButton.setBackgroundTintList(ColorStateList.valueOf(appColors.getButtonBgColor()));
            positiveButton.setTextColor(appColors.getDialogButtonTextColor());
            negativeButton.setOnClickListener(view -> dialog.dismiss());
            positiveButton.setOnClickListener(view -> SingletonData.getInstance().getActivity().finish());
            dialog.show();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "TrialEndFragment/exitDialog");
        }
    }

}
