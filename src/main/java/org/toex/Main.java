package org.toex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        String testMarkdown = "";
        try {
            Path path = Paths.get("markdown.md");
            testMarkdown = Files.readString(path); // Reads the entire file content as a String
        } catch (Exception e) {
            e.printStackTrace();
        }

        MDParser parser = new MDParser();
        HTMLElement html = parser.compile(testMarkdown);
        String content = html.createDocument();
        String filePath = "test.html";

        try {
            File file = new File(filePath);

            // Check if the file exists
            if (!file.exists()) {
                // Create a new file if it doesn't exist
                file.createNewFile();
            }
            // Write to the file (overwriting the existing content)
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content);
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}