package fr.agroscan.service;

import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Set;

@Service
public class ImageValidationService {

    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public ImageContent validate(String imageBase64, String imageMediaType, long maxBytes, String tooLargeMessage) {
        if (!ALLOWED_MEDIA_TYPES.contains(imageMediaType)) {
            throw new InvalidImageException("Seules les images JPEG, PNG et WebP sont acceptées");
        }
        byte[] bytes = decode(imageBase64, "Le contenu de l'image est invalide");
        if (bytes.length == 0 || bytes.length > maxBytes) {
            throw new InvalidImageException(tooLargeMessage);
        }
        validateSignature(bytes, imageMediaType);
        return new ImageContent(bytes, imageMediaType);
    }

    public byte[] validateJpeg(String imageBase64, long maxBytes, String invalidMessage, String tooLargeMessage) {
        byte[] bytes = decode(imageBase64, invalidMessage);
        if (bytes.length == 0 || bytes.length > maxBytes) {
            throw new InvalidImageException(tooLargeMessage);
        }
        validateSignature(bytes, "image/jpeg");
        return bytes;
    }

    private byte[] decode(String value, String errorMessage) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidImageException(errorMessage);
        }
    }

    private void validateSignature(byte[] bytes, String mediaType) {
        boolean valid = switch (mediaType) {
            case "image/jpeg" -> bytes.length > 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8;
            case "image/png" -> bytes.length > 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                    && bytes[2] == 0x4E && bytes[3] == 0x47;
            case "image/webp" -> bytes.length > 12 && bytes[0] == 'R' && bytes[1] == 'I'
                    && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
                    && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
        if (!valid) {
            throw new InvalidImageException("Le contenu ne correspond pas au format d'image annoncé");
        }
    }

    public record ImageContent(byte[] bytes, String mediaType) {
    }
}
