package com.carland.carland_auth.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MsmTransactionException extends RuntimeException {
    public MsmTransactionException(String message) {
        super(message);
    }
}
