package com.nineleaps.leaps.service.implementation;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.nineleaps.leaps.service.StorageServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

import static com.nineleaps.leaps.LeapsApplication.NGROK;


@Service
@Slf4j
@Transactional
public class StorageServiceImpl implements StorageServiceInterface {

    String baseUrl = NGROK;

    @Value("${application.bucket.name}")
    String bucketName;

    @Autowired
    AmazonS3 s3Client;

    // private method to determine Content type
    String determineContentType(String fileName) {
        String contentType;
        if (fileName.endsWith(".pdf")) {
            contentType = MediaType.APPLICATION_PDF_VALUE;
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        } else if (fileName.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType;
    }


    // private method to convert multipart file to file
    File convertMultiPartFileToFile(MultipartFile file) {
        File convertedFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            byte[] fileBytes = file.getBytes();
            fos.write(fileBytes);
        } catch (IOException e) {
            log.error("Error converting multipartFile to file", e);
        }
        return convertedFile;
    }



    // upload file to s3 cloud AWS storage
    public String uploadFile(MultipartFile file) {
        File fileObj = convertMultiPartFileToFile(file);
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        s3Client.putObject(new PutObjectRequest(bucketName, fileName, fileObj));
        try {
            Files.delete(fileObj.toPath());
        } catch (IOException e) {
            log.error(String.valueOf(e));
        }
        return UriComponentsBuilder.fromHttpUrl(baseUrl).path("/api/v1/file/view/").path(fileName).toUriString();
    }

    // download the image from s3
    public byte[] downloadFile(String fileName) {
        S3Object s3Object = s3Client.getObject(bucketName, fileName);
        S3ObjectInputStream inputStream = s3Object.getObjectContent();

        try {
            return IOUtils.toByteArray(inputStream);

        } catch (IOException e) {
            log.error(String.valueOf(e));
        }
        return new byte[0];
    }

    //delete the file from s3
    public String deleteFile(String fileName) {
        s3Client.deleteObject(bucketName, fileName);
        return fileName + " removed ...";
    }


    //view the file from s3
    public void viewFile(String fileName, HttpServletResponse response) {
        try {
            S3Object s3Object = s3Client.getObject(bucketName, fileName);
            S3ObjectInputStream inputStream = s3Object.getObjectContent();

            // Set the response headers
            String contentType = determineContentType(fileName);
            response.setHeader(HttpHeaders.CONTENT_TYPE, contentType);


            OutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.flush();
        } catch (IOException e) {
            log.error("Amazon S3 Network Error");
        }
    }
}

