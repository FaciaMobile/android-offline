
package com.facia.faciasdk.ApiModels.Result;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class Data {

    @SerializedName("_id")
    @Expose
    private String id;
    @SerializedName("video_id")
    @Expose
    private String videoId;
    @SerializedName("merchant_id")
    @Expose
    private Integer merchantId;
    @SerializedName("reference_id")
    @Expose
    private String referenceId;
    @SerializedName("callback_url")
    @Expose
    private String callbackUrl;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("updated_at")
    @Expose
    private String updatedAt;
    @SerializedName("created_at")
    @Expose
    private String createdAt;
    @SerializedName("main_cluster_assign_at")
    @Expose
    private MainClusterAssignAt mainClusterAssignAt;
    @SerializedName("main_cluster_id")
    @Expose
    private String mainClusterId;
    @SerializedName("No_face")
    @Expose
    private Boolean noFace;
    @SerializedName("cluster_assign_at")
    @Expose
    private String clusterAssignAt;
    @SerializedName("cluster_id")
    @Expose
    private String clusterId;
    @SerializedName("face")
    @Expose
    private Boolean face;
    @SerializedName("is_request_original")
    @Expose
    private Integer isRequestOriginal;
    @SerializedName("ml_request_finalized_at")
    @Expose
    private String mlRequestFinalizedAt;
    @SerializedName("ml_request_status")
    @Expose
    private String mlRequestStatus;
    @SerializedName("decline_reason")
    @Expose
    private String declineReason;
    @SerializedName("face_match")
    @Expose
    private FaceMatch faceMatch;
    @SerializedName("quick_liveness_response")
    @Expose
    private Integer quickLivenessResponse;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public Integer getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Integer merchantId) {
        this.merchantId = merchantId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public MainClusterAssignAt getMainClusterAssignAt() {
        return mainClusterAssignAt;
    }

    public void setMainClusterAssignAt(MainClusterAssignAt mainClusterAssignAt) {
        this.mainClusterAssignAt = mainClusterAssignAt;
    }

    public String getMainClusterId() {
        return mainClusterId;
    }

    public void setMainClusterId(String mainClusterId) {
        this.mainClusterId = mainClusterId;
    }

    public Boolean getNoFace() {
        return noFace;
    }

    public void setNoFace(Boolean noFace) {
        this.noFace = noFace;
    }

    public String getClusterAssignAt() {
        return clusterAssignAt;
    }

    public void setClusterAssignAt(String clusterAssignAt) {
        this.clusterAssignAt = clusterAssignAt;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public Boolean getFace() {
        return face;
    }

    public void setFace(Boolean face) {
        this.face = face;
    }

    public Integer getIsRequestOriginal() {
        return isRequestOriginal;
    }

    public void setIsRequestOriginal(Integer isRequestOriginal) {
        this.isRequestOriginal = isRequestOriginal;
    }

    public String getMlRequestFinalizedAt() {
        return mlRequestFinalizedAt;
    }

    public void setMlRequestFinalizedAt(String mlRequestFinalizedAt) {
        this.mlRequestFinalizedAt = mlRequestFinalizedAt;
    }

    public String getMlRequestStatus() {
        return mlRequestStatus;
    }

    public void setMlRequestStatus(String mlRequestStatus) {
        this.mlRequestStatus = mlRequestStatus;
    }

    public FaceMatch getFaceMatch() {
        return faceMatch;
    }

    public void setFaceMatch(FaceMatch faceMatch) {
        this.faceMatch = faceMatch;
    }

    public Integer getQuickLivenessResponse() {
        return quickLivenessResponse;
    }

    public void setQuickLivenessResponse(Integer quickLivenessResponse) {
        this.quickLivenessResponse = quickLivenessResponse;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }
}
