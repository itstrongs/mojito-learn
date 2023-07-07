package com.mojito.learn.util;

/**
 * @author liufq
 * @since 2023-07-07 10:01:44
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
