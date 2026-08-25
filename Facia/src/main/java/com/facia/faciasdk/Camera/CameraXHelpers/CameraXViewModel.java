package com.facia.faciasdk.Camera.CameraXHelpers;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.facia.faciasdk.Logs.Webhooks;
import com.google.common.util.concurrent.ListenableFuture;

/** View model for interacting with CameraX. */
public final class CameraXViewModel extends AndroidViewModel {

  private MutableLiveData<ProcessCameraProvider> cameraProviderLiveData;

  /**
   * Create an instance which interacts with the camera service via the given application context.
   */
  public CameraXViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<ProcessCameraProvider> getProcessCameraProvider() {
    if (cameraProviderLiveData == null) {
      cameraProviderLiveData = new MutableLiveData<>();

      ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
          ProcessCameraProvider.getInstance(getApplication());
      cameraProviderFuture.addListener(() -> {
            try {
              cameraProviderLiveData.setValue(cameraProviderFuture.get());
            } catch (Exception e) {
                Webhooks.exceptionReport(e, "CameraScreen/CameraXViewModel/getProcessCameraProvider");
            }
          },
          ContextCompat.getMainExecutor(getApplication()));
    }

    return cameraProviderLiveData;
  }
}
