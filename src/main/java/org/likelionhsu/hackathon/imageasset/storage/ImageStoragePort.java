package org.likelionhsu.hackathon.imageasset.storage;

public interface ImageStoragePort {

    StoredImage upload(ImageStorageUploadRequest request);

    void delete(String publicId);
}
