package common;

import java.util.Base64;

public class EncryptionUtil {

    // A simple key for XOR encryption
    private static final char XOR_KEY = 'X';

    /**
     * Applies XOR cipher and then encodes to Base64
     */
    public static String encrypt(String input) {
        if (input == null) return null;

        StringBuilder xorResult = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            xorResult.append((char) (input.charAt(i) ^ XOR_KEY));
        }

        // Encode to Base64 to ensure safe transmission/storage
        return Base64.getEncoder().encodeToString(xorResult.toString().getBytes());
    }

    /**
     * Decodes from Base64 and then reverses the XOR cipher
     */
    public static String decrypt(String base64Input) {
        if (base64Input == null) return null;

        // Decode Base64 back to the XORed string
        byte[] decodedBytes = Base64.getDecoder().decode(base64Input);
        String xorResult = new String(decodedBytes);

        StringBuilder original = new StringBuilder();
        for (int i = 0; i < xorResult.length(); i++) {
            original.append((char) (xorResult.charAt(i) ^ XOR_KEY));
        }

        return original.toString();
    }

    // MAIN METHOD FOR TESTING (As requested in Step 11)
    public static void main(String[] args) {
        String testMessage = "Hello, this is a secret chat message!";
        System.out.println("Original: " + testMessage);

        String encryptedMessage = encrypt(testMessage);
        System.out.println("Encrypted (Base64): " + encryptedMessage);

        String decryptedMessage = decrypt(encryptedMessage);
        System.out.println("Decrypted: " + decryptedMessage);

        if (testMessage.equals(decryptedMessage)) {
            System.out.println("\nSUCCESS: Step 11 is complete. Encryption works!");
        } else {
            System.out.println("\nERROR: Decrypted string does not match the original.");
        }
    }
}