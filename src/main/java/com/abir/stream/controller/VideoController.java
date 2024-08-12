package com.abir.stream.controller;

import com.abir.stream.model.Video;
import com.abir.stream.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/video")
@CrossOrigin("*")
public class VideoController {

    @Autowired
    private  VideoService videoService;
    @Autowired
    private  ResourceLoader resourceLoader;



    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable String videoId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        try {
            System.out.println("Video Id : "+videoId);
            // Fetch the video metadata
            Video video = videoService.findById(videoId);

            if (video == null) {
                return ResponseEntity.notFound().build();
            }

            // Construct the path to the HLS playlist
            Path playlistPath = Paths.get(video.getFilePath());

            // Check if the playlist file exists
            if (!Files.exists(playlistPath)) {
                return ResponseEntity.notFound().build();
            }

            // Load the file as a resource
            Resource resource = new FileSystemResource(playlistPath);
            String contentType = "application/vnd.apple.mpegurl";
            long fileLength = Files.size(playlistPath);

            if (rangeHeader != null) {
                try {
                    // Handle range requests for seeking
                    String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                    long rangeStart = Long.parseLong(ranges[0]);
                    long rangeEnd = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileLength - 1;

                    // Validate range end
                    if (rangeEnd >= fileLength) {
                        rangeEnd = fileLength - 1;
                    }

                    // Validate range start
                    if (rangeStart > rangeEnd) {
                        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                                .build();
                    }

                    // Calculate content length
                    long contentLength = rangeEnd - rangeStart + 1;

                    // Prepare headers
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
                    headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
                    headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                    headers.add(HttpHeaders.PRAGMA, "no-cache");
                    headers.add(HttpHeaders.EXPIRES, "0");
                    headers.add(HttpHeaders.CONTENT_TYPE, contentType);

                    // Serve the partial content
                    InputStream inputStream = Files.newInputStream(playlistPath);
                    inputStream.skip(rangeStart);

                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                            .headers(headers)
                            .body(new InputStreamResource(inputStream));
                } catch (NumberFormatException e) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                            .build();
                }
            } else {
                // Serve the full content
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_TYPE, contentType);
                headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength));
                headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                headers.add(HttpHeaders.PRAGMA, "no-cache");
                headers.add(HttpHeaders.EXPIRES, "0");
                System.out.println(resource.toString());
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource);
            }

        } catch (IOException e) {
            // Handle IOException
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            // Handle other exceptions
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

//    @GetMapping("/stream/{videoId}")
//    public ResponseEntity<Resource> streamVideo(@PathVariable String videoId) {
//        try {
//            // Assuming videoService.getHlsPlaylistPath(videoId) returns the full path to the HLS playlist
//            Path videoPath = videoService.getHlsPlaylistPath(videoId);
//
//            // Ensure the path is valid and points to the correct file
//            Resource resource = new UrlResource(videoPath.toUri());
//
//            if (resource.exists() && resource.isReadable()) {
//                return ResponseEntity.ok()
//                        .header(HttpHeaders.CONTENT_TYPE, MediaType.valueOf("application/x-mpegURL").toString())
//                        .body(resource);
//            } else {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//            }
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }










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
        response.put("status",true);
        response.put("code",HttpStatus.OK.value());
        response.put("data",videos);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVideo(@PathVariable String id, @RequestBody Video video) throws Exception {
        List<Video> data = new ArrayList<>();
        data.add(videoService.updateVideoDetails(id, video));

        Map<String, Object> response = new HashMap<>();
        response.put("status",true);
        response.put("message","Video updated successfully..");
        response.put("code",HttpStatus.OK.value());
        response.put("data",data);

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
        response.put("status",true);
        response.put("message","Video Deleted Successfully");
        response.put("code",200);
        return ResponseEntity
                .status(200)
                .body(response);
    }

//    @GetMapping("/stream/range/{videoId}")
//    public ResponseEntity<Resource> streamVideoRange(
//            @PathVariable String videoId,
//            @RequestHeader(value = "Range", required = false) String range
//    ) {
//        try {
//            Video video = videoService.findById(videoId);
//            Path path = Paths.get(video.getFilePath());
//
//            if (!Files.exists(path)) {
//                return ResponseEntity.notFound().build();
//            }
//
//            Resource resource = new FileSystemResource(path);
//            String contentType = video.getContentType() != null ? video.getContentType() : "application/octet-stream";
//            long fileLength = Files.size(path);
//
//            if (range == null) {
//                return ResponseEntity.ok()
//                        .contentType(MediaType.parseMediaType(contentType))
//                        .body(resource);
//            }
//
//            long rangeStart;
//            long rangeEnd;
//
//            String[] ranges = range.replace("bytes=", "").split("-");
//            rangeStart = Long.parseLong(ranges[0]);
//            rangeEnd = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileLength - 1;
//
//            if (rangeEnd > fileLength - 1) {
//                rangeEnd = fileLength - 1;
//            }
//
//            if (rangeStart > rangeEnd) {
//                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
//                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
//                        .build();
//            }
//
//            InputStream inputStream = Files.newInputStream(path);
//            inputStream.skip(rangeStart);
//
//            long contentLength = rangeEnd - rangeStart + 1;
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
//            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
//            headers.add(HttpHeaders.PRAGMA, "no-cache");
//            headers.add(HttpHeaders.EXPIRES, "0");
//            headers.add(HttpHeaders.CONTENT_TYPE, "nosniff");
//            headers.setContentLength(contentLength);
//
//            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
//                    .headers(headers)
//                    .contentType(MediaType.parseMediaType(contentType))
//                    .body(new InputStreamResource(inputStream));
//        } catch (IOException ex) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
}
