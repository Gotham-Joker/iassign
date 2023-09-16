package com.github.authorization;

import com.github.core.ApiException;

public class AuthenticationContext {

    private static final ThreadLocal<Authentication> holder = new ThreadLocal<>();

    public static Authentication current() {
        Authentication authentication = holder.get();
        if (authentication == null) {
            throw new ApiException(401, "token is required");
        }
        return authentication;
    }

    public static UserDetails details() {
        return current().getDetails();
    }

    public static void setAuthentication(Authentication authentication) {
        holder.set(authentication);
    }

    public static void clearContext() {
        holder.remove();
    }

}
