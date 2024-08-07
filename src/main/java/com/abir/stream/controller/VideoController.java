package com.abir.stream.controller;

import com.abir.stream.model.Video;
import com.abir.stream.service.VideoService;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/video")
@CrossOrigin("*")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestParam("file")MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description
            ) throws IOException {
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        videoService.saveVideo(video,file);

        Map<String, Object> response = new HashMap<>();
        response.put("status",true);
        response.put("code",HttpStatus.OK.value());
        response.put("message","Video uploaded successfully..");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAll(){
        Map<String, Object> response = new HashMap<>();
        List<Video> videos = videoService.findAll();
        videos.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        if (videos.isEmpty()){
            response.put("status", false);
            response.put("code",HttpStatus.NOT_FOUND.value());
            response.put("message","Data not found.");
        }else {
            response.put("status", true);
            response.put("code",200);
            response.put("message","Data found.");
            response.put("data",videos);
        }
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/active")
    public List<Video> findByActiveTrue(){
        List<Video> videos = videoService.findByActiveTrue();
        videos.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        return videos;
    }

    @GetMapping("/featured")
    public List<Video> findByFeaturedTrue() {
        List<Video> videos = videoService.findByFeaturedTrue();
        // Sort the videos by createdAt time in descending order
        videos.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        return videos;
    }

}
