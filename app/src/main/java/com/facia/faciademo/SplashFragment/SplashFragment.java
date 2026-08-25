package com.facia.faciademo.SplashFragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.facia.faciademo.ErrorLogs;
import com.facia.faciademo.MainSetAndGetData;
import com.facia.faciademo.R;
import com.facia.faciademo.databinding.FragmentSplashBinding;

public class SplashFragment extends Fragment {
    FragmentSplashBinding fragmentSplashBinding;

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentSplashBinding = FragmentSplashBinding.inflate(inflater, container, false);
        timerToDismissSplashScreen();
        return fragmentSplashBinding.getRoot();
    }

    /**
     * Method to dismiss splash fragment after required time
     */
    private void timerToDismissSplashScreen(){
        try{
            new CountDownTimer(1000, 500) {
                @Override
                public void onTick(long l) {}
                @Override
                public void onFinish() {
                    try {
                        if (!MainSetAndGetData.getInstance().isSplashGone()) {
                            MainSetAndGetData.getInstance().setSplashGone(true);
                        }
                    }catch (Exception e){
                        ErrorLogs.exceptionReport(e, "SplashFragment/onCreateView/CountDownTimer");
                    }
                }
            }.start();
        }catch (Exception e){
            ErrorLogs.exceptionReport(e, "SplashFragment/timerToDismissSplashScreen");
        }
    }
}
