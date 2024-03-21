package org.example.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;
import java.io.Serializable;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class MailMergeException extends Exception implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public MailMergeException(final String message) {
        super(message);
    }

    public MailMergeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
