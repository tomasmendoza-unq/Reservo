package com.reservo.service;

import com.reservo.modelo.property.ReservoImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {
    ReservoImage saveImage(MultipartFile file);
    List<ReservoImage> saveImages(List<MultipartFile> files);
    void deleteImage(String publicId);
    void deleteImages(List<ReservoImage> images);
}
