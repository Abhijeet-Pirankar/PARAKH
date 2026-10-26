package com.recruitshield.exception;

public class DuplicateEmailException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Unable to complete registration with the provided details. Please verify your information or log in.";

    public DuplicateEmailException() {
        super(DEFAULT_MESSAGE);
    }

    public DuplicateEmailException(String message) {
        super(message);
    }
}
