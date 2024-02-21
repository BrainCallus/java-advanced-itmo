package info.kgeorgiy.ja.churakova.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import static info.kgeorgiy.ja.churakova.walk.Walk.*;

public class RecursiveWalk {
    private static List<String> recursiveWalk(final Path path) {
        final List<String> hashes = new ArrayList<>();
        if (Files.isDirectory(path)) {
            try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {
                for (final Path entrails : dirStream) {
                    hashes.addAll(recursiveWalk(entrails));
                }
            } catch (final IOException | DirectoryIteratorException | SecurityException eRec) {
                return List.of(NULL_HASH + " " + path);
            }
            return hashes;
        } else {
            return List.of(computeFileHash(path) + " " + path);
        }
    }

    private static void handleFileList(final String inputFileName, final String outputFileName) {
        try {
            final List<String> files = Files.readAllLines(Path.of(inputFileName), StandardCharsets.UTF_8);
            try {
                final Path outPath = Path.of(outputFileName);
                if (!Files.exists(outPath) && !Files.exists(outPath.getParent())) {
                    Files.createDirectories(outPath.getParent()); // :NOTE: IOException
                }

                // :NOTE: encoding
                try (final BufferedWriter bufWriter = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
                    for (final String curFile : files) {
                        try {
                            for (final String hash : recursiveWalk(Path.of(curFile))) {
                                try {
                                    bufWriter.write(hash);
                                    bufWriter.newLine();
                                } catch (final IOException eWr) {
                                    System.err.println("IOException, can't write hash for file"
                                            + hash.substring(64));
                                }
                            }
                        } catch (final InvalidPathException ip) {
                            try {
                                bufWriter.write(NULL_HASH + " " + curFile);
                                bufWriter.newLine();
                            } catch (final IOException eWr) {
                                // :NOTE: copy-paste
                                System.err.println("IOException, can't write NULL hash for file "
                                        + curFile + ", caused by invalid path of this file");
                            }
                        }
                    }
                } catch (final UnsupportedEncodingException beOut) {
                    System.err.println("Bad encoding for output file " + outputFileName);
                } catch (final FileNotFoundException nfOut) {
                    System.out.println("Output file " + outputFileName + " not found");
                } catch (final NoSuchFileException nfOut) {
                    System.err.println("Output file " + outputFileName + " doesn't exist.");
                } catch (final IOException eOut) {
                    System.err.println("Oops, IOException! Something went wrong during writing to "
                            + outputFileName);
                } catch (final SecurityException sOut) {
                    System.err.println("Oops, SecurityException! It seems like you have no access to file "
                            + outputFileName);
                }
            } catch (final InvalidPathException ipOut) {
                System.err.println("Given path: " + outputFileName + " for output file is invalid");
            } catch (final IOException eDir) {
                System.err.println(
                        "Oops, IOException! Something went wrong during create directory for output file "
                                + outputFileName);
            }
        } catch (final InvalidPathException ipIn) {
            System.err.println("Invalid path of input file " + inputFileName);
        } catch (final UnsupportedEncodingException beIn) {
            System.err.println("Bad encoding for input file " + inputFileName);
        } catch (final FileNotFoundException nfIn) {
            System.out.println("Input file " + inputFileName + " not found");
        } catch (final NoSuchFileException e3) {
            System.err.println("Input file " + inputFileName + " doesn't exist.");
        } catch (final IOException eIn) {
            System.err.println("Oops, IOException! Something went wrong during reading filenames from input file "
                    + inputFileName);
        } catch (final SecurityException sIn) {
            // :NOTE: and what??
            System.err.println("Oops, SecurityException! It seems like you have no access to file "
                    + inputFileName);
        }
    }

    public static void main(final String[] args) {
        if (badArgs(args)) {
            return;
        }
        handleFileList(args[0], args[1]);
    }
}
