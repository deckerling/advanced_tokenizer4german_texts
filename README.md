# *advanced_tokenizer4german_texts*
An extended whitespace tokenizer for German text files and websites.

## Overview:
The program provides:
* a whitespace [tokenizer for German texts](src/tokenizer4germantexts/Tokenizer.java) that deletes punctuation marks etc.,
* a [tool to download and tokenize websites](src/tokenizer4germantexts/Webloader.java) (which deletes HTML tags as well),
* an (optional) "extended tokenization" feature that
    * clarifies abbreviations ("z.B." becomes "zum beispiel"),
    * transfers numbers into words ("14" becomes "vierzehn"), and
    * clarifies time and date formats ("12:30 Uhr" becomes "zwölf uhr dreißig", "03.01.2019" becomes "dritter januar zweitausendneunzehn").

*advanced_tokenizer4german_texts* is a whitespace tokenizer for German texts coming with a tool to download and tokenize websites. It also provides an "extended tokenization" feature, which clarifies certain abbreviations (e.g. "z.B." becomes "zum beispiel") and transfers numbers into words (e.g. "14" becomes "vierzehn"). Furthermore, certain time and date formats will be clarified as well (e.g. "12:30 Uhr" becomes "zwölf uhr dreißig" and "03.01.2019" becomes "dritter januar zweitausendneunzehn"). If you do not want or do not need such changes for your purpose, you can simply use the "standard" tokenization feature.  
Be aware that the "extended tokenization" feature will take some time to convert the numbers and abbreviations (especially the work on numerals is expensive).

This tokenizer can be used, for example, to provide token files as training data for neural networks to train word vectors.

## Options
*advanced_tokenizer4german_texts* offers three options:
You can tokenize
1. a single file or website,
2. all files in a certain directory, or
3. several files or websites by using a text file containing the paths or URLs to the files and websites you want to tokenize (it is also possible to save paths of directories in such a file so the program will tokenize all text files in all of those directories).  
In case you want to tokenize files that are UTF-8 encoded, it is possible to select UTF-8 coding. Furthermore, you can use a standard or an extended tokenization (as described above).

## Examples
If you use the "extended tokenization" feature the following text ...

> Es war bspw. am 15.1.2019, als ich – es muss ca. -20°C gehabt haben! – mit meinem Auto, das ich bzw. meine Frau im Sommer 2008 erstanden hatte, rund 300km von meiner Heimat entfernt mit 47 km/h geblitzt wurde; dabei bin ich mir sicher, dass da ein 50er-Bereich war!!! Weil mir so etwas nun schon zum 3ten Mal passierte und ich nicht wollte, dass 4-mal daraus werden, beschloss ich – wie Hannibal um 200 v. Chr. gegen Rom – dagegen vorzugehen, ggf. auch dann, wenn mir meine Familie und Freunde etc. davon abrieten. Doch als ich mich bei der Behörde beschweren wollte, stellte ich fest, dass sie nur von 09.30-12.00h geöffnet hatte…

... will be tokenized to ...

> es war beispielsweise am fünfzehnten januar zweitausendneunzehn als ich es muss circa minus zwanzig grad celsius gehabt haben mit meinem auto das ich beziehungsweise meine frau im sommer zweitausendacht erstanden hatte rund dreihundert kilometer von meiner heimat entfernt mit siebenundvierzig kilometer pro stunde geblitzt wurde dabei bin ich mir sicher dass da ein fünfziger-bereich war weil mir so etwas nun schon zum dritten mal passierte und ich nicht wollte dass viermal daraus werden beschloss ich wie hannibal um zweihundert vor christus gegen rom dagegen vorzugehen gegebenenfalls auch dann wenn mir meine familie und freunde et cetera davon abrieten doch als ich mich bei der behörde beschweren wollte stellte ich fest dass sie nur von neun uhr dreißig bis zwölf uhr geöffnet hatte

If you use the "standard" tokenization the output will be ...

> es war bspw. am 15.1.2019 als ich es muss ca. -20 °c gehabt haben mit meinem auto das ich bzw. meine frau im sommer 2008 erstanden hatte rund 300 km von meiner heimat entfernt mit 47 kmh geblitzt wurde dabei bin ich mir sicher dass da ein 50er-bereich war weil mir so etwas nun schon zum 3ten mal passierte und ich nicht wollte dass 4-mal daraus werden beschloss ich wie hannibal um 200 v.chr. gegen rom dagegen vorzugehen ggf. auch dann wenn mir meine familie und freunde etc. davon abrieten doch als ich mich bei der behörde beschweren wollte stellte ich fest dass sie nur von 09.30 - 12.00h geöffnet hatte

Note that not all abbreviations or numbers will be clarified (for example, "312" will remain "312" and "s.o." will remain "s.o." (so the dots won’t be deleted here)). Especially ambiguous abbreviations won’t be changed in order to avoid errors. Nevertheless, all of the replacements done by the program are based on probabilities: In some cases errors may occur, but, as this only occurs rarely, those errors will hardly affect your later work (especially if you are working with a large amount of data and neural networks).

## Connect your token files with *text_file_connecter.py*
*text_file_connecter.py* is a simple tool to create a single token file out of all token files (or rather text files in general) in a directory by connecting their contents and saving them in a new file.

## License
The work contained in this package is licensed under the Apache License, Version 2.0 (see the file "[LICENSE](LICENSE)").
