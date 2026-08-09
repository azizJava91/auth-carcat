package com.carland.carland_auth.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeakPinException extends RuntimeException {

    public WeakPinException(String message) {
        super(message);
    }
}
