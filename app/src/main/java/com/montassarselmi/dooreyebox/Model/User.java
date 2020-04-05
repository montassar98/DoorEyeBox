package com.montassarselmi.dooreyebox.Model;

public class User {

    private String fullName;
    private String phoneNumber;
    private String email;
    private String boxId;
    private String status;
    private String profileImage;

    public User() {
    }

    public User(String fullName, String phoneNumber, String email, String boxId ,String status) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.boxId = boxId;
        this.status = status;
    }
    public User(String profileImage, String fullName, String email){
        this.profileImage = profileImage;
        this.fullName = fullName;
        this.email = email;
    }

    public User(String fullName, String email){
        this.fullName = fullName;
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBoxId() {
        return boxId;
    }

    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
