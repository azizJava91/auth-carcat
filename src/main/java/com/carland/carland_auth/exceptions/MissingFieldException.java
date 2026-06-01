package com.carland.carland_auth.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MissingFieldException extends RuntimeException {

    public MissingFieldException(String message) {
        super(message);
    }
}

