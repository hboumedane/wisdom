package org.wisdom.crypto;

import com.google.common.base.Preconditions;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.felix.ipojo.annotations.*;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.api.crypto.Crypto;
import org.wisdom.api.crypto.Hash;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * An implementation of the crypto service.
 */
@Component
@Provides
@Instantiate(name = "crypto")
public class CryptoServiceSingleton implements Crypto {

    private final String secret;
    private String cipherAlgorithm = "AES/CBC/PKCS5Padding";

    @Property(name = "aes.key.size", value = "256")
    private int keySize;

    @Property(name = "aes.iterations", value = "20")
    private int iterationCount;

    @Property(value = "MD5")
    private Hash defaultHash;

    private Cipher cipher;

    public CryptoServiceSingleton(@Requires ApplicationConfiguration configuration) {
        this(configuration
                .getOrDie(ApplicationConfiguration.APPLICATION_SECRET), null, null, null, null);
    }

    public CryptoServiceSingleton(String secret, Hash defaultHash) {
        this(secret, defaultHash, null, null, null);
    }

    public CryptoServiceSingleton(String secret, Hash defaultHash, String cipherAlgorithm, Integer keySize, Integer iterationCount) {
        this.secret = secret;
        if (defaultHash != null) {
            this.defaultHash = defaultHash;
        }

        if (this.keySize == 0) {
            this.keySize = keySize;
        }

        if (this.iterationCount == 0) {
            this.iterationCount = iterationCount;
        }

        if (cipherAlgorithm != null) {
            this.cipherAlgorithm = cipherAlgorithm;
            try {
                cipher = Cipher.getInstance(this.cipherAlgorithm);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new IllegalStateException(e);
            }
        }
    }


