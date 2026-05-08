package common;

// Base64 handles safe text encoding
import java.util.Base64;

public class EncryptionUtil {

    // Single char XOR encryption key
    private static final char XOR_KEY = 'X';

    public static String encrypt(String input) {
        // Return null if input null
        if (input == null) return null;

        // Builder stores XOR result
        StringBuilder xorResult = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            // XOR each character with key
            xorResult.append((char) (input.charAt(i) ^ XOR_KEY));
        }

        // Encode to Base64 to ensure safe transmission/storage
        return Base64.getEncoder().encodeToString(xorResult.toString().getBytes());
    }

     //Decodes from Base64 and then reverses the XOR cipher

    public static String decrypt(String base64Input) {
        // Return null if input null
        if (base64Input == null) return null;

        // Decode Base64 back to the XORed string
        byte[] decodedBytes = Base64.getDecoder().decode(base64Input);
        // Convert bytes back to string
        String xorResult = new String(decodedBytes);

        // Builder restores original text
        StringBuilder original = new StringBuilder();
        for (int i = 0; i < xorResult.length(); i++) {
            // Reverse XOR to get original
            original.append((char) (xorResult.charAt(i) ^ XOR_KEY));
        }

        return original.toString();
    }

    // MAIN METHOD FOR TESTING
    public static void main(String[] args) {
        // Sample message to test encryption
        String testMessage = "Hello, this is a secret chat message!";
        System.out.println("Original: " + testMessage);

        // Encrypt then print result
        String encryptedMessage = encrypt(testMessage);
        System.out.println("Encrypted (Base64): " + encryptedMessage);

        // Decrypt and print result
        String decryptedMessage = decrypt(encryptedMessage);
        System.out.println("Decrypted: " + decryptedMessage);

        // Verify decryption matches original
        if (testMessage.equals(decryptedMessage)) {
            System.out.println("\nSUCCESS: Step 11 is complete. Encryption works!");
        } else {
            System.out.println("\nERROR: Decrypted string does not match the original.");
        }
    }
}