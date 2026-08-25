
package com.facia.faciasdk.ApiModels.CheckSimilarity;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class Data {

    @SerializedName("reference_id")
    @Expose
    private String referenceId;
    @SerializedName("similarity_score")
    @Expose
    private Double similarityScore;
    @SerializedName("similarity_status")
    @Expose
    private Integer similarityStatus;

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public Integer getSimilarityStatus() {
        return similarityStatus;
    }

    public void setSimilarityStatus(Integer similarityStatus) {
        this.similarityStatus = similarityStatus;
    }
}
