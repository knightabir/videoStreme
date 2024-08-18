package com.abir.stream.controller;

import com.abir.stream.model.Video;
import com.abir.stream.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/video")
@CrossOrigin("*")
public class VideoController {

    @Autowired
    private VideoService videoService;
    @Autowired
    private ResourceLoader resourceLoader;


    @Value(value = "${files.video}")
    private String VIDEO_DIR;

    @GetMapping("/stream/{videoId}/index.m3u8")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable String videoId
    ) throws Exception {
        System.out.println(videoId);

        Video video = videoService.findById(videoId);

        String url = video.getFilePath();
        Path path = Path.of(url);
        System.out.println(path);

        if (!Files.exists(path)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl"
                )
                .body(resource);
    }


    @GetMapping("/stream/{videoId}/{segment}")
    public ResponseEntity<Resource> serveSegments(
            @PathVariable String videoId,
            @PathVariable String segment
    ) throws Exception {
        Video video = videoService.findById(videoId);

        String url = video.getFilePath();
        url = url.replace("index.m3u8", "");
        Path path = Path.of(url);
        System.out.println(path);
        System.out.println(segment);

        if (!Files.exists(path)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "video/mp2t"
                )
                .body(resource);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description
    ) throws IOException {
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        videoService.saveVideo(video, file);

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("code", HttpStatus.OK.value());
        response.put("message", "Video uploaded successfully.");

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> response = new HashMap<>();
        List<Video> videos = videoService.findAll();
        videos.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        if (videos.isEmpty()) {
            response.put("status", false);
            response.put("code", HttpStatus.NOT_FOUND.value());
            response.put("message", "Data not found.");
        } else {
            response.put("status", true);
            response.put("code", HttpStatus.OK.value());
            response.put("message", "Data found.");
            response.put("data", videos);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVideoById(@PathVariable String id) throws Exception {
        List<Video> videos = new ArrayList<>();
        videos.add(videoService.findById(id));
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("code", HttpStatus.OK.value());
        response.put("data", videos);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVideo(@PathVariable String id, @RequestBody Video video) throws Exception {
        List<Video> data = new ArrayList<>();
        data.add(videoService.updateVideoDetails(id, video));

        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("message", "Video updated successfully..");
        response.put("code", HttpStatus.OK.value());
        response.put("data", data);

        return ResponseEntity
                .status(HttpStatus.OK.value())
                .body(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Video>> findByActiveTrue() {
        List<Video> videos = videoService.findByActiveTrue();
        videos.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<Video>> findByFeaturedTrue() {
        List<Video> videos = videoService.findByFeaturedTrue();
        videos.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        return ResponseEntity.ok(videos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable String id) throws Exception {
        videoService.deleteById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("status", true);
        response.put("message", "Video Deleted Successfully");
        response.put("code", 200);
        return ResponseEntity
                .status(200)
                .body(response);
    }
}