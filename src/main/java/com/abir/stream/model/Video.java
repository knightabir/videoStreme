package com.abir.stream.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    @Id
    private String id;
    private String title;
    private String description;
    private String contentType;
    private String filePath;
    private boolean active;
    private boolean featured;
    private Date createdAt;
    private Date updatedAt;
}
