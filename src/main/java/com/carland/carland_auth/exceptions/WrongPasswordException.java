package com.carland.carland_auth.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException(String message){
        super(message);
    }

}
