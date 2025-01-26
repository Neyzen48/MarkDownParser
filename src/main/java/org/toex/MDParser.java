package org.toex;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MDParser {

    private final Pattern hPattern = Pattern.compile("^(\\s{0,3}#+) +(.*)");
    private final Pattern olPattern = Pattern.compile("^(\\s{0,3}\\d\\s).*");
    private final Pattern ulPattern = Pattern.compile("^(\\s{0,3}[-+*]\\s).*"); //
    private final Pattern tb1Pattern = Pattern.compile("(?m)^( {0,3}-+ *)(?!.)");
    private final Pattern boldPattern = Pattern.compile(""); // TO DO

    private final Pattern doubleNLPattern = Pattern.compile("(?m)(\\s*\\n){2,}");
    private final Pattern continuePattern = Pattern.compile("(?m)^(?!`{3})(.+)\\n(?!\\s*[\\-#*+]\\s)(?!\\s*\\d[.)]\\s)(?!\\s*>)(?!`{3})(.+)"); // Tested
    private final Pattern blockCodePattern = Pattern.compile("(?m)^ {0,3}`{3} *(.*)\\n((?:.*|\\n)+)\\n {0,3}`{3,}"); // Tested

    int inList = 0;

    private String mergeLines(String mdText) { // Tested
        while(continuePattern.matcher(mdText).find()) { // finds any line that is not a new paragraph
            mdText = continuePattern.matcher(mdText).replaceFirst("$1 $2"); // and merges them, hope not merging something it shouldn't
        }
        return doubleNLPattern.matcher(mdText).replaceAll("\n"); // removes all empty lines and returns it back
    }

    private String precompile(String mdText) { // Tested
        System.out.println(mdText); // DEBUG CODE
        StringBuilder markdown = new StringBuilder(); // string builder for markdown simplify process
        Matcher blockCodeMatcher = blockCodePattern.matcher(mdText); // matcher for code block
        int lastEnd = 0; // last string position of code block
        while(blockCodeMatcher.find()) { // find code blocks
            markdown.append(mergeLines(mdText.substring(lastEnd, blockCodeMatcher.start()))) // change code blocks and adds them into new markdown
                    .append(blockCodeMatcher.group()); // don't change code blocks and adds them into new markdown
            lastEnd = blockCodeMatcher.end(); // reset last end position of code block
        }
        return markdown.append(mergeLines(mdText.substring(lastEnd))).toString(); // add left markdown after code block and return it back
    }

    public HTMLElement compile(String md) { // Tested
        HTMLElement html = new HTMLElement(); // creates a new html tree
        String precompiledMarkdown = precompile(md); // run precompiler to get rid of empty lines and merge continuously paragraphs
        System.out.println("\n----------------------------------------------------------------\n"); // DEBUG CODE
        System.out.println(precompiledMarkdown); // DEBUG CODE
        System.out.println("\n----------------------------------------------------------------\n"); // DEBUG CODE
        parseMarkdown(html, precompiledMarkdown);  // Starts to parse, html tree will be built
        return html; // returns built tree back to
    }

    public int getIndent(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                indent++;
            } else {
                break;
            }
        }
        return indent;
    }

    public boolean parseOL(HTMLElement html, ListIterator<String> i, int indent) {
        String line = i.next();
        if(indent == 0) {
            indent = getIndent(line);
        }
        if (olPattern.matcher(line).find()) {
            inList++;
            HTMLElement ol = new HTMLElement("ol", (HTMLElement) null);
            HTMLElement li = new HTMLElement("li", (HTMLElement) null);
            try {
                ol.add(li);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            parseMarkdown(li, i, indent);

        }
        i.previous();
        return false;
    }

    private void parseMarkdown(HTMLElement html, String md) {
        List lines = Arrays.asList(md.split("\n")); // separates markdown into lines
        ListIterator<String> lineIterator = lines.listIterator(); // initializes iterator to iterate between lines
        parseMarkdown(html, lineIterator, 0); // calls recursive version of this parser function to build html
    }

    private void parseMarkdown(HTMLElement html, ListIterator<String> i, int indent) {
        if(i.hasNext()) {
            if(parseHeader(html, i)) { // if next line is header then parse it
                parseMarkdown(html, i, indent); // if something parsed then continue to parse next lines
            } else if(parseOL(html, i, indent)) { // if next line is the beginning of a list then parse it
                parseMarkdown(html, i, indent); // if something parsed then continue to parse next lines
            } else parseParagraph(html, i);
            parseMarkdown(html, i, indent); // if something parsed then continue to parse next lines
        }
    }

    private void parseParagraph(HTMLElement html, ListIterator<String> i) {
        String line = i.next();
        try {
            HTMLElement textContent = new HTMLElement(line);
            if (inList == 0) {
                HTMLElement p = new HTMLElement("p", textContent);
                html.add(p);
            } else {
                html.add(textContent);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean parseHeader(HTMLElement html, ListIterator<String> i) {
        String text = i.next(); // get next line
        Matcher hMatcher = hPattern.matcher(text); // initializes the header matcher for md line
        if(hMatcher.find()) { // if given line is a header
            try {
                HTMLElement h = new HTMLElement("h" + hMatcher.group(1).length(), hMatcher.group(2)); // initializes header html element
                html.add(h); // add header in to html tree
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true; // return true to markdownParser() function back
        } else if(i.hasNext()) { // if no header is found
            if(tb1Pattern.matcher(i.next()).find()) { // then look if the next line contains horizontal line
                try {
                    html.add(new HTMLElement("h1", text.trim())); // add header into html tree
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
            i.previous(); // if no header with horizontal line is found then go to previous line
        }
        i.previous(); // if no header with # found then go to previous line
        return false;
    }
}
