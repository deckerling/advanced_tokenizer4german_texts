// Main.java
//
// Copyright 2019 E. Decker
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// An extended whitespace tokenizer for German text files and websites.
package tokenizer4germantexts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class Main {
    
    public static void main(String args[]) {
        String address;
        final Boolean extendedTokenization;
        int allUTF8 = 0; // stays 0 if the user enters a single URL or path to a single file to tokenize
        try (final Scanner scanner = new Scanner(System.in)) {
            System.out.println("Enter either \n\t- an URL or a path to a file that should be tokenized, OR\n\t- a path to a directory to tokenize all text files in it, OR\n\t- \"file\" to read several URLs or paths from a file to tokenize all of them:");
            address = scanner.nextLine();
            System.out.println("Should the extended tokenization feature be used? (Enter 'y' for \"yes\" or anything else for \"no\".)");
            extendedTokenization = (scanner.nextLine().equalsIgnoreCase("y"))? true : false;
            if (address.equalsIgnoreCase("file")) {
                System.out.println("Enter the path to the file you saved the URLs or paths in (every line of that file should contain exactly one URL or path; the paths can lead to both files and directories):");
                address = scanner.nextLine();
                try (final Scanner fileScanner = new Scanner(new FileInputStream(new File(address)))) {
                    String currentAddressToWorkOn;
                    System.out.println("If the file contains paths to files you want to tokenize:\n\tAre all of these files UTF-8 encoded? (Enter 'y' for \"yes\" or anything else for \"no\" or to skip this question.)");
                    allUTF8 = (scanner.nextLine().equalsIgnoreCase("y"))? 1 : 2;
                    while (fileScanner.hasNextLine()) { // works on all URLs or paths found in the file
                        currentAddressToWorkOn = fileScanner.nextLine().trim();
                        if (!currentAddressToWorkOn.isEmpty()) {
                            startProcess(currentAddressToWorkOn, "/"+address, extendedTokenization, allUTF8);
                        }
                    }
                } catch (FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                }
            } else {
                startProcess(address, "", extendedTokenization, allUTF8);
            }
        }
        System.out.println("Program terminated.");
    }
    
    private static final void startProcess(String address, final String target, final boolean extendedTokenization, final int allUTF8) {
    /* Tries to open the "address" as an URL. If "MalformedURLException" is raised, the program
     * checks if a directory or a file exists that has got the "address" as path; if not, the
     * program will be terminated (as long as allUTF8 == 0, which would mean that the program is
     * not already working on a directory or the content of a file containing URLs or paths to
     * files). */
        String charset = "default";
        try {
            /* Starts "Webloader". If there is both a problem opening the URL and neither a "file"
             * containing URLs or paths nor a directory containing text files used (so only one
             * file or URL should be tokenized), the program will be terminated. */
            final URL url = new URL(address);
            final boolean websiteLoaded = new Webloader(url).loadWebsite();
            if (!websiteLoaded && allUTF8 == 0) {
                System.err.println("Program terminated.");
                System.exit(0);
            } else if (!websiteLoaded) {
                throw new Exception();
            }
            address = "temp";
            System.out.println("Working on a website (\""+url+"\")...");
        } catch (MalformedURLException mue) {
            final File pathToCheck = new File(address);
            if (pathToCheck.isDirectory()) {
                workOnDirectory(address, extendedTokenization, allUTF8);
                return;
            } else if(!pathToCheck.exists()) {
                System.err.println("Your entry couldn't be classified! Neighter a file nor a valid URL corresponding to your entry was found.");
                if (allUTF8 == 0) {
                    System.out.println("Program terminated.");
                    System.exit(0);
                } else {
                    System.err.println("The tokenization of \""+address+"\" has failed.");
                    return;
                }
            } else {
                System.out.println("Working on a file(\""+address+"\")...");
                if (allUTF8 == 0) {
                    System.out.println("Is the file you want to tokenize UTF-8 encoded? (Enter 'y' for \"yes\" or anything else for \"no\".)");
                    try (final Scanner scanner = new Scanner(System.in)) {
                        charset = (scanner.nextLine().equalsIgnoreCase("y"))? "UTF-8" : "default";
                    }
                } else if (allUTF8 == 1) {
                    charset = "UTF-8";
                }
            }
        } catch (Exception e) {
            return;
        }
        /* Starts tokenizing the file "temp" (if the user has entered an URL) or the file with
         * "address" as path. */
        new Tokenizer(address, target, charset, extendedTokenization);
    }
    
    private static final void workOnDirectory(final String address, final boolean extendedTokenization, int allUTF8) {
    /* Passes all files found in the directory with the path "address" back to the method
     * "startProcess()" where they will finally be passed to the tokenizer. */
        try (final Scanner scanner = new Scanner(System.in)) {
            if (allUTF8 == 0) {
                System.out.println("You're about to tokenize all the files in the directory \""+address+"\".\nAre all of those files UTF-8 encoded? (Enter 'y' for \"yes\" or anything else for \"no\".)");
                allUTF8 = (scanner.nextLine().equalsIgnoreCase("y"))? 1 : 2;
            }
            final File directory = new File(address);
            final File[] filesInDirectory = directory.listFiles();
            for (File file : filesInDirectory) { // works on all files found in the directory
                if (file.isFile()) {
                    startProcess(directory+"/"+file.getName(), "/"+directory, extendedTokenization, allUTF8);
                }
            }
        }
    }
}