    /**
     * Generate key with hexadecimal parameters : salt and privateKey
     *
     * @param salt       : the hexadecimal salt key
     * @param privateKey : the hexadecimal private key
     * @return
     */
    private SecretKey generateKey(String salt, String privateKey) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] raw = Hex.decodeHex(salt.toCharArray());
            KeySpec spec = new PBEKeySpec(privateKey.toCharArray(), raw, iterationCount, keySize);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (DecoderException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Encrypt a String with the AES encryption advanced (Interoperable Java-JS). Private key must have a length of 16 bytes
     *
     * @param value      The String to encrypt
     * @param privateKey The key used to encrypt
     * @param salt       The hexadecimal key used to salt
     * @param iv         The hexadecimal initialization vector key
     * @return The B64 encrypted string
     */
    // TODO: @Override
    public String encryptAES(String value, String privateKey, String salt, String iv) {
        try {
            SecretKey genKey = generateKey(salt, privateKey);
            byte[] encrypted = doFinal(Cipher.ENCRYPT_MODE, genKey, iv, value.getBytes("UTF-8"));
            return new String(Base64.encodeBase64(encrypted));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Decrypt a String with the AES encryption advanced (Interoperable Java-JS). Private key must have a length of 16 bytes
     *
     * @param value      A B64 encrypted string
     * @param privateKey The key used to encrypt
     * @param salt       The hexadecimal key used to salt
     * @param iv         The hexadecimal initialization vector key
     * @return The decrypted String
     */
    // TODO: @Override
    public String decryptAES(String value, String privateKey, String salt, String iv) {
        try {
            SecretKey key = generateKey(salt, privateKey);
            byte[] decrypted = doFinal(Cipher.DECRYPT_MODE, key, iv, decodeBASE64(value));
            return new String(decrypted, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Cipher doFinal
     *
     * @param encryptMode : encrypt or decrypt mode
     * @param genKey      : the secret key use to encrypt/decrypt
     * @param iv          : the initialization vector
     * @param bytes       : the plain/cipher text to encrypt/decrypt
     * @return
     */
    private byte[] doFinal(int encryptMode, SecretKey genKey, String iv, byte[] bytes) {
        try {
            byte[] raw = Hex.decodeHex(iv.toCharArray());
            cipher.init(encryptMode, genKey, new IvParameterSpec(raw));
            return cipher.doFinal(bytes);
        } catch (DecoderException | InvalidKeyException | InvalidAlgorithmParameterException |
                IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Sign a message using the application secret key (HMAC-SHA1)
     */
    @Override
    public String sign(String message) {
        return sign(message, secret.getBytes());
    }

    /**
     * Sign a message with a key
     *
     * @param message The message to sign
     * @param key     The key to use
     * @return The signed message (in hexadecimal)
     */
    @Override
    public String sign(String message, byte[] key) {
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(key);
        try {
            // Get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(message.getBytes());

            // Convert raw bytes to Hex
            byte[] hexBytes = new Hex().encode(rawHmac);

            // Covert array of Hex bytes to a String
            return new String(hexBytes, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a hash using the default hashing algorithm
     *
     * @param input The password
     * @return The password hash
     */
    @Override
    public String hash(String input) {
        return hash(input, defaultHash);
    }

    /**
     * Create a hash using specific hashing algorithm
     *
     * @param input    The password
     * @param hashType The hashing algorithm
     * @return The password hash
     */
    @Override
    public String hash(String input, Hash hashType) {
        Preconditions.checkNotNull(input);
        Preconditions.checkNotNull(hashType);
        try {
            MessageDigest m = MessageDigest.getInstance(hashType.toString());
            byte[] out = m.digest(input.getBytes());
            return new String(Base64.encodeBase64(out));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encrypt a String with the AES encryption standard using the default secret (the application secret)
     *
     * @param value The String to encrypt
     * @return An hexadecimal encrypted string
     */
    @Override
    public String encryptAES(String value) {
        return encryptAES(value, secret.substring(0, 16));
    }

    /**
     * Encrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
     *
     * @param value      The String to encrypt
     * @param privateKey The key used to encrypt
     * @return An hexadecimal encrypted string
     */
    @Override
    public String encryptAES(String value, String privateKey) {
        try {
            byte[] raw = privateKey.getBytes();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return Hex.encodeHexString(cipher.doFinal(value.getBytes()));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * Decrypt a String with the AES encryption standard using the default secret (the application secret)
     *
     * @param value An hexadecimal encrypted string
     * @return The decrypted String
     */
    @Override
    public String decryptAES(String value) {
        return decryptAES(value, secret.substring(0, 16));
    }

    /**
     * Decrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
     *
     * @param value      An hexadecimal encrypted string
     * @param privateKey The key used to encrypt
     * @return The decrypted String
     */
    @Override
    public String decryptAES(String value, String privateKey) {
        try {
            byte[] raw = privateKey.getBytes();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return new String(cipher.doFinal(Hex.decodeHex(value.toCharArray())));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Sign a token.  This produces a new token, that has this token signed with a nonce.
     * <p/>
     * This primarily exists to defeat the BREACH vulnerability, as it allows the token to effectively be random per
     * request, without actually changing the value.
     *
     * @param token The token to sign
     * @return The signed token
     */
    @Override
    public String signToken(String token) {
        long nonce = System.currentTimeMillis();
        String joined = nonce + "-" + token;
        return sign(joined) + "-" + joined;
    }

    /**
     * Extract a signed token that was signed by {@link #signToken(String)}.
     *
     * @param token The signed token to extract.
     * @return The verified raw token, or null if the token isn't valid.
     */
    @Override
    public String extractSignedToken(String token) {
        String[] chunks = token.split("-", 3);
        Preconditions.checkState(chunks.length == 3);
        String signature = chunks[0];
        String nonce = chunks[1];
        String raw = chunks[2];
        if (constantTimeEquals(signature, sign(nonce + "-" + raw))) {
            return raw;
        } else {
            return null;
        }
    }

    /**
     * Constant time equals method.
     * <p/>
     * Given a length that both Strings are equal to, this method will always run in constant time.
     * This prevents timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        } else {
            int equal = 0;
            for (int i = 0; i < a.length(); i++) {
                equal = equal | a.charAt(i) ^ b.charAt(i);
            }
            return equal == 0;
        }
    }

    /**
     * Encode binary data to base64
     *
     * @param value The binary data
     * @return The base64 encoded String
     */
    @Override
    public String encodeBASE64(byte[] value) {
        return new String(Base64.encodeBase64(value));
    }

    /**
     * Decode a base64 value
     *
     * @param value The base64 encoded String
     * @return decoded binary data
     */
    @Override
    public byte[] decodeBASE64(String value) {
        try {
            return Base64.decodeBase64(value.getBytes("utf-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Build an hexadecimal MD5 hash for a String
     *
     * @param value The String to hash
     * @return An hexadecimal Hash
     */
    @Override
    public String hexMD5(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(value.getBytes("utf-8"));
            byte[] digest = messageDigest.digest();
            return String.valueOf(Hex.encodeHex(digest));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Build an hexadecimal SHA1 hash for a String
     *
     * @param value The String to hash
     * @return An hexadecimal Hash
     */
    @Override
    public String hexSHA1(String value) {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-1");
            md.update(value.getBytes("utf-8"));
            byte[] digest = md.digest();
            return String.valueOf(Hex.encodeHex(digest));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
