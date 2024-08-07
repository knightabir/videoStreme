package com.abir.stream.repository;

import com.abir.stream.model.Video;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface VideoRepository extends MongoRepository<Video, String> {
    List<Video> findByFeaturedTrue();
    List<Video> findByActiveTrue();

}
