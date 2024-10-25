import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class RarBruteForce {

    private static final String RAR_FILE = "final.rar";  // Name of the RAR file
    private static final String DICTIONARY_FILE = "words.txt";  // Name of the dictionary file
    private static final String UNRAR_EXE = "UnRar.exe";     // Name of the unrar executable

    public static void main(String[] args) {
        int threads = Runtime.getRuntime().availableProcessors(); // Use all available cores
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try (BufferedReader br = Files.newBufferedReader(Paths.get(DICTIONARY_FILE))) {
            String password;
            while ((password = br.readLine()) != null) {
                String finalPassword = password;
                executor.submit(() -> {
                    String threadName = Thread.currentThread().getName();
                    System.out.println("Thread " + threadName + " attempting password: " + finalPassword);
                    
                    if (tryPassword(finalPassword)) {
                        System.out.println("Password found: " + finalPassword);
                        executor.shutdownNow();
                    }
                });
            }
        } catch (IOException e) {
            System.out.println("Error reading dictionary file: " + e.getMessage());
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.out.println("Password not found within the dictionary file.");
            }
        } catch (InterruptedException e) {
            System.out.println("Execution interrupted: " + e.getMessage());
        }
    }

    private static boolean tryPassword(String password) {
        // Clean up the password: convert to uppercase, remove spaces, and remove special characters
        String cleanedPassword = password.toUpperCase()
                                        .replaceAll("\\s+", "")  // Remove all spaces
                                        .replaceAll("[^A-Z0-9]", ""); // Remove all non-alphanumeric characters

        try {
            ProcessBuilder builder = new ProcessBuilder(UNRAR_EXE, "t", "-p" + cleanedPassword, RAR_FILE);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = output.readLine()) != null) {
                if (line.contains("All OK")) {
                    return true;
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println("Error trying password: " + cleanedPassword + " - " + e.getMessage());
        }
        return false;
    }

}
