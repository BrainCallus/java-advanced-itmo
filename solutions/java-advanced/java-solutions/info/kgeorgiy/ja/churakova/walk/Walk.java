package info.kgeorgiy.ja.churakova.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Walk {
    protected static final String NULL_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private static String digestBytesToString(final MessageDigest mesDig) {
        final StringBuilder hexFormat = new StringBuilder();
        for (final byte b : mesDig.digest()) {
            hexFormat.append(String.format("%02x", b));
        }
        return hexFormat.toString();
    }

    protected static String computeFileHash(final Path filePath) {
        try {
            MessageDigest mesDig = MessageDigest.getInstance("SHA-256");

            try (final DigestInputStream digInput = new DigestInputStream(new FileInputStream(filePath.toString()), mesDig)) {
                try {
                    digInput.readAllBytes();
                } catch (final IOException eRd) {
                    System.err.println("Oops, IOException! Something went wrong during reading bytes from ."
                            + filePath);
                }
                mesDig = digInput.getMessageDigest();
            } catch (final FileNotFoundException | NoSuchFileException | SecurityException efd) {
                return NULL_HASH;
            } catch (final IOException eDig) {
                System.err.println("Oops, IOException! Something went wrong during creating DigestInputStream.");
            }
            return digestBytesToString(mesDig);
        } catch (final NoSuchAlgorithmException a) {
            System.err.println("Incorrect algorithm name");
        }
        return NULL_HASH;
    }

    private static void handleFiles(final String inputFileName, final String outputFileName) {
        // :NOTE: simplify
        try {
            List<String> files = Files.readAllLines(Path.of(inputFileName), StandardCharsets.UTF_8);
            try {
                Path outPath = Path.of(outputFileName);
                if (!Files.exists(outPath) && !Files.exists(outPath.getParent())) {
                    Files.createDirectories(outPath.getParent()); // :NOTE: IOException
                }

                try (BufferedWriter bufWriter = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
                    for (String curFile : files) {
                        try {
                            bufWriter.write(computeFileHash(Path.of(curFile)) + " " + curFile);
                            bufWriter.newLine();
                        } catch (InvalidPathException ip) {
                            try {
                                bufWriter.write(NULL_HASH + " " + curFile);
                                bufWriter.newLine();
                            } catch (IOException eWr) {
                                System.err.println("IOException, can't write NULL hash for file "
                                        + curFile + ", caused by invalid path of this file");
                            }
                        } catch (IOException eWr) {
                            System.err.println("IOException, can't write hash for file " + curFile);
                        }
                    }
                } catch (UnsupportedEncodingException beOut) {
                    System.err.println("Bad encoding for output file " + outputFileName);
                } catch (FileNotFoundException nfOut) {
                    System.out.println("Output file " + outputFileName + " not found");
                } catch (NoSuchFileException nfOut) {
                    System.err.println("Output file " + outputFileName + " doesn't exist.");
                } catch (IOException eOut) {
                    System.err.println("Oops, IOException! Something went wrong during writing to "
                            + outputFileName);
                } catch (SecurityException sOut) {
                    System.err.println("Oops, SecurityException! It seems like you have no access to file "
                            + outputFileName);
                }
            } catch (InvalidPathException ipOut) {
                System.err.println("Given path: " + outputFileName + " for output file is invalid");
            } catch (IOException eDir) {
                System.err.println(
                        "Oops, IOException! Something went wrong during create directory for output file "
                                + outputFileName);
            }
        } catch (InvalidPathException ipIn) {
            System.err.println("Invalid path of input file " + inputFileName);
        } catch (UnsupportedEncodingException beIn) {
            System.err.println("Bad encoding for input file " + inputFileName);
        } catch (FileNotFoundException nfIn) {
            System.out.println("Input file " + inputFileName + " not found");
        } catch (NoSuchFileException e3) {
            System.err.println("Input file " + inputFileName + " doesn't exist.");
        } catch (IOException eIn) {
            System.err.println("Oops, IOException! Something went wrong during reading filenames from input file "
                    + inputFileName);
        } catch (SecurityException sIn) {
            System.err.println("Oops, SecurityException! It seems like you have no access to file "
                    + inputFileName);
        }
    }

    protected static boolean badArgs(final String[] args) {
        if (args == null) {
            System.err.println("Args are null");
            return true;
        } else if (args.length != 2) {
            System.err.println("Required 2 arguments, found " + args.length);
            return true;
        } else if (args[0] == null && args[1] == null) {
            System.err.println("Both of arguments is null");
            return true;
        } else if (args[0] == null) {
            System.err.println("First argument is null");
            return true;
        } else if (args[1] == null) {
            System.err.println("Second argument is null");
            return true;
        }
        return false;
    }

    public static void main(final String[] args) {
        if (badArgs(args)) {
            return;
        }
        handleFiles(args[0], args[1]);
    }
}
