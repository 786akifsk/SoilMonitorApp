package com.example.myapplication;

public class PasswordResetRequest {
    private String email;
    private String redirect_to;

    public PasswordResetRequest(String email, String redirect_to) {
        this.email = email;
        this.redirect_to = redirect_to;
    }

//    Getters and setters (if needed by Gson)
//    public String getEmail() { return email; }
//    public String getRedirect_to() { return redirect_to; }
}