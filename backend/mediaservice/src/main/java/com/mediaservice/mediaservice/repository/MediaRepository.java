package com.mediaservice.mediaservice.repository;
import com.mediaservice.mediaservice.models.Media;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MediaRepository extends MongoRepository<Media, String> {
    public void deleteByImagePath(String imagepath);
}