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
import java.net.URLConnection;
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
            URLConnection openConnection = url.openConnection();
            openConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            is = openConnection.getInputStream();
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
        try {
            URLConnection openConnection = url.openConnection();
            openConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            final BufferedReader br = new BufferedReader(new InputStreamReader(openConnection.getInputStream()));
            String line;
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
                    .replaceAll(initial[i]+"[lr]aquo;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"[blr]dquo;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"sbquo;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"[lr]s(a)?quo;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"[mn]dash;", temporaryChar[i]+"-")
                    .replaceAll(initial[i]+"[dlru]arr;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"#859[2-5];", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"hellip;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"#8230;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"#3[49];", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"frasl;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"amp;", temporaryChar[i]+"&")
                    .replaceAll(initial[i]+"#38;", temporaryChar[i]+"&")
                    .replaceAll(initial[i]+"[Aa]acute;", temporaryChar[i]+"á")
                    .replaceAll(initial[i]+"[Aa]circ;", temporaryChar[i]+"â")
                    .replaceAll(initial[i]+"[Aa]grave;", temporaryChar[i]+"à")
                    .replaceAll(initial[i]+"[Ee]acute;", temporaryChar[i]+"é")
                    .replaceAll(initial[i]+"[Ee]circ;", temporaryChar[i]+"ê")
                    .replaceAll(initial[i]+"[Ee]grave;", temporaryChar[i]+"è")
                    .replaceAll(initial[i]+"[Ii]acute;", temporaryChar[i]+"í")
                    .replaceAll(initial[i]+"[Ii]circ;", temporaryChar[i]+"î")
                    .replaceAll(initial[i]+"[Oo]acute;", temporaryChar[i]+"ó")
                    .replaceAll(initial[i]+"[Oo]circ;", temporaryChar[i]+"ô")
                    .replaceAll(initial[i]+"[Oo]grave;", temporaryChar[i]+"ò")
                    .replaceAll(initial[i]+"[Uu]acute;", temporaryChar[i]+"ú")
                    .replaceAll(initial[i]+"[Uu]circ;", temporaryChar[i]+"û")
                    .replaceAll(initial[i]+"AElig;", temporaryChar[i]+"æ")
                    .replaceAll(initial[i]+"aelig;", temporaryChar[i]+"æ")
                    .replaceAll(initial[i]+"OElig;", temporaryChar[i]+"œ")
                    .replaceAll(initial[i]+"oelig;", temporaryChar[i]+"œ")
                    .replaceAll(initial[i]+"permil;", temporaryChar[i]+" promille ")
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
                    .replaceAll(initial[i]+"frac14;", temporaryChar[i]+" ein viertel ")
                    .replaceAll(initial[i]+"#188;", temporaryChar[i]+" ein viertel ")
                    .replaceAll(initial[i]+"frac34;", temporaryChar[i]+" drei viertel ")
                    .replaceAll(initial[i]+"#190;", temporaryChar[i]+" drei viertel ")
                    .replaceAll(initial[i]+"frac12;", temporaryChar[i]+"½")
                    .replaceAll(initial[i]+"#189;", temporaryChar[i]+"½")
                    .replaceAll(initial[i]+"times;", temporaryChar[i]+"×")
                    .replaceAll(initial[i]+"#215;", temporaryChar[i]+"×")
                    .replaceAll(initial[i]+"divide;", temporaryChar[i]+"÷")
                    .replaceAll(initial[i]+"#247;", temporaryChar[i]+"÷")
                    .replaceAll(initial[i]+"sect;", temporaryChar[i]+"§")
                    .replaceAll(initial[i]+"#167;", temporaryChar[i]+"§")
                    .replaceAll(initial[i]+"dagger;", temporaryChar[i]+"†")
                    .replaceAll(initial[i]+"#134;", temporaryChar[i]+"†")
                    .replaceAll(initial[i]+"[Aa]uml;", temporaryChar[i]+"ä")
                    .replaceAll(initial[i]+"#228;", temporaryChar[i]+"ä")
                    .replaceAll(initial[i]+"#196;", temporaryChar[i]+"ä")
                    .replaceAll(initial[i]+"[Oo]uml;", temporaryChar[i]+"ö")
                    .replaceAll(initial[i]+"#246;", temporaryChar[i]+"ö")
                    .replaceAll(initial[i]+"#214;", temporaryChar[i]+"ö")
                    .replaceAll(initial[i]+"[Uu]uml;", temporaryChar[i]+"ü")
                    .replaceAll(initial[i]+"#252;", temporaryChar[i]+"ü")
                    .replaceAll(initial[i]+"#220;", temporaryChar[i]+"ü")
                    .replaceAll(initial[i]+"szlig;", temporaryChar[i]+"ß")
                    .replaceAll(initial[i]+"#223;", temporaryChar[i]+"ß")
                    .replaceAll(initial[i]+"[Cc]cedil;", temporaryChar[i]+"ç")
                    .replaceAll(initial[i]+"#231;", temporaryChar[i]+"ç")
                    .replaceAll(initial[i]+"thorn;", temporaryChar[i]+"þ")
                    .replaceAll(initial[i]+"THORN;", temporaryChar[i]+"þ")
                    .replaceAll(initial[i]+"#254;", temporaryChar[i]+"þ")
                    .replaceAll(initial[i]+"#222;", temporaryChar[i]+"þ")
                    .replaceAll(initial[i]+"eth;", temporaryChar[i]+"ð")
                    .replaceAll(initial[i]+"#240;", temporaryChar[i]+"ð")
                    .replaceAll(initial[i]+"[Aa]ring;", temporaryChar[i]+"å")
                    .replaceAll(initial[i]+"#229;", temporaryChar[i]+"å")
                    .replaceAll(initial[i]+"#197;", temporaryChar[i]+"å")
                    .replaceAll(initial[i]+"[Oo]slash;", temporaryChar[i]+"ø")
                    .replaceAll(initial[i]+"#248;", temporaryChar[i]+"ø")
                    .replaceAll(initial[i]+"#216;", temporaryChar[i]+"ø")
                    .replaceAll(initial[i]+"[Yy]acute;", temporaryChar[i]+"ý")
                    .replaceAll(initial[i]+"#253;", temporaryChar[i]+"ý")
                    .replaceAll(initial[i]+"#45;", temporaryChar[i]+"-")
                    .replaceAll(initial[i]+"#173;", temporaryChar[i]+"-")
                    .replaceAll(initial[i]+"#448;", temporaryChar[i]+" ")
                    .replaceAll(initial[i]+"#8211;", temporaryChar[i]+" ");
        }
        tempFileContent = tempFileContent.replaceAll("&#.*?;", "");
        return tempFileContent.trim();
    }
}
