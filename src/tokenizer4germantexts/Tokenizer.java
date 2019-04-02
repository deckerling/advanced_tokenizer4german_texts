// Tokenizer.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

// A whitespace tokenizer for German texts (note that all tokens will be set to lower case letters).
public class Tokenizer {
    
    private final static String[] UMLAUTE = {"ä", "ö", "ü"};
    private final static String[] FIRST_PART_OF_NUMERAL = {"ein", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun"};
    private final static String[][] MONTHS = {{"januar ", "jan ", "jan\\."}, {"februar ", "feb ", "feb\\."}, {"märz ", "mär ", "mär\\.", "mar ", "mar\\."}, {"april ", "apr ", "apr\\."}, {"mai ", "may "}, {"juni ", "jun ", "jun\\."}, {"juli ", "jul ", "jul\\."}, {"august ", "aug ", "aug\\."}, {"september ", "sep ", "sep\\."}, {"oktober ", "okt ", "okt\\.", "oct ", "oct."}, {"november ", "nov", "nov\\."}, {"dezember ", "dez ", "dez\\.", "dec ", "dec."}};
    private final boolean extendedTokenization;
    
    public Tokenizer(final String address, String target, final String charset, final Boolean extendedTokenization) {
	/* Starts the tokenizer, creates the output file (and - if necessary - its directory), reads
	 * and tokenizes the input file and writes its tokenized content into the output file. */
        this.extendedTokenization = extendedTokenization;
	    /* If a file with URLs or paths is used - and not a single URL or a single file to
	     * tokenize - a name for a new directory for this project will be generated out of the name
	     * of the file that contains the URLs or paths (e.g. if the file containing the URLs or
	     * paths is called "file1.txt" the new directory will be "token_files/file1.txt/"). The
	     * program will work similar if the user is about to tokenize all files in a certain
	     * directory. */
        if (target.contains("/") || target.contains("\\")) {
            target = target.replaceAll("\\\\", "/");
            StringBuffer sb;
            while (target.contains("/")) {
                sb = new StringBuffer(target);
                sb = sb.delete(0, target.indexOf("/")+1);
    	        target = sb.toString();
    	    }
            target = "/"+target;
        }
        /* Checks if the directories "token_files" and ""token_files"+target" already exist; if
         * not, they will be created. */
        final File tokenFiles = new File("token_files"+target);
        if (!new File("token_files").exists()) {
            new File("token_files").mkdir();
        }
        if (!target.equals("") && !target.equals("/") && !tokenFiles.exists()) {
            tokenFiles.mkdir();
        }
    	// Numbers and names the token files (i.e. the created token files get default names).
        File tokenFile = new File("token_files"+target+"/tokens0.txt");
    	int fileNumber = 0;
        while(tokenFile.exists()) {
            fileNumber++;
            tokenFile = new File("token_files"+target+"/tokens"+Integer.toString(fileNumber)+".txt");
        }
        FileInputStream fileStream = null;
        final File fileToTokenize = new File(address);
        Scanner scanner = null;
        String lines;
        Writer writer = null;
        /* Reads the input file, tokenizes its content, and writes the tokenized content to the
         * output file. */
        try {
            fileStream = new FileInputStream(fileToTokenize);
            if (charset.equals("UTF-8")) {
                scanner = new Scanner(fileStream, charset);
                writer = new OutputStreamWriter(new FileOutputStream("token_files"+target+"/tokens"+Integer.toString(fileNumber)+".txt", true), StandardCharsets.UTF_8);
            } else {
                scanner = new Scanner(fileStream);
                writer = new OutputStreamWriter(new FileOutputStream("token_files"+target+"/tokens"+Integer.toString(fileNumber)+".txt", true));
            }
            int count = 0, flush_factor = 1;
            while (scanner.hasNextLine()) {
                lines = "";
                for (int i=0; i<100; i++) { // working on more than one line at once makes the program run faster in many cases
                    if (scanner.hasNextLine()) {
                        lines += scanner.nextLine()+" ";
                    } else {
                        break;
                    }
                }
                if (!lines.isEmpty()) {
                    lines = tokenize(" "+lines); // starts the actual "tokenize()" method
                    lines = lines.trim();
                    if (!lines.isEmpty()) {
                        if (scanner.hasNextLine()) {
                            lines += " ";
                        }
                        while (!checkIfLinesStartWithLetterOrNumber(lines)) { // makes sure that unwanted characters at the beginning of "lines" get deleted
                            lines = lines.substring(1);
                        }
                        try {
                            writer.write(lines);
                            if (count == 50*flush_factor) { // flushes in an interval of 50 (with respect to "count")
                                writer.flush();
                                flush_factor++;
                            }
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                }
                count++;
            }
            System.out.println("Tokenization"+(!address.equals("temp")? " of \""+address+"\"" : "")+" successful! (Output file: \"token_files"+target+"/tokens"+Integer.toString(fileNumber)+".txt\")");
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            if (scanner != null) {
                scanner.close();
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        // Deletes the "temp"-file created by "Webloader" if a website was tokenized.
        if (address.equals("temp")) {
            fileToTokenize.delete();
        }
    }
    
    private final String tokenize(String lines) {
	/* Tokenizes the input text (whitespace tokenization). If "extendedTokenization" is "true" some
	 * special work like the replacement of numbers with their corresponding German numerals
	 * (words) will be done. 
	 * Certain '.', ':', and '-' in the text that will be needed later get temporarily replaced with
	 * "|ßß|", " XßßX " and "YßßY". 
	 * Be aware that the work that will be done if "extendedTokenization" is "true" could take some
	 * time. */
        lines = lines.toLowerCase();
        lines = lines.replaceAll("[\\(\\)\\[\\]\\s+]", " ");
        
        if (extendedTokenization) {
            for (int i=0; i<10; i++) {
                for (int j=0; j<10; j++) {
                    lines = lines.replaceAll(Integer.toString(i)+" "+Integer.toString(j), Integer.toString(i)+"|ßß|"+Integer.toString(j))
                            .replaceAll(Integer.toString(i)+":"+Integer.toString(j), Integer.toString(i)+" XßßX "+Integer.toString(j))
                            .replaceAll(Integer.toString(i)+"-"+Integer.toString(j), Integer.toString(i)+" YßßY "+Integer.toString(j));
                }
                lines = lines.replaceAll(Integer.toString(i)+"\\. jh(d(t)?)?\\.", Integer.toString(i)+". jahrhundert ")
                        .replaceAll("\\."+Integer.toString(i), "|ßß|"+Integer.toString(i))
                        .replaceAll(Integer.toString(i)+"\\.", Integer.toString(i)+"|ßß|")
                        .replaceAll(","+Integer.toString(i), "|ßß|"+Integer.toString(i))
                        .replaceAll(Integer.toString(i)+"( )?km", Integer.toString(i)+" kilometer")
                        .replaceAll(Integer.toString(i)+"( )?kg", Integer.toString(i)+" kilogramm")
                        .replaceAll(" -"+Integer.toString(i), " minus "+Integer.toString(i));
            }
        } else {
            for (int i=0; i<10; i++) {
                lines = lines.replaceAll(Integer.toString(i)+"km", Integer.toString(i)+" km")
                        .replaceAll(Integer.toString(i)+"kg", Integer.toString(i)+" kg")
                        .replaceAll(" -"+Integer.toString(i), " YßßY"+Integer.toString(i))
                        .replaceAll(Integer.toString(i)+"\\.", Integer.toString(i)+"|ßß|");
            }
        }
        
        lines = lines.replaceAll("[;:!#_<>~…„“”»«›‹•·..‚¨\"\\^\\*\\?\\{\\}\\\\]", " ")
                .replaceAll(" ‘", " ")
                .replaceAll("‘ ", " ")
                .replaceAll(" `", " ")
                .replaceAll("` ", " ")
                .replaceAll(" ´", " ")
                .replaceAll("´ ", " ")
                .replaceAll(" ’", " ")
                .replaceAll("’ ", " ");
        
        lines = workOnAbbreviations(lines);

        lines = workOnHyphensAndDashes(lines);
        for (String umlaut : UMLAUTE) {
            lines = lines.replaceAll(" "+umlaut+"\\.", " "+umlaut+"|ßß|")
                    .replaceAll("\\.( )?"+umlaut+"\\.", "|ßß|"+umlaut+"|ßß|");
        }
        lines = lines.replaceAll("[,/\\.]", " ");
        lines = lines.replaceAll("[‘’'´`]", "")
                .replaceAll("\\|ßß\\|", ".")
                .replaceAll("= = = = =", " ");
        lines = lines.replaceAll("= = = =", " ");
        lines = lines.replaceAll("= = =", " ");
        lines = lines.replaceAll("= =", " ");
        lines = lines.replaceAll("===+", " ");

        final String[] variousCharsToReplace = {"\\$", "\\+", "§", "€", "£", "¥", "%", "&", "×", "÷", "²", "†"};
        if (extendedTokenization) {
            /* Replaces certain characters like '&' or numbers like 10 with their corresponding
             * words like "und" (German for "and") or numerals like "zehn" (German for "ten").
             * Furthermore, some time formats will be replaced as well (e.g. "12:43 Uhr" will be
             * replaced with "zwölf uhr dreiundvierzig"). */
            final String[] replacements = {" dollar ", " plus ", " paragraph ", " euro ", " pfund ", " yen ", " prozent ", " und ", " mal ", " geteilt durch ", " hoch zwei ", " gestorben "};
            for (int i=0; i<variousCharsToReplace.length; i++) {
                lines = lines.replaceAll(variousCharsToReplace[i], replacements[i]);
            }
            lines = lines.replaceAll("( )?°c ", " grad celsius ")
                    .replaceAll("( )?°f ", " grad fahrenheit ");
            lines = lines.replaceAll("°", " grad ")
                    .replaceAll("\\|", " ")
                    .replaceAll("=(=)?", " ist ");
            lines = lines.replaceAll("\\s+", " ");
            lines = replaceTimeFormats(lines);
            lines = clarifyDates(lines.split(" "));
            lines = lines.replaceAll(" \\D ", " "); // deletes single characters (which are not an digits) (this won't affect most of the abbreviations because they normally got the format "a.")
            lines = replaceNumbers(lines); // this method will work less efficient if you deactivate the method "clarifyDates()"
            lines = lines.replaceAll(" uhr null ", " uhr ");
            lines = detectYears(lines); // this method will work less efficient if you deactivate the methods "replaceNumbers()" or "clarifyDates()"
        } else {
            for (int i=2; i<variousCharsToReplace.length; i++) {
                lines = lines.replaceAll(variousCharsToReplace[i], " "+variousCharsToReplace[i]+" ");
            }
            lines = lines.replaceAll("\\$", " \\$ ")
                    .replaceAll("\\+", " + ")
                    .replaceAll("=(=)?", " = ")
                    .replaceAll("°c ", " °c ")
                    .replaceAll("°f ", " °f ");
        }
        
        lines = lines.replaceAll("YßßY", "-")
                .replaceAll(" XßßX ", " : ")
                .replaceAll(" \\.", " ");
	    return lines.replaceAll("\\s+", " ");
    }

    private final String workOnAbbreviations(String lines) {
        // Saves or - if "extendedTokenization" == "true" - clarifies common German abbreviations.
        lines = lines.replaceAll(" bsp\\. ", " bsp|ßß| ")
                .replaceAll(" et al\\. ", " et al|ßß| ")
                .replaceAll(" etw\\. ", " etw|ßß| ")
                .replaceAll(" evtl\\. ", " evtl|ßß| ")
                .replaceAll(" ff\\. ", " ff|ßß| ")
                .replaceAll(" gdw\\. ", " gdw|ßß| ")
                .replaceAll(" hg\\. ", " hg|ßß| ")
                .replaceAll(" hrsg\\. ", " hrsg|ßß| ")
                .replaceAll(" idr\\. ", " idr|ßß| ")
                .replaceAll(" jmd\\. ", " jmd|ßß| ")
                .replaceAll(" ka\\. ", " ka|ßß| ")
                .replaceAll(" oä\\. ", " oä|ßß| ")
                .replaceAll(" so\\. ", " so|ßß| ")
                .replaceAll(" su\\. ", " su|ßß| ")
                .replaceAll(" ua\\. ", " ua|ßß| ")
                .replaceAll(" uä\\. ", " uä|ßß| ")
                .replaceAll(" usw\\. ", " usw|ßß| ")
                .replaceAll(" uu\\. ", " uu|ßß| ")
                .replaceAll(" uvm\\. ", " uvm|ßß| ")
                .replaceAll(" va\\. ", " va|ßß| ")
                .replaceAll(" zt\\. ", " zt|ßß| ");
        final String[][] abbreviationsOld0 = {{" f\\.f\\. ", " k\\.a\\. ", " s\\.o\\. ", " s\\.u\\. ", " u\\.a\\. ", " u\\.u\\. ", " v\\.a\\. ", " z\\.z\\. ", " g\\.d\\.w\\. ", " u\\.s\\.w\\. "}, {" f\\. f\\. ", " k\\. a\\. ", " s\\. o\\. ", " s\\. u\\. ", " u\\. a\\. ", " u\\. u\\. ", " v\\. a\\. ", " z\\. z\\. ", " g\\. d\\. w\\. ", " u\\. s\\. w\\. "}};
        final String[] abbreviationsNew0 = {" f|ßß|f|ßß| ", " k|ßß|a|ßß| ", " s|ßß|o|ßß| ", " s|ßß|u|ßß| ", " u|ßß|a|ßß| ", " u|ßß|u|ßß| ", " v|ßß|a|ßß| ", " z|ßß|z|ßß| ", " g|ßß|d|ßß|w|ßß| ", " u|ßß|s|ßß|w|ßß| "};
        for (int i=0; i<abbreviationsOld0.length; i++) {
            for (int j=0; j<abbreviationsNew0.length; j++) {
                lines = lines.replaceAll(abbreviationsOld0[i][j], abbreviationsNew0[j]);
            }
        }
        final String[] abbreviationsOld1 = {" d\\.( )?h\\. ", " n\\.( )?chr\\. ", " o\\.( )?ä\\. ", " u\\.( )?ä\\. ", " v\\.( )?chr\\. ", " z\\.( )?b\\. ", " z\\.( )?hd\\. ", " z\\.( )?t\\. ", " i\\.( )?d\\.( )?r\\. ", " u\\.( )?v\\.( )?m\\. ", " abschn\\. ", " aufl\\. ", " bspw\\. ", " bzgl\\. ", " bzw\\. ", " ca\\. ", " dh\\. ", " dr\\. ", " etc\\. ", " ggf\\. ", " grds\\. ", " inkl\\. ", " insb\\. ", " nchr\\. ", " nr\\. ", " prof\\. ", " st\\. ", " vchr\\. ", " vgl\\. ", " zb\\. ", " zzgl\\. "};
        if (extendedTokenization) {
            lines = lines.replaceAll(" kilometer/h", " kilometer pro stunde ");
            final String[] abbreviationsNew1 = {" das heißt ", " nach christus ", " oder ähnl|ßß| ", " und ähnl|ßß| ", " vor christus ", " zum beispiel ", " zu händen ", " zum teil ", " in der regel ", " und viel|ßß| mehr ", " abschnitt ", " auflage ", " beispielsweise ", " bezüglich ", " beziehungsweise ", " circa ", " das heißt ", " doktor ", " et cetera ", " gegebenenfalls ", " grundsätzlich ", " inklusive ", " insbesondere ", " nach christus ", " nummer ", " professor ", " sankt ", " vor christus ", " vergleiche ", " zum beispiel ", " zuzüglich "};
            for (int i=0; i<abbreviationsOld1.length; i++) {
                lines = lines.replaceAll(abbreviationsOld1[i], abbreviationsNew1[i]);
            }
            for (int i=1; i<10; i++) {
                lines = lines.replaceAll("str\\. "+Integer.toString(i), "straße "+Integer.toString(i));
            }
        } else {
            lines = lines.replaceAll(" km/h", " kmh ");
            final String[] abbreviationsNew1 = {" d|ßß|h|ßß| ", " n|ßß|chr|ßß| ", " o|ßß|ä|ßß| ", " u|ßß|ä|ßß| ", " v|ßß|chr|ßß| ", " z|ßß|b|ßß| ", " z|ßß|hd|ßß| ", " z|ßß|t|ßß| ", " i|ßß|d|ßß|r|ßß| ", " u|ßß|v|ßß|m|ßß| ", " abschn|ßß| ", " aufl|ßß| ", " bspw|ßß| ", " bzgl|ßß| ", " bzw|ßß| ", " ca|ßß| ", " d|ßß|h|ßß| ", " dr|ßß| ", " etc|ßß| ", " ggf|ßß| ", " grds|ßß| ", " inkl|ßß| ", " insb|ßß| ", " n|ßß|chr|ßß| ", " nr|ßß| ", " prof|ßß| ", " st|ßß| ", " v|ßß|chr|ßß| ", " vgl|ßß| ", " z|ßß|b|ßß| ", " zzgl|ßß| "};
            for (int i=0; i<abbreviationsOld1.length; i++) {
                lines = lines.replaceAll(abbreviationsOld1[i], abbreviationsNew1[i]);
            }
            for (int i=1; i<10; i++) {
                lines = lines.replaceAll("str\\. "+Integer.toString(i), "str|ßß| "+Integer.toString(i));
            }
        }
	    return lines;
	}
	
	private static final String workOnHyphensAndDashes(String lines) {
	/* Saves important hyphens or dashes by temporarily replacing them with "YßßY"; also some
	 * important dots will be saved in a similar way by replacing them with "|ßß|". */
        for (int i=97; i<123; i++) {
            lines = lines.replaceAll(" "+(char)i+"\\.", " "+(char)i+"|ßß|")
                    .replaceAll("\\."+(char)i+"\\.", "|ßß|"+(char)i+"|ßß|")
                    .replaceAll((char)i+"[-––-—­] &", (char)i+"YßßY &")
                    .replaceAll((char)i+"[-––-—­] und", (char)i+"YßßY und")
                    .replaceAll((char)i+"[-––-—­] oder", (char)i+"YßßY oder")
                    .replaceAll((char)i+"[-––-—­], ", (char)i+"YßßY ");
            for (int j=97; j<123; j++) {
                lines = lines.replaceAll((char)i+"[-––-—­]"+(char)j, (char)i+"YßßY"+(char)j);
            }
        }
        for (int i=0; i<10; i++) {
            lines = lines.replaceAll(Integer.toString(i)+"[-––-—­]e", Integer.toString(i)+"YßßYe")
                    .replaceAll(Integer.toString(i)+"[-––-—­]m", Integer.toString(i)+"YßßYm")
                    .replaceAll(Integer.toString(i)+"[-––-—­]n", Integer.toString(i)+"YßßYn")
                    .replaceAll(Integer.toString(i)+"[-––-—­]r", Integer.toString(i)+"YßßYr")
                    .replaceAll(Integer.toString(i)+"[-––-—­]s", Integer.toString(i)+"YßßYs")
                    .replaceAll(Integer.toString(i)+"[-––-—­]t", Integer.toString(i)+"YßßYt");
            for (int j=0; j<10; j++) {
                lines = lines.replaceAll(Integer.toString(i)+"( )?[-––-—­]( )?"+Integer.toString(j), Integer.toString(i)+" YßßY "+Integer.toString(j));
            }
        }
        for (int i=0; i<UMLAUTE.length; i++) {
            lines = lines.replaceAll(UMLAUTE[i]+"[-––-—­] &", UMLAUTE[i]+"YßßY &")
                    .replaceAll(UMLAUTE[i]+"[-––-—­] und", UMLAUTE[i]+"YßßY und")
                    .replaceAll(UMLAUTE[i]+"[-––-—­] oder", UMLAUTE[i]+"YßßY oder")
                    .replaceAll(UMLAUTE[i]+"[-––-—­], ", UMLAUTE[i]+"YßßY ");
            for (int j=0; j<UMLAUTE.length; j++) {
                lines = lines.replaceAll(UMLAUTE[i]+"[-––-—­]"+UMLAUTE[j], UMLAUTE[i]+"YßßY"+UMLAUTE[j]);
            }
        }
        return lines.replaceAll("[-––-—­]", " ");
	}
	
    private static final String replaceTimeFormats(String lines) {
    // Detects several time formats and prepares them for the method "replaceNumbers()".
        final String[] clockString = {"( )?uhr ", "( )?h ", "( )?ce(s)?t ", "( )?me(s)?z "};
        for (int hour=0; hour<24; hour++) {
            for (int minutes0=0; minutes0<6; minutes0++) {
                for (int minutes1=0; minutes1<10; minutes1++) {
                    for (int i=0; i<clockString.length; i++) {
                        lines = lines.replaceAll("[ \\.]"+Integer.toString(hour)+"\\."+Integer.toString(minutes0)+Integer.toString(minutes1)+clockString[i], " "+Integer.toString(hour)+" uhr "+Integer.toString(minutes0)+Integer.toString(minutes1)+" ")
                                .replaceAll("[ \\.]"+Integer.toString(hour)+" XßßX "+Integer.toString(minutes0)+Integer.toString(minutes1)+clockString[i], " "+Integer.toString(hour)+" uhr "+Integer.toString(minutes0)+Integer.toString(minutes1)+" ");
                        if (hour < 10) {
                            lines = lines.replaceAll("[ \\.]0"+Integer.toString(hour)+"\\."+Integer.toString(minutes0)+Integer.toString(minutes1)+clockString[i], " "+Integer.toString(hour)+" uhr "+Integer.toString(minutes0)+Integer.toString(minutes1)+" ")
                                    .replaceAll("[ \\.]0"+Integer.toString(hour)+" XßßX "+Integer.toString(minutes0)+Integer.toString(minutes1)+clockString[i], " "+Integer.toString(hour)+" uhr "+Integer.toString(minutes0)+Integer.toString(minutes1)+" ");
                        }
                    }
                }
            }
        }
        for (int hour0=0; hour0<10; hour0++) {
            for (int hour1=0; hour1<24; hour1++) {
                for (int minutes0=0; minutes0<6; minutes0++) {
                    for (int minutes1=0; minutes1<10; minutes1++) {
                        lines = lines.replaceAll(Integer.toString(hour0)+"\\."+Integer.toString(minutes0)+Integer.toString(minutes1)+" YßßY "+Integer.toString(hour1)+" uhr ", Integer.toString(hour0)+" uhr "+Integer.toString(minutes0)+Integer.toString(minutes1)+" bis "+Integer.toString(hour1)+" uhr ")
                                .replaceAll(Integer.toString(hour0)+" XßßX "+Integer.toString(minutes0)+Integer.toString(minutes1)+" YßßY "+Integer.toString(hour1)+" uhr ", Integer.toString(hour0)+" uhr "+Integer.toString(minutes0)+Integer.toString(minutes1)+" bis "+Integer.toString(hour1)+" uhr ");
                    }
                }
            }
        }
        return lines;
    }
    
    private static final String clarifyDates(String[] tokens) {
    // Helps replacing numeric representations of dates with their corresponding words and phrases.
        int numberOfDots;
        boolean date;
        String[] dateTokens;
        for (int i=0; i<tokens.length; i++) {
            // Counts '.' in "tokens[i]".
            numberOfDots = tokens[i].length()-tokens[i].replaceAll("\\.", "").length();
            /* If there are two dots in "tokens[i]" it can be assumed that "tokens[i]" represents a
             * date, so the program checks if the substring between both dots is a number between 1
             * and 12; if so, this number will be replaced with the name of the month corresponding
             * to this number (e.g. "2" or 02" will be replaced with "februar" (German for
             * "February")). */
            if (numberOfDots == 2) {
                date = false;
                dateTokens = tokens[i].split("\\.");
                switch (dateTokens[1]) {
                case "1":
                case "01":
                    dateTokens[1] = "januar ";
                    date = true;
                    break;
                case "2":
                case "02":
                    dateTokens[1] = "februar ";
                    date = true;
                    break;
                case "3":
                case "03":
                    dateTokens[1] = "märz ";
                    date = true;
                    break;
                case "4":
                case "04":
                    dateTokens[1] = "april ";
                    date = true;
                    break;
                case "5":
                case "05":
                    dateTokens[1] = "mai ";
                    date = true;
                    break;
                case "6":
                case "06":
                    dateTokens[1] = "juni ";
                    date = true;
                    break;
                case "7":
                case "07":
                    dateTokens[1] = "juli ";
                    date = true;
                    break;
                case "8":
                case "08":
                    dateTokens[1] = "august ";
                    date = true;
                    break;
                case "9":
                case "09":
                    dateTokens[1] = "september ";
                    date = true;
                    break;
                case "10":
                    dateTokens[1] = "oktober ";
                    date = true;
                    break;
                case "11":
                    dateTokens[1] = "november ";
                    date = true;
                    break;
                case "12":
                    dateTokens[1] = "dezember ";
                    date = true;
                }
                if (date) {
                    tokens[i] = dateTokens[0]+". "+dateTokens[1];
                    if (dateTokens.length == 3) {
                        tokens[i] += dateTokens[2];
                    }
                }
            } else if (numberOfDots > 3 || tokens[i].length() > 50) { // deletes tokens with more than 3 dots or more than 50 characters
                tokens[i] = "";
            }
        }
        String lines = Arrays.toString(tokens);
        lines = lines.substring(1, lines.length()-1);
        return " "+lines.replaceAll(",", "")+" ";
    }
    
    private static final String replaceNumbers(String lines) {
    /* Replaces numbers with their corresponding German words (e.g. "24" will be replaced with
     * "vierundzwanzig" (German for "twenty-four"), "10mal" will be replaced with "zehnmal" ("ten
     * times"), and "5ter" will be replaced with "fünfter" ("fifth")). */
        lines = lines.replaceAll(" 0+ ", " null ")
                .replaceAll(" 0+YßßY", " null")
                .replaceAll(" 0+e", " nulle")
                .replaceAll(" 0+te", " nullte")
                .replaceAll(" 0+mal ", " nullmal ")
                .replaceAll(" 0*1 ", " eins ")
                .replaceAll(" 0*1mal", " einmal")
                .replaceAll(" 0*1x i", " einmal i")
                .replaceAll(" 0*1x am ", " einmal am ")
                .replaceAll(" 0*1x pro ", " einmal pro ")
                .replaceAll(" 11 ", " elf ")
                .replaceAll(" 11YßßY", " elf")
                .replaceAll(" 11e", " elfe")
                .replaceAll(" 11te", " elfte")
                .replaceAll(" 11mal", " elfmal")
                .replaceAll(" 11x i", " elfmal i")
                .replaceAll(" 11x am ", " elfmal am ")
                .replaceAll(" 11x pro ", " elfmal pro ")
                .replaceAll(" 12 ", " zwölf ")
                .replaceAll(" 12YßßY", " zwölf")
                .replaceAll(" 12e", " zwölfe")
                .replaceAll(" 12te", " zwölfte")
                .replaceAll(" 12mal", " zwölfmal")
                .replaceAll(" 12x i", " zwölfmal i")
                .replaceAll(" 12x am ", " zwölfmal am ")
                .replaceAll(" 12x pro ", " zwölfmal pro ")
                .replaceAll(" 1(\\.)?000(\\.)?000 ", " eine Million ");
        final String[] decades = {"zehn", "zwanzig", "dreißig", "vierzig", "fünfzig", "sechzig", "siebzig", "achtzig", "neunzig"};
        final String[] ordinalNumbers = {"erst", "zweit", "dritt", "viert", "fünft", "sechst", "siebt", "acht", "neunt"};
        final String[][] preChars = {{" am ", " an der ", " an dieser ", " den ", " des ", "einen ", "em ", " in der ", " in dieser ", "nes ", " seit ", " vom ", " zum "}, {" das ", " der ", " die ", " dieser ", " dieses ", " jede ", " jeder ", " jedes "}};
        final String[] caseMarkers = {"n ", " "};
        for (int i=0; i<MONTHS.length; i++) {
            if (i < caseMarkers.length) {
                for (int j=0; j<preChars[i].length; j++) {
                    lines = lines.replaceAll(preChars[i][j]+"11\\. ", preChars[i][j]+"elfte"+caseMarkers[i])
                            .replaceAll(preChars[i][j]+"12\\. ", preChars[i][j]+"zwölfte"+caseMarkers[i]);
                }
            }
            for (int j=0; j<MONTHS[i].length; j++) {
                lines = lines.replaceAll(" 11\\. "+MONTHS[i][j], " elfter "+MONTHS[i][0])
                        .replaceAll(" 12\\. "+MONTHS[i][j], " zwölfter "+MONTHS[i][0]);
            }
        }
        for (int i=1; i<100; i++) {
            if (i < 10) {
                for (int j=0; j<caseMarkers.length; j++) {
                    for (int k=0; k<preChars[j].length; k++) {
                        lines = lines.replaceAll(preChars[j][k]+"0*"+Integer.toString(i)+"\\. ", preChars[j][k]+ordinalNumbers[i-1]+"e"+caseMarkers[j]);
                        if (i != 1) {
                            lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"0\\. ", preChars[j][k]+decades[i-1]+"ste"+caseMarkers[j]);
                        } else {
                            lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"0\\. ", preChars[j][k]+decades[i-1]+"te"+caseMarkers[j]);
                        }
                    }
                }
                for (int j=0; j<MONTHS.length; j++) {
                    for (int k=0; k<MONTHS[j].length; k++) {
                        lines = lines.replaceAll(" (0)?"+Integer.toString(i)+"\\. "+MONTHS[j][k], " "+ordinalNumbers[i-1]+"er "+MONTHS[j][0]);
                        if (i == 1) {
                            lines = lines.replaceAll(" "+Integer.toString(i)+"0\\. "+MONTHS[j][k], " "+decades[i-1]+"ter "+MONTHS[j][0]);
                        } else if (i == 2 || i == 3){
                            lines = lines.replaceAll(" "+Integer.toString(i)+"0\\. "+MONTHS[j][k], " "+decades[i-1]+"ster "+MONTHS[j][0]);
                        }
                    }
                }
                lines = lines.replaceAll(" "+Integer.toString(i)+"0 ", " "+decades[i-1]+" ")
                        .replaceAll(" "+Integer.toString(i)+"0te", " "+decades[i-1]+"te")
                        .replaceAll(" "+Integer.toString(i)+"0e", " "+decades[i-1]+"e")
                        .replaceAll(" "+Integer.toString(i)+"0YßßYt", " "+decades[i-1]+"t")
                        .replaceAll(" "+Integer.toString(i)+"0mal", " "+decades[i-1]+"mal")
                        .replaceAll(" "+Integer.toString(i)+"0x i", " "+decades[i-1]+"mal i")
                        .replaceAll(" "+Integer.toString(i)+"0x am ", " "+decades[i-1]+"mal am ")
                        .replaceAll(" "+Integer.toString(i)+"0x pro ", " "+decades[i-1]+"mal pro ")
                        .replaceAll(" "+Integer.toString(i)+"00 ", " "+FIRST_PART_OF_NUMERAL[i-1]+"hundert ")
                        .replaceAll(" "+Integer.toString(i)+"00st", " "+FIRST_PART_OF_NUMERAL[i-1]+"hundertst")
                        .replaceAll(" "+Integer.toString(i)+"00e", " "+FIRST_PART_OF_NUMERAL[i-1]+"hunderte")
                        .replaceAll(" "+Integer.toString(i)+"00mal", " "+FIRST_PART_OF_NUMERAL[i-1]+"hundertmal")
                        .replaceAll(" "+Integer.toString(i)+"00YßßY", " "+FIRST_PART_OF_NUMERAL[i-1]+"hundert")
                        .replaceAll(" "+Integer.toString(i)+"(\\.)?000 ", " "+FIRST_PART_OF_NUMERAL[i-1]+"tausend ")
                        .replaceAll(" "+Integer.toString(i)+"(\\.)?000mal", " "+FIRST_PART_OF_NUMERAL[i-1]+"tausendmal")
                        .replaceAll(" "+Integer.toString(i)+"(\\.)?000st", " "+FIRST_PART_OF_NUMERAL[i-1]+"tausendst")
                        .replaceAll(" "+Integer.toString(i)+"(\\.)?000e", " "+FIRST_PART_OF_NUMERAL[i-1]+"tausende")
                        .replaceAll(" "+Integer.toString(i)+"(\\.)?000YßßY", " "+FIRST_PART_OF_NUMERAL[i-1]+"tausend")
                        .replaceAll(" "+Integer.toString(i)+"0(\\.)?000 ", " "+decades[i-1]+"tausend ")
                        .replaceAll(" "+Integer.toString(i)+"00(\\.)?000 ", " "+FIRST_PART_OF_NUMERAL[i-1]+"hunderttausend ")
                        .replaceAll(" "+Integer.toString(i)+"0(\\.)?000(\\.)?000 ", " "+decades[i-1]+" Millionen ");
            }
            if (i > 1 && i < 10) {
                lines = lines.replaceAll(" "+Integer.toString(i)+"0(YßßY)?st", " "+decades[i-1]+"st")
                        .replaceAll(" "+Integer.toString(i)+"(\\.)?000(\\.)?000 ", " "+FIRST_PART_OF_NUMERAL[i-1]+" Millionen ")
                        .replaceAll(" 0*"+Integer.toString(i)+" ", " "+FIRST_PART_OF_NUMERAL[i-1]+" ")
                        .replaceAll(" "+Integer.toString(i)+"e", " "+FIRST_PART_OF_NUMERAL[i-1]+"e")
                        .replaceAll(" "+Integer.toString(i)+"mal", " "+FIRST_PART_OF_NUMERAL[i-1]+"mal")
                        .replaceAll(" "+Integer.toString(i)+"x i", " "+FIRST_PART_OF_NUMERAL[i-1]+"mal i")
                        .replaceAll(" "+Integer.toString(i)+"x am ", " "+FIRST_PART_OF_NUMERAL[i-1]+"mal am ")
                        .replaceAll(" "+Integer.toString(i)+"x pro ", " "+FIRST_PART_OF_NUMERAL[i-1]+"mal pro ");
                switch (i) {
                case 3:
                    lines = lines.replaceAll(" "+Integer.toString(i)+"(YßßY)?te", " dritte")
                            .replaceAll(" "+Integer.toString(i)+"YßßYe", " "+FIRST_PART_OF_NUMERAL[i-1]+"e");
                    break;
                case 7:
                    lines = lines.replaceAll(" "+Integer.toString(i)+"(YßßY)?te", " siebte")
                            .replaceAll(" "+Integer.toString(i)+"YßßYe", " "+FIRST_PART_OF_NUMERAL[i-1]+"e");
                    break;
                default:
                    lines = lines.replaceAll(" "+Integer.toString(i)+"te", " "+FIRST_PART_OF_NUMERAL[i-1]+"te")
                            .replaceAll(" "+Integer.toString(i)+"YßßY", " "+FIRST_PART_OF_NUMERAL[i-1]);
                }
            } else if (i > 12 && i < 20) {
                if (i != 17) {
                    for (int j=0; j<caseMarkers.length; j++) {
                        for (int k=0; k<preChars[j].length; k++) {
                            lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"\\. ", preChars[j][k]+FIRST_PART_OF_NUMERAL[i-11]+"zehnte"+caseMarkers[j]);
                        }
                    }
                    for (int j=0; j<MONTHS.length; j++) {
                        for (int k=0; k<MONTHS[j].length; k++) {
                            lines = lines.replaceAll(" "+Integer.toString(i)+"\\. "+MONTHS[j][k], " "+FIRST_PART_OF_NUMERAL[i-11]+"zehnter "+MONTHS[j][0]);
                        }
                    }
                    lines = lines.replaceAll(" "+Integer.toString(i)+" ", " "+FIRST_PART_OF_NUMERAL[i-11]+"zehn ")
                            .replaceAll(" "+Integer.toString(i)+"te", FIRST_PART_OF_NUMERAL[i-11]+"zehnte")
                            .replaceAll(" "+Integer.toString(i)+"YßßY", " "+FIRST_PART_OF_NUMERAL[i-11]+"zehn")
                            .replaceAll(" "+Integer.toString(i)+"e", " "+FIRST_PART_OF_NUMERAL[i-11]+"zehne")
                            .replaceAll(" "+Integer.toString(i)+"mal", " "+FIRST_PART_OF_NUMERAL[i-11]+"mal")
                            .replaceAll(" "+Integer.toString(i)+"x i", " "+FIRST_PART_OF_NUMERAL[i-11]+"mal i")
                            .replaceAll(" "+Integer.toString(i)+"x am ", " "+FIRST_PART_OF_NUMERAL[i-11]+"mal am ")
                            .replaceAll(" "+Integer.toString(i)+"x pro ", " "+FIRST_PART_OF_NUMERAL[i-11]+"mal pro ");
                } else {
                    for (int j=0; j<caseMarkers.length; j++) {
                        for (int k=0; k<preChars[j].length; k++) {
                            lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"\\. ", preChars[j][k]+"siebzehnte"+caseMarkers[j]);
                        }
                    }
                    for (int j=0; j<MONTHS.length; j++) {
                        for (int k=0; k<MONTHS[j].length; k++) {
                            lines = lines.replaceAll(" "+Integer.toString(i)+"\\. "+MONTHS[j][k], " siebzehnter "+MONTHS[j][0]);
                        }
                    }
                    lines = lines.replaceAll(" "+Integer.toString(i)+" ", " siebzehn ")
                            .replaceAll(" "+Integer.toString(i)+"te", " siebzehnte")
                            .replaceAll(" "+Integer.toString(i)+"YßßY", " siebzehn")
                            .replaceAll(" "+Integer.toString(i)+"e", " siebzehne")
                            .replaceAll(" "+Integer.toString(i)+"mal", " siebzehnmal")
                            .replaceAll(" "+Integer.toString(i)+"x i", " siebzehnmal i")
                            .replaceAll(" "+Integer.toString(i)+"x am ", " siebzehnmal am ")
                            .replaceAll(" "+Integer.toString(i)+"x pro ", " siebzehnmal pro ");
                }
            } else if (i > 20 && i < 30) {
                for (int j=0; j<caseMarkers.length; j++) {
                    for (int k=0; k<preChars[j].length; k++) {
                        lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"\\. ", preChars[j][k]+FIRST_PART_OF_NUMERAL[i-21]+"undzwanzigste"+caseMarkers[j]);
                    }
                }
                for (int j=0; j<MONTHS.length; j++) {
                    for (int k=0; k<MONTHS[j].length; k++) {
                        lines = lines.replaceAll(" "+Integer.toString(i)+"\\. "+MONTHS[j][k], " "+FIRST_PART_OF_NUMERAL[i-21]+"undzwanzigster "+MONTHS[j][0]);
                    }
                }
                lines = lines.replaceAll(" "+Integer.toString(i)+" ", " "+FIRST_PART_OF_NUMERAL[i-21]+"undzwanzig ")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?st", " "+FIRST_PART_OF_NUMERAL[i-21]+"undzwanzigst")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?e", " "+FIRST_PART_OF_NUMERAL[i-21]+"undzwanzige")
                        .replaceAll(" "+Integer.toString(i)+"mal", " "+FIRST_PART_OF_NUMERAL[i-21]+"undzwanzigmal")
                        .replaceAll(" "+Integer.toString(i)+"x i", " "+FIRST_PART_OF_NUMERAL[i-21]+"undzwanzigmal i")
                        .replaceAll(" "+Integer.toString(i)+"x am ", " "+FIRST_PART_OF_NUMERAL[i-21]+"undzwanzigmal am ")
                        .replaceAll(" "+Integer.toString(i)+"x pro ", " "+FIRST_PART_OF_NUMERAL[i-21]+"undzwanzigmal pro ");
            } else if (i > 30 && i < 40) {
                for (int j=0; j<caseMarkers.length; j++) {
                    for (int k=0; k<preChars[j].length; k++) {
                        lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"\\. ", preChars[j][k]+FIRST_PART_OF_NUMERAL[i-31]+"unddreißigste"+caseMarkers[j]);
                    }
                }
                if (i < 32) { // "i < 32" because there are no months with more than 31 days
                    for (int j=0; j<MONTHS.length; j++) {
                        if (j != 1) { // "j != 1" because february (which corresponds to "month[1]") has never got more than 29 days
                            for (int k=0; k<MONTHS[j].length; k++) {
                            lines = lines.replaceAll(" "+Integer.toString(i)+"\\. "+MONTHS[j][k], " "+FIRST_PART_OF_NUMERAL[i-31]+"unddreißigster "+MONTHS[j][0]);
                            }
                        }
                    }
                }
                lines = lines.replaceAll(" "+Integer.toString(i)+" ", " "+FIRST_PART_OF_NUMERAL[i-31]+"unddreißig ")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?st", " "+FIRST_PART_OF_NUMERAL[i-31]+"unddreißigst")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?e", " "+FIRST_PART_OF_NUMERAL[i-31]+"unddreißige")
                        .replaceAll(" "+Integer.toString(i)+"mal", " "+FIRST_PART_OF_NUMERAL[i-31]+"unddreißigmal")
                        .replaceAll(" "+Integer.toString(i)+"x i", " "+FIRST_PART_OF_NUMERAL[i-31]+"unddreißigmal i")
                        .replaceAll(" "+Integer.toString(i)+"x am ", " "+FIRST_PART_OF_NUMERAL[i-31]+"unddreißigmal am ")
                        .replaceAll(" "+Integer.toString(i)+"x pro ", " "+FIRST_PART_OF_NUMERAL[i-31]+"unddreißigmal pro ");
            } else if (i > 40 && i < 50) {
                for (int j=0; j<caseMarkers.length; j++) {
                    for (int k=0; k<preChars[j].length; k++) {
                        lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"\\. ", preChars[j][k]+FIRST_PART_OF_NUMERAL[i-41]+"undvierzigste"+caseMarkers[j]);
                    }
                }
                lines = lines.replaceAll(" "+Integer.toString(i)+" ", " "+FIRST_PART_OF_NUMERAL[i-41]+"undvierzig ")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?st", " "+FIRST_PART_OF_NUMERAL[i-41]+"undvierzigst")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?e", " "+FIRST_PART_OF_NUMERAL[i-41]+"undvierzige")
                        .replaceAll(" "+Integer.toString(i)+"mal", " "+FIRST_PART_OF_NUMERAL[i-41]+"undvierzigmal")
                        .replaceAll(" "+Integer.toString(i)+"x i", " "+FIRST_PART_OF_NUMERAL[i-41]+"undvierzigmal i")
                        .replaceAll(" "+Integer.toString(i)+"x am ", " "+FIRST_PART_OF_NUMERAL[i-41]+"undvierzigmal am ")
                        .replaceAll(" "+Integer.toString(i)+"x pro ", " "+FIRST_PART_OF_NUMERAL[i-41]+"undvierzigmal pro ");
            } else if (i > 50 && i < 60) {
                for (int j=0; j<caseMarkers.length; j++) {
                    for (int k=0; k<preChars[j].length; k++) {
                        lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"\\. ", preChars[j][k]+FIRST_PART_OF_NUMERAL[i-51]+"undfünfzigste"+caseMarkers[j]);
                    }
                }
                lines = lines.replaceAll(" "+Integer.toString(i)+" ", " "+FIRST_PART_OF_NUMERAL[i-51]+"undfünfzig ")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?st", " "+FIRST_PART_OF_NUMERAL[i-51]+"undfünfzigst")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?e", " "+FIRST_PART_OF_NUMERAL[i-51]+"undfünfzige")
                        .replaceAll(" "+Integer.toString(i)+"mal", " "+FIRST_PART_OF_NUMERAL[i-51]+"undfünfzigmal");
            } else if (i > 60 && i < 70) {
                for (int j=0; j<caseMarkers.length; j++) {
                    for (int k=0; k<preChars[j].length; k++) {
                        lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"\\. ", preChars[j][k]+FIRST_PART_OF_NUMERAL[i-61]+"undsechzigste"+caseMarkers[j]);
                    }
                }
                lines = lines.replaceAll(" "+Integer.toString(i)+" ", " "+FIRST_PART_OF_NUMERAL[i-61]+"undsechzig ")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?st", " "+FIRST_PART_OF_NUMERAL[i-61]+"undsechzigst")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?e", " "+FIRST_PART_OF_NUMERAL[i-61]+"undsechzige")
                        .replaceAll(" "+Integer.toString(i)+"mal", " "+FIRST_PART_OF_NUMERAL[i-61]+"undsechzigmal");
            } else if (i > 70 && i < 80) {
                for (int j=0; j<caseMarkers.length; j++) {
                    for (int k=0; k<preChars[j].length; k++) {
                        lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"\\. ", preChars[j][k]+FIRST_PART_OF_NUMERAL[i-71]+"undsiebzigste"+caseMarkers[j]);
                    }
                }
                lines = lines.replaceAll(" "+Integer.toString(i)+" ", " "+FIRST_PART_OF_NUMERAL[i-71]+"undsiebzig ")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?st", " "+FIRST_PART_OF_NUMERAL[i-71]+"undsiebzigst")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?e", " "+FIRST_PART_OF_NUMERAL[i-71]+"undsiebzige")
                        .replaceAll(" "+Integer.toString(i)+"mal", " "+FIRST_PART_OF_NUMERAL[i-71]+"undsiebzigmal");
            } else if (i > 80 && i < 90) {
                for (int j=0; j<caseMarkers.length; j++) {
                    for (int k=0; k<preChars[j].length; k++) {
                        lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"\\. ", preChars[j][k]+FIRST_PART_OF_NUMERAL[i-81]+"undachtzigste"+caseMarkers[j]);
                    }
                }
                lines = lines.replaceAll(" "+Integer.toString(i)+" ", " "+FIRST_PART_OF_NUMERAL[i-81]+"undachtzig ")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?st", " "+FIRST_PART_OF_NUMERAL[i-81]+"undachtzigst")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?e", " "+FIRST_PART_OF_NUMERAL[i-81]+"undachtzige")
                        .replaceAll(" "+Integer.toString(i)+"mal", " "+FIRST_PART_OF_NUMERAL[i-81]+"undachtzigmal");
            } else if (i > 90) {
                for (int j=0; j<caseMarkers.length; j++) {
                    for (int k=0; k<preChars[j].length; k++) {
                        lines = lines.replaceAll(preChars[j][k]+Integer.toString(i)+"\\. ", preChars[j][k]+FIRST_PART_OF_NUMERAL[i-91]+"undneunzigste"+caseMarkers[j]);
                    }
                }
                lines = lines.replaceAll(" "+Integer.toString(i)+" ", " "+FIRST_PART_OF_NUMERAL[i-91]+"undneunzig ")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?st", " "+FIRST_PART_OF_NUMERAL[i-91]+"undneunzigst")
                        .replaceAll(" "+Integer.toString(i)+"(YßßY)?e", " "+FIRST_PART_OF_NUMERAL[i-91]+"undneunzige")
                        .replaceAll(" "+Integer.toString(i)+"mal", " "+FIRST_PART_OF_NUMERAL[i-91]+"undneunzigmal");
            }
        }
        return lines;
    }
    
    private static final String detectYears(String lines) {
    /* Detects numbers that represent a certain year by checking "lines" for "yearMarkers" and
     * names of months in front of the number. If such a number is found, it will be replaced with
     * the corresponding words for the year (e.g. "1975" will be replaced with
     * "neunzehnhundertfünfundsiebzig" (German for "nineteen seventy-five")). */
        int year;
        final String[] yearMarkers = {"jahr ", "jahre ", "jahren ", "jahres ", "jahrs ", "frühjahr ", "frühling ", "sommer ", "herbst ", "winter ", "ostern ", "pfingsten ", "weihnachten ", "semester ", "geboren ", "gestorben "};
        for (int century=1; century<21; century++) {
            for (int i=0; i<100; i++) {
                year = i+century*100;
                for (int j=0; j<yearMarkers.length; j++) {
                    lines = replaceYears(lines, yearMarkers[j], year, century);
                }
                for (int j=0; j<MONTHS.length; j++) {
                    lines = replaceYears(lines, MONTHS[j][0], year, century);
                }
            }
        }
        return lines;
    }
    
    private static final String replaceYears(String lines, final String start, final int year, final int century) {
    /* Replaces the numbers that represent a certain year (these numbers have to be detected by the
     * method "detectYears" before).
     * Note that the numbers between 0 and 100 that have been replaced by the method
     * "replaceNumbers()" are already replaced with correct German expressions for years as well. */
        lines = lines.replaceAll(start+Integer.toString(year)+"\\. ", start+Integer.toString(year)+" "); // detects years at the end of a sentence and deletes the '.' behind them
        final String[] centuries = {"einhundert", "zweihundert", "dreihundert", "vierhundert", "fünfhundert", "sechshundert", "siebenhundert", "achthundert", "neunhundert", "eintausend", "elfhundert", "zwölfhundert", "dreizehnhundert", "vierzehnhundert", "fünfzehnhundert", "sechzehnhundert", "siebzehnhundert", "achtzehnhundert", "neunzehnhundert", "zweitausend"};
        if (year == (century*100)) {
            return (year > 1099 && year != 2000)? lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+" ") : lines;
        } else if (year == (century*100+1)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"eins ");
        } else if (year > (century*100+1) && year < (century*100+10)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+FIRST_PART_OF_NUMERAL[(year-century*100)-1]+" ");
        } else if (year == (century*100+10)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"zehn ");
        } else if (year == (century*100+11)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"elf ");
        } else if (year == (century*100+12)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"zwölf ");
        } else if (year < (century*100+20)) {
            // Note that there is a difference whether "year" ends with "17" or not!
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+(year != (century*100+17)? FIRST_PART_OF_NUMERAL[(year-century*100)-11]+"zehn " : "siebzehn "));
        } else if (year == (century*100+20)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"zwanzig ");
        } else if (year < (century*100+30)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+FIRST_PART_OF_NUMERAL[(year-century*100)-21]+"undzwanzig ");
        } else if (year == (century*100+30)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"dreißig ");
        } else if (year < (century*100+40)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+FIRST_PART_OF_NUMERAL[(year-century*100)-31]+"unddreißig ");
        } else if (year == (century*100+40)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"vierzig ");
        } else if (year < (century*100+50)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+FIRST_PART_OF_NUMERAL[(year-century*100)-41]+"undvierzig ");
        } else if (year == (century*100+50)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"fünfzig ");
        } else if (year < (century*100+60)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+FIRST_PART_OF_NUMERAL[(year-century*100)-51]+"undfünfzig ");
        } else if (year == (century*100+60)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"sechzig ");
        } else if (year < (century*100+70)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+FIRST_PART_OF_NUMERAL[(year-century*100)-61]+"undsechzig ");
        } else if (year == (century*100+70)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"siebzig ");
        } else if (year < (century*100+80)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+FIRST_PART_OF_NUMERAL[(year-century*100)-71]+"undsiebzig ");
        } else if (year == (century*100+80)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"achtzig ");
        } else if (year < (century*100+90)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+FIRST_PART_OF_NUMERAL[(year-century*100)-81]+"undachtzig ");
        } else if (year == (century*100+90)) {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+"neunzig ");
        } else  {
            return lines.replaceAll(start+Integer.toString(year)+" ", start+centuries[century-1]+FIRST_PART_OF_NUMERAL[(year-century*100)-91]+"undneunzig ");
        }
    }
    
    private static final boolean checkIfLinesStartWithLetterOrNumber(final String lines) {
    /* Returns "true" if the "lines" start with a letter or a number and "false" otherwise. (The
     * characters in the array "acceptableChars" are sorted in order of their frequency
     * (starting with the most frequent letter to appear in initial position in German words - this
     * should decrease the average running time of this method).) */
        final String[] acceptableChars = {"d", "s", "e", "i", "w", "a", "m", "h", "g", "u", "b", "n", "k", "f", "z", "v", "l", "r", "t", "p", "o", "j", "ü", "ä", "c", "ö", "y", "q", "x", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        for (String acceptableChar : acceptableChars) {
            if (lines.startsWith(acceptableChar)) {
                return true;
            }
        }
        return false;
    }
}
