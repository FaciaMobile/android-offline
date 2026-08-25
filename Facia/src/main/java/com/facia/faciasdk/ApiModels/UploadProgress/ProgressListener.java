package com.facia.faciasdk.ApiModels.UploadProgress;

public interface ProgressListener {
    void onProgress(long bytesWritten, long contentLength);
}
