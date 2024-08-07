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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    @Value("${files.video}")
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
        return videoRepository.findById(id).orElseThrow(() -> new Exception("Video not found with this id."));
    }

    @Override
    public void saveVideo(Video video, MultipartFile file) throws IOException {
        try {
            // Get the original filename and content type
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            // Generate a UUID for the file and determine the new filename
            String uuid = UUID.randomUUID().toString();
            String extension = filename != null ? filename.substring(filename.lastIndexOf('.')) : "";
            String newFilename = uuid + extension;

            // Clean and define the folder path
            String cleanFolder = StringUtils.cleanPath(DIR);

            // Create the target directory if it does not exist
            Path directoryPath = Paths.get(cleanFolder);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Define the full path for the file
            Path filePath = directoryPath.resolve(newFilename);

            // Log information
            logger.info("Content Type: " + contentType);
            logger.info("File Path: " + filePath.toString());

            // Copy the file to the target location
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            // Set the video metadata
            video.setContentType(contentType);
            video.setFilePath(filePath.toString());
            video.setCreatedAt(Date.from(Instant.now()));

            // Save the video metadata
            videoRepository.save(video);

        } catch (IOException e) {
            logger.error("Error occurred while saving video: ", e);
        }
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
