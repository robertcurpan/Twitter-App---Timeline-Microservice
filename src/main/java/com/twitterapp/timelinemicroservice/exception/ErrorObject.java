package com.twitterapp.timelinemicroservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ErrorObject {

    private String message;
    private HttpStatus status;
    private LocalDateTime timestamp;
}