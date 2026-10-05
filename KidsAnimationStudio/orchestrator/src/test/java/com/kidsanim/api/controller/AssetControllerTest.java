package com.kidsanim.api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TempDir
    Path tempDir;

    @Test
    void viewAssetReturns200WithFile() throws Exception {
        Path testFile = tempDir.resolve("sample.png");
        Files.write(testFile, new byte[]{0x1, 0x2, 0x3});

        mockMvc.perform(get("/api/v1/assets/view")
                        .param("path", testFile.toAbsolutePath().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void viewAssetReturns404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/assets/view")
                        .param("path", "/path/non/existent/file.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void viewAssetReturns400WhenPathBlank() throws Exception {
        mockMvc.perform(get("/api/v1/assets/view")
                        .param("path", "   "))
                .andExpect(status().isBadRequest());
    }
}
