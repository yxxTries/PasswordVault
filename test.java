public class test{ 
 public static void main(String[] args) {
        try {
            String originalText = "Hello, World!";
            System.out.println("Original Text: " + originalText);

            String encryptedText = encryption.encrypt(originalText);
            System.out.println("Encrypted Text: " + encryptedText);

            String decryptedText = encryption.decrypt(encryptedText);
            System.out.println("Decrypted Text: " + decryptedText);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}