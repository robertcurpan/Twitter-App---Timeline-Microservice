package com.twitterapp.timelinemicroservice.repository;


import com.twitterapp.timelinemicroservice.entity.EditedTweet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RedisTweetsRepository {

    private static final String HASH_KEY_DELETED_TWEETS = "DeletedTweets";
    private static final String HASH_KEY_EDITED_TWEETS = "EditedTweets";

    @Autowired
    private HashOperations<String, String, String> deletedTweetsHash;
    @Autowired
    private HashOperations<String, String, EditedTweet> editedTweetsHash;


    public boolean isTweetDeleted(String tweetId) {
        String tweetIdValue = deletedTweetsHash.get(HASH_KEY_DELETED_TWEETS, tweetId);
        return tweetIdValue != null;
    }

    public boolean isTweetEdited(String tweetId) {
        EditedTweet editedTweet = editedTweetsHash.get(HASH_KEY_EDITED_TWEETS, tweetId);
        return editedTweet != null;
    }

    public String getNewTextForEditedTweet(String tweetId) {
        return editedTweetsHash.get(HASH_KEY_EDITED_TWEETS, tweetId).getText();
    }

}
