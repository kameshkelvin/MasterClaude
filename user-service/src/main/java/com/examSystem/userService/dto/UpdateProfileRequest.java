package com.examSystem.userService.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新用户资料请求DTO
 * 
 * 基于系统设计文档中的API接口设计
 * 用于接收和验证用户资料更新请求参数
 */
public class UpdateProfileRequest {

    @Size(max = 50, message = "姓氏长度不能超过50字符")
    private String firstName;

    @Size(max = 50, message = "名字长度不能超过50字符")
    private String lastName;

    @Pattern(regexp = "^[1-9]\\d{10}$", message = "手机号格式不正确")
    private String phone;

    private String profile; // JSON格式的个人资料信息

    @Size(max = 200, message = "个人简介不能超过200字符")
    private String bio;

    @Size(max = 100, message = "职位不能超过100字符")
    private String position;

    @Size(max = 100, message = "公司/学校不能超过100字符")
    private String organization;

    @Size(max = 100, message = "地址不能超过100字符")
    private String location;

    @Pattern(regexp = "^https?://.*", message = "网站地址格式不正确")
    private String website;

    // 默认构造函数
    public UpdateProfileRequest() {}

    // 构造函数
    public UpdateProfileRequest(String firstName, String lastName, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    @Override
    public String toString() {
        return "UpdateProfileRequest{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", bio='" + bio + '\'' +
                ", position='" + position + '\'' +
                ", organization='" + organization + '\'' +
                ", location='" + location + '\'' +
                ", website='" + website + '\'' +
                '}';
    }
}