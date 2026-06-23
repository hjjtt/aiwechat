package com.aiwechat.auth.model.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private String userId;
    private String nickname;
    private String avatarUrl;
    private boolean isNewUser;

    public static LoginResponse of(String token, String userId, String nickname, String avatarUrl, boolean isNewUser) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(userId);
        response.setNickname(nickname);
        response.setAvatarUrl(avatarUrl);
        response.setNewUser(isNewUser);
        return response;
    }
}
