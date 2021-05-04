package fr.kosmik.exception;

import java.io.IOException;

public class GithubAPIException extends RuntimeException{

    public static GithubAPIException fromIOException(IOException e){
        return new GithubAPIException("An exception occured while calling github", e);
    }

    public GithubAPIException(String message, Throwable cause) {
        super(message, cause);
    }
}
