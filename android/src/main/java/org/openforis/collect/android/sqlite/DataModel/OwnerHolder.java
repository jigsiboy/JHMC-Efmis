package org.openforis.collect.android.sqlite.DataModel;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OwnerHolder {

    @SerializedName("enabled")
    private Boolean enabled;
    @SerializedName("id")
    private int id;
    @SerializedName("username")
    private String username;
    @SerializedName("password")
    private String password;
    @SerializedName("rawPassword")
    private String rawPassword;
    @SerializedName("roles")
    private List<String> roles;

    public Boolean getEnabled() {
        return enabled;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRawPassword() {
        return rawPassword;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        return "OwnerHolder{" +
                "enabled=" + enabled +
                ", id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", rawPassword='" + rawPassword + '\'' +
                ", roles=" + roles +
                '}';
    }
}
