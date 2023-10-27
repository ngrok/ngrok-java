package com.ngrok;

import java.io.IOException;

public class NgrokException extends IOException {
    private final String code;
    private final String details;

    public NgrokException(String code, String details) {
        super(String.format("%s\n\n%s", code, details));
        this.code = code;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public String getDetails() {
        return details;
    }
}
