package org.openforis.collect.android.sqlite.DataModel;

public class UserData {

    String id;
    String email;
    String fullName;

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public UserData(String id, String email, String fullName) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
    }
}
