package com.twitterapp.timelinemicroservice.service;


import com.twitterapp.extrastructures.UserTypeEnum;
import com.twitterapp.timelinemicroservice.entity.Tweet;
import com.twitterapp.timelinemicroservice.exception.exceptions.GenericException;
import com.twitterapp.timelinemicroservice.exception.exceptions.PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimelineService {

    @Autowired
    private RestTemplate restTemplate;

    public List<Tweet> getUserTimeline(String jws, Integer userId, Integer pageNumber, Integer pageSize) throws GenericException {
        return getTweetsPageFromDatabaseForUserTimeline(jws, userId, pageNumber, pageSize);
    }

    public List<Tweet> getHomeTimeline(String jws, Integer userId, UserTypeEnum userType, Integer pageNumber, Integer pageSize) throws PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException, GenericException {
        if(userType == UserTypeEnum.ACTIVE || userType == UserTypeEnum.LIVE) {
            // Luam din microserviciul de Fanout (daca pagina dorita depaseste limita cache-ului, luam din db)
            // Deci va trebui sa verificam in catch-ul requestului daca se arunca exceptia de depasire a cache-ului din fanout
            try {
                return getTweetsFromCacheForHomeTimeline(jws, userId, pageNumber, pageSize);
            } catch (HttpClientErrorException exception) {
                HttpStatus statusCode = exception.getStatusCode();
                if(statusCode == HttpStatus.NOT_FOUND) {
                    // we try to take the tweets from the main database
                    try {
                        return getTweetsPageFromDatabaseForHomeTimeline(jws, userId, pageNumber, pageSize);
                    } catch (HttpClientErrorException newException) {
                        String exceptionMessage = newException.getResponseBodyAsString();
                        throw new GenericException(exceptionMessage);
                    }
                }
            } finally {
                if(userType == UserTypeEnum.ACTIVE) {
                    changeUserType(jws, userId, UserTypeEnum.LIVE);
                }
            }
        } else if (userType == UserTypeEnum.PASSIVE) {
            // Luam din DB prima pagina si actualizam cache-ul
            if(pageNumber != 0) throw new PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException();

            try {
                List<Tweet> firstBulkOfTweetsFromDatabase = getFirstBulkOfTweetsFromDatabaseForHomeTimeline(jws, userId);
                addTweetsToCacheListForUser(jws, userId, firstBulkOfTweetsFromDatabase);
                int endIndex = firstBulkOfTweetsFromDatabase.size() < pageSize ? (firstBulkOfTweetsFromDatabase.size() - 1) : (pageSize - 1);
                return firstBulkOfTweetsFromDatabase.subList(0, endIndex + 1); // practic e prima pagina din cache-ul tocmai actualizat
            } catch (HttpClientErrorException exception) {
                String exceptionMessage = exception.getResponseBodyAsString();
                throw new GenericException(exceptionMessage);
            } finally {
                changeUserType(jws, userId, UserTypeEnum.LIVE);
            }
        }

        return new ArrayList<>();
    }

    private void changeUserType(String jws, Integer userId, UserTypeEnum newUserType) throws GenericException {
        String url = "https://localhost:8081/api/users/" + userId.toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        Map<String, UserTypeEnum> requestBody = new HashMap<>();
        requestBody.put("userType", newUserType);
        HttpEntity<Map<String, UserTypeEnum>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PATCH, entity, String.class);
        } catch (HttpClientErrorException exception) {
            String exceptionMessage = exception.getResponseBodyAsString();
            throw new GenericException(exceptionMessage);
        }

        //TODO - aici ar trebui generat un eveniment in coada de mesaje pe care ar trebui s-o asculte clientul web (cand se schimba tipul, trebuie regenerat jws-ul in clientul web)
    }

    public List<Tweet> getTweetsPageFromDatabaseForUserTimeline(String jws, Integer userId, Integer pageNumber, Integer pageSize) throws GenericException {
        String url = "https://localhost:8083/api/tweets/" + userId.toString() + "/forUserTimeline?pageNumber={pageNumber}&pageSize={pageSize}";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        Map<String, Integer> requestParameters = new HashMap<>();
        requestParameters.put("pageNumber", pageNumber);
        requestParameters.put("pageSize", pageSize);

        try {
            ResponseEntity<List<Tweet>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Tweet>>(){}, requestParameters);
            List<Tweet> tweetsForHomeTimeline = response.getBody();
            return tweetsForHomeTimeline;
        } catch (HttpClientErrorException exception) {
            String exceptionMessage = exception.getResponseBodyAsString();
            throw new GenericException(exceptionMessage);
        }

    }

    public List<Tweet> getTweetsFromCacheForHomeTimeline(String jws, Integer userId, Integer pageNumber, Integer pageSize) {
        String url = "https://localhost:8084/api/fanout/" + userId.toString() + "/?pageNumber={pageNumber}&pageSize={pageSize}";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<List<Tweet>> entity = new HttpEntity<>(headers);

        Map<String, Integer> requestParameters = new HashMap<>();
        requestParameters.put("pageNumber", pageNumber);
        requestParameters.put("pageSize", pageSize);

        ResponseEntity<List<Tweet>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Tweet>>(){}, requestParameters);
        List<Tweet> tweetsForHomeTimeline = response.getBody();
        return tweetsForHomeTimeline;
    }

    public List<Tweet> getTweetsPageFromDatabaseForHomeTimeline(String jws, Integer userId, Integer pageNumber, Integer pageSize) {
        String url = "https://localhost:8083/api/tweets/" + userId.toString() + "/forHomeTimeline?pageNumber={pageNumber}&pageSize={pageSize}";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        Map<String, Integer> requestParameters = new HashMap<>();
        requestParameters.put("pageNumber", pageNumber);
        requestParameters.put("pageSize", pageSize);

        ResponseEntity<List<Tweet>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Tweet>>(){}, requestParameters);
        List<Tweet> tweetsForHomeTimeline = response.getBody();
        return tweetsForHomeTimeline;
    }

    public List<Tweet> getFirstBulkOfTweetsFromDatabaseForHomeTimeline(String jws, Integer userId) {
        String url = "https://localhost:8083/api/tweets/" + userId.toString() + "/forHomeTimeline/firstBulk";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Tweet>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Tweet>>(){});
        List<Tweet> tweetsForHomeTimeline = response.getBody();
        return tweetsForHomeTimeline;
    }

    public void addTweetsToCacheListForUser(String jws, Integer userId, List<Tweet> tweets) throws GenericException {
        String url = "https://localhost:8084/api/fanout/" + userId.toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<List<Tweet>> entity = new HttpEntity<>(tweets, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        } catch (HttpClientErrorException exception) {
            String exceptionMessage = exception.getResponseBodyAsString();
            throw new GenericException(exceptionMessage);
        }
    }

}
