package com.reservo.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.reservo.modelo.property.ReservoImage;
import com.reservo.persistencia.DAO.inmueble.ImageDAO;
import com.reservo.service.ImageService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ImageServiceImpl implements ImageService {

    private final Cloudinary cloudinary;
    private final ImageDAO imageDAO;

    public ImageServiceImpl(Cloudinary cloudinary, ImageDAO imageDAO) {
        this.cloudinary = cloudinary;
        this.imageDAO = imageDAO;
    }

    @Override
    public ReservoImage saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap());
            ReservoImage image = new ReservoImage(
                    (String) uploadResult.get("public_id"),
                    (String) uploadResult.get("secure_url")
            );
            return imageDAO.save(image);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen", e);
        }
    }

    @Override
    public List<ReservoImage> saveImages(List<MultipartFile> files) {
        List<ReservoImage> reservoImages = new ArrayList<>();
        
        if (files == null || files.isEmpty()) {
            return reservoImages;
        }

        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap());
                    ReservoImage image = new ReservoImage(
                            (String) uploadResult.get("public_id"),
                            (String) uploadResult.get("secure_url")
                    );
                    imageDAO.save(image);
                    reservoImages.add(image);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar las imágenes", e);
        }

        return reservoImages;
    }

    @Override
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar la imagen", e);
        }
    }

    @Override
    public void deleteImages(List<ReservoImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }

        try {
            for (ReservoImage image : images) {
                cloudinary.uploader().destroy(
                        image.getPublicId(),
                        ObjectUtils.asMap("resource_type", "image")
                );
                imageDAO.delete(image);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar las imágenes", e);
        }
    }
}
