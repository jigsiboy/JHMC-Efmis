package org.openforis.collect.android.sqlite.DataModel;

import com.google.gson.annotations.SerializedName;

public class RecordHolder {

    @SerializedName("newRecord")
    private String newRecord;
    @SerializedName("errors")
    private String errors;
    @SerializedName("skipped")
    private String skipped;
    @SerializedName("missing")
    private String missing;
    @SerializedName("missingErrors")
    private String missingErrors;
    @SerializedName("missingWarnings")
    private String missingWarnings;
    @SerializedName("warnings")
    private String warnings;
    @SerializedName("id")
    private String id;
    @SerializedName("owner")
    private OwnerHolder owner;

    @SerializedName("creationDate")
    private String creationDate;
    @SerializedName("modifiedDate")
    private String modifiedDate;
    @SerializedName("surveyId")
    private String surveyId;

    public String getNewRecord() {
        return newRecord;
    }

    public String getErrors() {
        return errors;
    }

    public String getSkipped() {
        return skipped;
    }

    public String getMissing() {
        return missing;
    }

    public String getMissingErrors() {
        return missingErrors;
    }

    public String getMissingWarnings() {
        return missingWarnings;
    }

    public String getWarnings() {
        return warnings;
    }

    public OwnerHolder getOwner() {
        return owner;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public String getSurveyId() {
        return surveyId;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "RecordHolder{" +
                "newRecord='" + newRecord + '\'' +
                ", errors='" + errors + '\'' +
                ", skipped='" + skipped + '\'' +
                ", missing='" + missing + '\'' +
                ", missingErrors='" + missingErrors + '\'' +
                ", missingWarnings='" + missingWarnings + '\'' +
                ", warnings='" + warnings + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
