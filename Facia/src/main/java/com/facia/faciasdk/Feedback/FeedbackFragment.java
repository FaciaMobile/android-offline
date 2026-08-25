package com.facia.faciasdk.Feedback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.facia.faciasdk.Activity.Helpers.IntentHelper;
import com.facia.faciasdk.Activity.Helpers.RequestModel;
import com.facia.faciasdk.Greeting.GreetingFragment;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ApiConstants;
import com.facia.faciasdk.Utils.Constants.ThresholdConstants;
import com.facia.faciasdk.Utils.Constants.TimeConstants;
import com.facia.faciasdk.databinding.FragmentFeedbackBinding;

import java.util.HashMap;

public class FeedbackFragment extends Fragment implements View.OnClickListener {
    /**
     * method to handle screen's back press click
     */
    OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
        }
    };
    private RequestModel requestModel;
    private Boolean isUserSatisfied = true;
    private ApiHelper apiHelper;
    private final HashMap<String, String> requestResponseObj;
    private final String token;
    private FragmentFeedbackBinding fragmentFeedbackBinding;

    public FeedbackFragment(String token, HashMap<String, String> requestResponseObj) {
        this.token = token;
        this.requestResponseObj = requestResponseObj;
    }

    /**
     * method to check whether the email is valid or not
     */
    private static boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentFeedbackBinding = FragmentFeedbackBinding.inflate(inflater, container, false);
        initialization();
        return fragmentFeedbackBinding.getRoot();
    }

    /**
     * method to initialize instances
     * setting listeners, callbacks
     */
    private void initialization() {
        try {
            requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);
            feedbackIconListener();
            requireActivity().getOnBackPressedDispatcher().addCallback(this.requireActivity(), backPressCallBack);
            fragmentFeedbackBinding.continueBtn.setOnClickListener(this);
            apiHelper = new ApiHelper(fragmentFeedbackBinding, isUserSatisfied, token);
            fragmentFeedbackBinding.feedbackParentLayout.setOnClickListener(view -> hideKeyboard());
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "FeedbackFragment/initialization");
        }
    }

    /**
     * listener to handle radio buttons
     * to handle feedback ans (yes/no)
     */
    private void feedbackIconListener() {
        try {
            fragmentFeedbackBinding.satisfyIcon.setOnClickListener(view -> {
                isUserSatisfied = true;
                fragmentFeedbackBinding.satisfyIcon.setBackgroundResource(R.drawable.ic_satisfy_enabled);
                fragmentFeedbackBinding.noSatisfyIcon.setBackgroundResource(R.drawable.ic_no_satisfy_disabled);
                enableContBtn();
            });
            fragmentFeedbackBinding.noSatisfyIcon.setOnClickListener(view -> {
                isUserSatisfied = false;
                fragmentFeedbackBinding.satisfyIcon.setBackgroundResource(R.drawable.ic_satisfy_disabled);
                fragmentFeedbackBinding.noSatisfyIcon.setBackgroundResource(R.drawable.ic_no_satisfy_enabled);
                if (fragmentFeedbackBinding.suggestionEdtTxt.getText().toString().trim().length() < 3){
                    disableContBtn();
                }else {
                    enableContBtn();
                }
            });
            suggestionsEdtListener();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "FeedbackFragment/feedbackIconListener");
        }
    }

    /**
     * Method to hide keyboard forcefully
     */
    private void hideKeyboard() {
        try {
            if (SingletonData.getInstance().getActivity().getWindow().getDecorView().getRootView() != null) {
                InputMethodManager v = (InputMethodManager) SingletonData.getInstance().getActivity().getWindow()
                        .getDecorView().getRootView().getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                v.hideSoftInputFromWindow(SingletonData.getInstance().getActivity().getWindow().getDecorView()
                        .getRootView().getWindowToken(), 0);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "FeedbackFragment/hideKeyboard");
        }
    }

    /**
     * method to call Greeting Screen
     */
    private void navigateToGreetingScreen() {
        try {
            Fragment greetingFragment = new GreetingFragment(requestResponseObj);
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, greetingFragment, greetingFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "FeedbackFragment/navigateToGreetingScreen");
        }
    }

    @Override
    public void onClick(View view) {
        synchronized (view) {
            view.setEnabled(false);
            int id = view.getId();
            if (id == R.id.continueBtn) {
//                submitFeedback();
            }
        }
        new Handler().postDelayed(() -> view.setEnabled(true), TimeConstants.SYNCHRONIZED_CONSTANT);
    }

    /**
     * method to submit feedback on button click
     * and to navigate to greeting screen
     */
//    private void submitFeedback() {
//        try {
//            if (fragmentFeedbackBinding.suggestionEdtTxt.getText().toString().trim().length() <=
//                    ThresholdConstants.SUGGESTIONS_TEXT_MAX_LIMIT){
//                apiHelper.sendFeedbackJsonObject();
//                if (requestModel.getConfigObject().getBoolean("showGreetings")) {
//                    navigateToGreetingScreen();
//                }else {
//                    requestModel.getRequestListener().requestStatus(requestResponseObj);
//                    SingletonData.getInstance().getActivity().finish();
//                }
//            }else {
//                Toast.makeText(SingletonData.getInstance().getContext(), getString(R.string.suggestion_max_length_error_msg), Toast.LENGTH_SHORT).show();
//            }
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "FeedbackFragment/submitFeedback");
//        }
//    }

    /**
     * method to detect changes in edit text of type a task
     * to enable/disable submit button to create task
     */
    protected void suggestionsEdtListener() {
        try {
            fragmentFeedbackBinding.suggestionEdtTxt.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @SuppressLint("SetTextI18n")
                @Override
                public void afterTextChanged(Editable editable) {
                    try {
                        fragmentFeedbackBinding.textTaskEdtCount.setText(fragmentFeedbackBinding.suggestionEdtTxt.
                                getText().length() + "/" + ThresholdConstants.SUGGESTIONS_TEXT_MAX_LIMIT);
                        if (!isUserSatisfied){
                            if (fragmentFeedbackBinding.suggestionEdtTxt.getText().toString().trim().length() < 3){
                                disableContBtn();
                            }else {
                                enableContBtn();
                            }
                        } else {
                            enableContBtn();
                        }
                    } catch (Exception e) {
                        Webhooks.exceptionReport(e, "FeedbackFragment:textTaskListener/suggestionsEdtListener-Catch");
                    }
                }
            });
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "FeedbackFragment:suggestionsEdtListener-Catch");
        }
    }

    private void enableContBtn(){
        try{
            fragmentFeedbackBinding.continueBtn.setEnabled(true);
            fragmentFeedbackBinding.continueBtn.setClickable(true);
            fragmentFeedbackBinding.continueBtn.setAlpha(1);
        }catch (Exception e){
            Webhooks.exceptionReport(e, "FeedbackFragment:enableContBtn");
        }
    }
    private void disableContBtn(){
        try{
            fragmentFeedbackBinding.continueBtn.setAlpha(0.33f);
            fragmentFeedbackBinding.continueBtn.setEnabled(false);
            fragmentFeedbackBinding.continueBtn.setClickable(false);
        }catch (Exception e){
            Webhooks.exceptionReport(e, "FeedbackFragment:disableContBtn");
        }
    }
}
