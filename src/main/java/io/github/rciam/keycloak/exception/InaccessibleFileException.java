package io.github.rciam.keycloak.exception;

public class InaccessibleFileException extends Exception {
    public InaccessibleFileException(String errorMessage) {
        super(errorMessage);
    }
}
