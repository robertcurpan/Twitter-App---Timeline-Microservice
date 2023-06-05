package com.twitterapp.timelinemicroservice.util;

import com.twitterapp.timelinemicroservice.dto.UserAndRolesDto;
import com.twitterapp.timelinemicroservice.exception.exceptions.AuthorizationHeaderMissingException;
import com.twitterapp.timelinemicroservice.exception.exceptions.GenericException;
import com.twitterapp.timelinemicroservice.extrastructures.JwsObject;
import com.twitterapp.timelinemicroservice.extrastructures.RolesEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

@Component
public class AuthorizationUtil {

    @Autowired
    private RestTemplate restTemplate;

    public String getJwsFromRequest(HttpServletRequest request) throws AuthorizationHeaderMissingException {
        String authorizationHeader = request.getHeader("Authorization");
        if(authorizationHeader == null) throw new AuthorizationHeaderMissingException();
        String jws = authorizationHeader.substring(7);
        return jws;
    }

    public UserAndRolesDto validateJws(String jws) throws GenericException {
        JwsObject jwsObject = new JwsObject(jws);
        String url = "http://54.246.43.171:8081/api/auth/validateJws";
        HttpEntity<JwsObject> entity = new HttpEntity<>(jwsObject);

        try {
            ResponseEntity<UserAndRolesDto> response = restTemplate.exchange(url, HttpMethod.POST, entity, UserAndRolesDto.class);
            return response.getBody();
        } catch (HttpClientErrorException exception) {
            String exceptionMessage = exception.getResponseBodyAsString();
            throw new GenericException(exceptionMessage);
        }
    }

    public boolean checkRoleAuthorization(List<RolesEnum> roles, RolesEnum requiredRole) {
        for(RolesEnum rolesEnum : roles) {
            if(Objects.equals(rolesEnum.getRoleId(), requiredRole.getRoleId())) {
                return true;
            }
        }

        return false;
    }

}
