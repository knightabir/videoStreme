package com.abir.stream.service;

import com.abir.stream.model.Video;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface VideoService {
    List<Video> findAll();
    Video findById(String id) throws Exception;
    void saveVideo(Video video, MultipartFile file) throws IOException;

    Video updateVideoDetails(String id, Video video) throws Exception;

    Video updateById(String id, Video video, MultipartFile file) throws Exception;

    void deleteById(String id) throws Exception;
    List<Video> findByActiveTrue();
    List<Video> findByFeaturedTrue();
    Path getHlsPlaylistPath(String id);
}
