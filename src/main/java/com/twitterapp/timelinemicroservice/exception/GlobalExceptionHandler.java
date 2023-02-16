package com.twitterapp.timelinemicroservice.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.twitterapp.timelinemicroservice.exception.exceptions.AccessForbiddenException;
import com.twitterapp.timelinemicroservice.exception.exceptions.AuthorizationHeaderMissingException;
import com.twitterapp.timelinemicroservice.exception.exceptions.GenericException;
import com.twitterapp.timelinemicroservice.exception.exceptions.PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.time.LocalDateTime;


@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = { AuthorizationHeaderMissingException.class })
    public ResponseEntity<ErrorObject> handleAuthorizationHeaderMissingException(AuthorizationHeaderMissingException ex, WebRequest request) {
        String errorMessage = "Missing authorization header!";
        ErrorObject errorObject = new ErrorObject(errorMessage, HttpStatus.UNAUTHORIZED, LocalDateTime.now());
        return new ResponseEntity<ErrorObject>(errorObject, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = { AccessForbiddenException.class })
    public ResponseEntity<ErrorObject> handleAccessForbiddenException(AccessForbiddenException ex, WebRequest request) {
        String errorMessage = "You don't have the proper roles to perform this operation!";
        ErrorObject errorObject = new ErrorObject(errorMessage, HttpStatus.FORBIDDEN, LocalDateTime.now());
        return new ResponseEntity<ErrorObject>(errorObject, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(value = { PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException.class })
    public ResponseEntity<ErrorObject> handlePassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException(PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException ex, WebRequest request) {
        String errorMessage = "You can't retrieve a page that is not the first one as a passive user!";
        ErrorObject errorObject = new ErrorObject(errorMessage, HttpStatus.NOT_ACCEPTABLE, LocalDateTime.now());
        return new ResponseEntity<ErrorObject>(errorObject, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(value = { GenericException.class })
    public ResponseEntity<ErrorObject> handleGenericException(GenericException ex, WebRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

            String exceptionMessage = ex.getMessage();
            ErrorObject errorObject = mapper.readValue(exceptionMessage, ErrorObject.class);
            return new ResponseEntity<ErrorObject>(errorObject, errorObject.getStatus());
        } catch (JsonMappingException e) {
            ErrorObject errorObject = new ErrorObject("Error while mapping json!", HttpStatus.INTERNAL_SERVER_ERROR, LocalDateTime.now());
            return new ResponseEntity<ErrorObject>(errorObject, errorObject.getStatus());
        } catch (JsonProcessingException e) {
            ErrorObject errorObject = new ErrorObject("Error while processing json!", HttpStatus.INTERNAL_SERVER_ERROR, LocalDateTime.now());
            return new ResponseEntity<ErrorObject>(errorObject, errorObject.getStatus());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
