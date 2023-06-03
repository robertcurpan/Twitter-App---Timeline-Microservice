package com.twitterapp.timelinemicroservice.controller;


import com.twitterapp.timelinemicroservice.dto.UserAndRolesDto;
import com.twitterapp.timelinemicroservice.entity.Tweet;
import com.twitterapp.timelinemicroservice.entity.UserInfo;
import com.twitterapp.timelinemicroservice.exception.exceptions.AccessForbiddenException;
import com.twitterapp.timelinemicroservice.exception.exceptions.AuthorizationHeaderMissingException;
import com.twitterapp.timelinemicroservice.exception.exceptions.GenericException;
import com.twitterapp.timelinemicroservice.exception.exceptions.PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException;
import com.twitterapp.timelinemicroservice.extrastructures.RolesEnum;
import com.twitterapp.timelinemicroservice.service.TimelineService;
import com.twitterapp.timelinemicroservice.util.AuthorizationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/timeline")
@CrossOrigin("http://localhost:80")
public class TimelineController {

    @Autowired
    private TimelineService timelineService;
    @Autowired
    private AuthorizationUtil authorizationUtil;


    @GetMapping("/{userId}/userTimeline")
    public ResponseEntity<List<Tweet>> getUserTimeline(HttpServletRequest request, @PathVariable Integer userId,
                                                      @RequestParam Integer pageNumber, @RequestParam Integer pageSize) throws AuthorizationHeaderMissingException, GenericException, AccessForbiddenException {
        String jws = authorizationUtil.getJwsFromRequest(request);
        UserAndRolesDto userAndRolesDto = authorizationUtil.validateJws(jws);
        if(!(authorizationUtil.checkRoleAuthorization(userAndRolesDto.getUserRoles(), RolesEnum.READER) && Objects.equals(userAndRolesDto.getUserId(), userId))) {
            throw new AccessForbiddenException();
        }

        List<Tweet> userTimeline = timelineService.getUserTimeline(jws, userId, pageNumber, pageSize);
        return new ResponseEntity<>(userTimeline, HttpStatus.OK);
    }

    @GetMapping("/{userId}/homeTimeline")
    public ResponseEntity<List<Tweet>> getHomeTimeline(HttpServletRequest request, @PathVariable Integer userId,
                                                       @RequestParam Integer pageNumber, @RequestParam Integer pageSize,
                                                       @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime timestamp, @RequestParam String lastDocumentSeenId) throws AuthorizationHeaderMissingException, GenericException, AccessForbiddenException, PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException {
        String jws = authorizationUtil.getJwsFromRequest(request);
        UserAndRolesDto userAndRolesDto = authorizationUtil.validateJws(jws);
        if(!(authorizationUtil.checkRoleAuthorization(userAndRolesDto.getUserRoles(), RolesEnum.READER) && Objects.equals(userAndRolesDto.getUserId(), userId))) {
            throw new AccessForbiddenException();
        }

        List<Tweet> homeTimeline = timelineService.getHomeTimeline(jws, userId, timestamp, lastDocumentSeenId, pageNumber, pageSize);
        return new ResponseEntity<>(homeTimeline, HttpStatus.OK);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<String> addUserInfo(@PathVariable Integer userId) {
        timelineService.addUserInfo(userId);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserInfo> getUserInfo(@PathVariable Integer userId) {
        return new ResponseEntity<>(timelineService.getUserInfo(userId), HttpStatus.OK);
    }
}
