package org.example.error;

public class GenericError {
    private String message;
    private int status;

    public GenericError(String message, int status) {
        this.message = message;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }
}
