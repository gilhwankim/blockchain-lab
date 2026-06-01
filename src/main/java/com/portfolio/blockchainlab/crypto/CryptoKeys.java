package com.portfolio.blockchainlab.crypto;

import com.portfolio.blockchainlab.core.Hash;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class CryptoKeys {
    private CryptoKeys() {
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to generate key pair", e);
        }
    }

    public static String publicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static String addressFromPublicKey(String publicKey) {
        return Hash.sha256(publicKey).substring(0, 40);
    }

    public static String sign(PrivateKey privateKey, String payload) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to sign payload", e);
        }
    }

    public static boolean verify(String publicKey, String payload, String signatureValue) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(decodePublicKey(publicKey));
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureValue));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    private static PublicKey decodePublicKey(String publicKey) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(publicKey);
        return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(bytes));
    }
}
