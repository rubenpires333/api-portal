package com.api_portal.backend.modules.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${app.upload.avatars-dir:uploads/avatars}")
    private String avatarsDir;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public String uploadAvatar(MultipartFile file, UUID userId) throws IOException {
        // Validar tipo de arquivo
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Apenas imagens são permitidas");
        }
        
        // Validar tamanho (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Tamanho máximo de 5MB excedido");
        }
        
        // Criar diretório se não existir
        Path uploadPath = Paths.get(avatarsDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Gerar nome único para o arquivo
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String filename = userId.toString() + extension;
        Path filePath = uploadPath.resolve(filename);
        
        // Salvar arquivo
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Retornar URL pública
        return baseUrl + "/uploads/avatars/" + filename;
    }
    
    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return;
        }
        
        try {
            // Extrair nome do arquivo da URL
            String filename = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(avatarsDir).resolve(filename);
            
            // Deletar arquivo se existir
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.error("Erro ao deletar avatar: {}", e.getMessage());
        }
    }
}
