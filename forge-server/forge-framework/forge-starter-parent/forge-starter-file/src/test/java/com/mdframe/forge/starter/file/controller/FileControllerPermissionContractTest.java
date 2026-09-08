package com.mdframe.forge.starter.file.controller;

import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileControllerPermissionContractTest {

    @Test
    void fileControllerShouldNotIgnorePermissionAtClassLevel() {
        assertFalse(FileController.class.isAnnotationPresent(ApiPermissionIgnore.class));
    }

    @Test
    void loggedInFileApisShouldRemainPermissionIgnored() throws NoSuchMethodException {
        assertIgnored("upload", MultipartFile.class, String.class, String.class, String.class, Boolean.class);
        assertIgnored("download", String.class, HttpServletResponse.class);
        assertIgnored("getAccessUrl", String.class, Integer.class, HttpServletResponse.class);
        assertIgnored("delete", String.class);
        assertIgnored("initMultipartUpload", String.class, String.class, String.class, String.class);
        assertIgnored("uploadPart", String.class, Integer.class, MultipartFile.class, String.class);
        assertIgnored("completeMultipartUpload", String.class, List.class, String.class);
    }

    @Test
    void bucketApisShouldNotIgnorePermission() throws NoSuchMethodException {
        assertNotIgnored("createBucket", String.class, String.class);
        assertNotIgnored("deleteBucket", String.class, String.class);
        assertNotIgnored("bucketExists", String.class, String.class);
    }

    private void assertIgnored(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = FileController.class.getDeclaredMethod(methodName, parameterTypes);
        assertTrue(method.isAnnotationPresent(ApiPermissionIgnore.class), methodName);
    }

    private void assertNotIgnored(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = FileController.class.getDeclaredMethod(methodName, parameterTypes);
        assertFalse(method.isAnnotationPresent(ApiPermissionIgnore.class), methodName);
    }
}
