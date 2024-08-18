package com.abir.stream.service.impl;

import com.abir.stream.model.Video;
import com.abir.stream.repository.VideoRepository;
import com.abir.stream.service.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VideoServiceImpl implements VideoService {

    Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);
    @Value(value = "${files.video}")
    String DIR;

    private final VideoRepository videoRepository;

    public VideoServiceImpl(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    public void init(){
        File file = new File(DIR);

        if (!file.exists()){
            file.mkdir();
            logger.info("Video Folder creating...");
        } else {
            logger.info("Video folder already created ...");
        }
    }

    @Override
    public List<Video> findAll() {
        return videoRepository.findAll();
    }

    @Override
    public Video findById(String id) throws Exception {
        return videoRepository.findById(id).orElseThrow(() -> new Exception("Video not found with this id." + id));
    }


    @Override
    public void saveVideo(Video video, MultipartFile file) throws IOException {
        // Define the base directory
        Path uploadDir = Paths.get(DIR);

        // Create the base directory if it doesn't exist
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Validate the MultipartFile
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        // Generate a unique filename and folder to avoid conflicts
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IllegalArgumentException("File name is invalid");
        }

        // Create a unique folder for the video
        String uniqueFolderName = UUID.randomUUID().toString();
        Path videoDir = uploadDir.resolve(uniqueFolderName);
        Files.createDirectories(videoDir);

        // Define the path for the original file
        Path originalFilePath = videoDir.resolve(originalFileName);

        // Save the file using Files.copy
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, originalFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Failed to save the file to: " + originalFilePath.toString(), e);
        }

        // Construct the FFmpeg command
        String indexPath = videoDir.resolve("index.m3u8").toString();
        String segmentFilePathPattern = videoDir.resolve("segment_%03d.ts").toString();

        String command = String.format("ffmpeg -i \"%s\" -codec:v libx264 -codec:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s\" \"%s\"",
                originalFilePath.toString(), segmentFilePathPattern, indexPath);

        // Execute the FFmpeg command
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Read the FFmpeg process output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // Print FFmpeg logs for debugging
            }
        } catch (IOException e) {
            throw new IOException("Error reading FFmpeg process output", e);
        }

        // Wait for the process to complete
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg process failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg process was interrupted", e);
        }

        // Delete the original MP4 video file
        try {
            Files.deleteIfExists(originalFilePath);
        }catch (IOException e){
            System.err.println("Failed to delete the original video file. "+ originalFilePath.toString());
        }

        // Save metadata to the database
        Video demo = new Video();
        demo.setFeatured(false);
        demo.setTitle(video.getTitle());
        demo.setFilePath(indexPath); // Store path to the playlist file
        demo.setContentType(file.getContentType());
        demo.setDescription(video.getDescription());

        videoRepository.save(demo);
    }




    @Override
    public Path getHlsPlaylistPath(String id){
        Video video = videoRepository.findById(id).orElseThrow(()-> new IllegalArgumentException("Video not found with this id. "+id));
        return Path.of(video.getFilePath() );
    }


    @Override
    public Video updateVideoDetails(String id, Video video) throws Exception {
        Video existingVideo = findById(id);
        existingVideo.setUpdatedAt(Date.from(Instant.now()));
        existingVideo.setActive(video.isActive());
        existingVideo.setTitle(video.getTitle());
        existingVideo.setDescription(video.getDescription());
        existingVideo.setFeatured(video.isFeatured());

        return videoRepository.save(existingVideo);
    }


    @Override
    public Video updateById(String id, Video video, MultipartFile file) throws Exception {
        Video existingVideo = findById(id);
        if (existingVideo != null) {
            // Delete the old video file
            String oldFilePath = existingVideo.getFilePath();
            if (oldFilePath != null && !oldFilePath.isEmpty()) {
                Path oldPath = Paths.get(oldFilePath);
                try {
                    Files.deleteIfExists(oldPath);
                } catch (IOException e) {
                    logger.error("Failed to delete old video file: " + e.getMessage());
                }
            }

            // Update the video details
            existingVideo.setUpdatedAt(Date.from(Instant.now()));
            existingVideo.setActive(video.isActive());
            existingVideo.setTitle(video.getTitle());
            existingVideo.setDescription(video.getDescription());
            existingVideo.setFeatured(video.isFeatured());

            // Save the new video file
            if (file != null && !file.isEmpty()) {
                String filename = file.getOriginalFilename();
                String contentType = file.getContentType();
                InputStream inputStream = file.getInputStream();

                // Generate a UUID for the file
                String uuid = UUID.randomUUID().toString();
                String extension = filename != null ? filename.substring(filename.lastIndexOf('.')) : "";
                String newFilename = uuid + extension;

                // Folder path create
                String cleanFolder = StringUtils.cleanPath(DIR);

                // Folder path
                Path newPath = Paths.get(cleanFolder, newFilename);

                logger.info(contentType);
                logger.info(newPath.toString());

                // Copy files to the folder
                Files.copy(inputStream, newPath, StandardCopyOption.REPLACE_EXISTING);

                // Update video details with new file info
                existingVideo.setContentType(contentType);
                existingVideo.setFilePath(newPath.toString());
            }

            // Save the updated video details
            return videoRepository.save(existingVideo);
        } else {
           throw new Exception("Video not found with this id.");
        }
    }

    @Override
    public void deleteById(String id) throws Exception {
        Video existingVideo = findById(id);
        if (existingVideo != null) {
            // Delete the video file if it exists
            String filePath = existingVideo.getFilePath();
            if (filePath != null && !filePath.isEmpty()) {
                Path path = Paths.get(filePath);
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    logger.error("Failed to delete video file: " + e.getMessage());
                }
            }

            // Delete the video record
            videoRepository.deleteById(id);
        }
    }

    @Override
    public List<Video> findByActiveTrue(){
        return videoRepository.findByActiveTrue();
    }

    @Override
    public List<Video> findByFeaturedTrue(){
        return videoRepository.findByFeaturedTrue();
    }
}
