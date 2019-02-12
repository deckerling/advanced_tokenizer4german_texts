// Webloader.java
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

package tokenizer4germantexts;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;

// A tool to download the source code of a website and to prepare it for tokenization.
public class Webloader {
    
    private final URL url;
    
    public Webloader(final URL url) {
        this.url = url;
    }
    
    public final boolean loadWebsite() {
        InputStream is = null;
        BufferedReader br = null;
        String line;
        OutputStreamWriter writer = null;
        // Tries to open the URL, to read the HTML-code, and to work on it.
        try {
            is = url.openStream();
            br = checkCharset()? new BufferedReader(new InputStreamReader(is, "UTF-8")) : new BufferedReader(new InputStreamReader(is));
            writer = new OutputStreamWriter(new FileOutputStream("temp"));
            String contentOfWebsite = "";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    contentOfWebsite += line+" ";
                }
            }
            writer.write(clearHTML(contentOfWebsite)); // writes the "cleared" HTML-code into a temporary file
        } catch (UnknownHostException | ConnectException internetProblems) {
            System.err.println("The URL (\""+url+"\") couldn't be opened. Check if it is spelled correctly and if you are connected to the internet!");
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (br != null) {
                    br.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return true;
    }
    
    private final boolean checkCharset() {
    // Returns "true" if the "charset" of the HTML-code is "UTF-8" and "false" otherwise.
        String line;
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            while ((line = br.readLine()) != null) {
                line = line.toLowerCase();
                if (line.contains("charset") && line.contains("utf-8")) {
                    return true;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return false;
    }
    
    private static final String clearHTML(String tempFileContent) {
    // Deletes HTML tags and comments and clarifies some of the special characters.
        // Deletes the HTML-tags and comments.
        final String[] start = {"<script", "<style", "<!--", "<", "&lt;", "&#60;", "&#91;"};
        final String[] end = {"/script>", "/style>", "-->", ">", "&gt;", "&#63;", "&#93;"};
        for (int i=0; i<start.length; i++) {
            tempFileContent = tempFileContent.replaceAll(start[i]+".*?"+end[i], " _ ");
        }
        /* Clarifies some of the special characters in the HTML code that seem to be important or
         * are often used in German. (Some of them will be replaced later during the tokenization.) */
        final String[] initial = {";", "&"};
        final String[] temporaryChar = {";", ""};
        tempFileContent = tempFileContent.replaceAll("&amp;nbsp;", " ");
        for (int i=0; i<initial.length; i++) {
            tempFileContent = tempFileContent.replaceAll(initial[i]+"nbsp;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"#160;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"quot;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"apos;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"#3[49];", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"amp;", temporaryChar[i]+"&")
                    .replaceAll(initial[i]+"#38;", temporaryChar[i]+"&")
                    .replaceAll(initial[i]+"cent;", temporaryChar[i]+" cent ")
                    .replaceAll(initial[i]+"#162;", temporaryChar[i]+" cent ")
                    .replaceAll(initial[i]+"euro;", temporaryChar[i]+" euro ")
                    .replaceAll(initial[i]+"#8364;", temporaryChar[i]+" euro ")
                    .replaceAll(initial[i]+"pound;", temporaryChar[i]+" pfund ")
                    .replaceAll(initial[i]+"#163;", temporaryChar[i]+" pfund ")
                    .replaceAll(initial[i]+"yen;", temporaryChar[i]+" yen ")
                    .replaceAll(initial[i]+"#165;", temporaryChar[i]+" yen ")
                    .replaceAll(initial[i]+"copy;", temporaryChar[i]+" copyright ")
                    .replaceAll(initial[i]+"#169;", temporaryChar[i]+" copyright ")
                    .replaceAll(initial[i]+"sect;", temporaryChar[i]+"§")
                    .replaceAll(initial[i]+"#167;", temporaryChar[i]+"§")
                    .replaceAll(initial[i]+"dagger;", temporaryChar[i]+"†")
                    .replaceAll(initial[i]+"#134;", temporaryChar[i]+"†")
                    .replaceAll(initial[i]+"auml;", temporaryChar[i]+"ä")
                    .replaceAll(initial[i]+"#228;", temporaryChar[i]+"ä")
                    .replaceAll(initial[i]+"Auml;", temporaryChar[i]+"Ä")
                    .replaceAll(initial[i]+"#196;", temporaryChar[i]+"Ä")
                    .replaceAll(initial[i]+"ouml;", temporaryChar[i]+"ö")
                    .replaceAll(initial[i]+"#246;", temporaryChar[i]+"ö")
                    .replaceAll(initial[i]+"Ouml;", temporaryChar[i]+"Ö")
                    .replaceAll(initial[i]+"#214;", temporaryChar[i]+"Ö")
                    .replaceAll(initial[i]+"uuml;", temporaryChar[i]+"ü")
                    .replaceAll(initial[i]+"#252;", temporaryChar[i]+"ü")
                    .replaceAll(initial[i]+"Uuml;", temporaryChar[i]+"Ü")
                    .replaceAll(initial[i]+"#220;", temporaryChar[i]+"Ü")
                    .replaceAll(initial[i]+"szlig;", temporaryChar[i]+"ß")
                    .replaceAll(initial[i]+"#223;", temporaryChar[i]+"ß")
                    .replaceAll(initial[i]+"#45;", temporaryChar[i]+"-")
                    .replaceAll(initial[i]+"#173;", temporaryChar[i]+"-")
                    .replaceAll(initial[i]+"#448;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"#8211;", temporaryChar[i]+" ");
        }
        return tempFileContent.trim();
    }
}
