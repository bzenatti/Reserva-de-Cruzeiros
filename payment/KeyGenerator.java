package com.cruise.payment.config;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyGenerator {

    public static void main(String[] args) throws Exception {
        
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // 2048-bit RSA key
        KeyPair keyPair = keyGen.generateKeyPair();

        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        Path projectRoot = Paths.get("").toAbsolutePath();

        Path privateKeyPath = projectRoot.resolve("payment-private.key");
        Path publicKeyPath = projectRoot.resolve("payment-public.key");

        try (FileOutputStream privateKeyOut = new FileOutputStream(privateKeyPath.toFile());
             FileOutputStream publicKeyOut = new FileOutputStream(publicKeyPath.toFile())) {
            privateKeyOut.write(privateKey.getEncoded());
            publicKeyOut.write(publicKey.getEncoded());
        }

        System.out.println("Keys generated and saved to: ");
        System.out.println("Private Key: " + privateKeyPath.toAbsolutePath());
        System.out.println("Public Key: " + publicKeyPath.toAbsolutePath());
    }
}