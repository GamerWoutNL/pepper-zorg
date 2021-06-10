package com.pepper.backend.services.messaging;

import com.google.common.hash.Hashing;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MessageEncryptorService {

    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // must be one of {128, 120, 112, 104, 96}
    private static final int IV_LENGTH_BYTE = 12;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    public String encrypt(String text, String password) throws GeneralSecurityException {
        byte[] iv = getRandomNonce(IV_LENGTH_BYTE);

        MessageDigest keyDigest = MessageDigest.getInstance("SHA-256");
        byte[] keyHash = keyDigest.digest(password.getBytes(UTF_8));
        SecretKey secretKey = new SecretKeySpec(keyHash, "AES");

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] cipherText = cipher.doFinal(text.getBytes(UTF_8));

        byte[] cipherTextWithIv = ByteBuffer.allocate(iv.length + cipherText.length)
                .put(iv)
                .put(cipherText)
                .array();

        return Base64.getEncoder().encodeToString(cipherTextWithIv);
    }

    public String decrypt(String text, String password) throws GeneralSecurityException {
        byte[] decode = Base64.getDecoder().decode(text.getBytes(UTF_8));

        ByteBuffer bb = ByteBuffer.wrap(decode);

        byte[] iv = new byte[IV_LENGTH_BYTE];
        bb.get(iv);

        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);

        MessageDigest keyDigest = MessageDigest.getInstance("SHA-256");
        byte[] keyHash = keyDigest.digest(password.getBytes(UTF_8));
        SecretKey secretKey = new SecretKeySpec(keyHash, "AES");

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] plainText = cipher.doFinal(cipherText);

        return new String(plainText, UTF_8);
    }

    public static byte[] getRandomNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    public String hash(String text) {
        return Hashing.sha256().hashString(text, StandardCharsets.UTF_8).toString();
    }

}
