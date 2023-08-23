package com.ngrok;

import java.io.IOException;

public class Error extends IOException {
    private String message;
    private String errorCode;

    public Error(String message, String errorCode) {
        super(String.format("%s: %s", errorCode, message));
        this.message = message;
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
