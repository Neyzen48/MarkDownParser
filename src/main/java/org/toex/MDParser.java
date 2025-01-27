package org.toex;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MDParser {

    private final Pattern hdrPattern = Pattern.compile("^( {0,3}#{1,6}) +(.*)");
    private final Pattern oliPattern = Pattern.compile("^ *(\\d)[.)] (.*)");
    private final Pattern uliPattern = Pattern.compile("^ *([-+*]) (.*)"); //
    private final Pattern tb1Pattern = Pattern.compile("(?m)^( {0,3}-= *)(?!.)");
    private final Pattern bldPattern = Pattern.compile("\\*{2}([^ ].+?[^ ])\\*{2}"); // TO DO
    private final Pattern itlPattern = Pattern.compile("[^\\*]\\*([^\\*].+?[^\\*])\\*[^\\*]"); // TO DO
    private final Pattern lnkPattern = Pattern.compile("[^!]\\[(.*?)\\]\\((.*?)\\)"); // TO DO
    private final Pattern imgPattern = Pattern.compile("\\!\\[(.*?)\\]\\((.*?)\\)"); // TO DO
    private final Pattern quoPattern = Pattern.compile(""); // TO DO
    private final Pattern doubleNLPattern = Pattern.compile("(?m)(\\s*\\n){2,}");
    private final Pattern continuePattern = Pattern.compile("(?m)^(?!`{3})(?!\\s*#{1,6}\\s)(.+)\\n(?!\\s*(?:[\\-*+]|\\#{1,6})\\s)(?!\\s*\\d[.)]\\s)(?!\\s*>)(?!`{3})(.+)"); // Tested
    private final Pattern blockCodePattern = Pattern.compile("(?m)^ {0,3}`{3} *(.*)\\n((?:.*|\\n)+)\\n {0,3}`{3,}"); // Tested

    private Pattern[] inlinePatterns = {
            bldPattern, itlPattern, lnkPattern, imgPattern
    };

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

    private void parseMarkdown(HTMLElement html, String md) {
        LinkedList<String> lines = new LinkedList<>(Arrays.asList(md.split("\n"))); // separates markdown into lines
        ListIterator<String> lineIterator = lines.listIterator(); // initializes iterator to iterate between lines
        parseMarkdown(html, lineIterator, 0); // calls recursive version of this parser function to build html
    }

    private void parseMarkdown(HTMLElement html, ListIterator<String> i, int indent) {
        if(i.hasNext()) {
            if(parseHeader(html, i) || parseOL(html, i, 0, indent) || parseUL(html, i, 0, indent)) {
                parseMarkdown(html, i, indent); // if something parsed then continue to parse next lines
            } else parseParagraph(html, i);
            if (inList == 0) {
                parseMarkdown(html, i, indent); // if something parsed then continue to parse next lines
            }
        }
    }

    public boolean parseOL(HTMLElement html, ListIterator<String> i, int parentIndent, int indent) {
        String line = i.next();
        Matcher olMatcher = oliPattern.matcher(line);
        if (olMatcher.find()) {
            HTMLElement currentTree = html;
            if(inList == 0 || olMatcher.start(1) >= parentIndent+indent + 3) {
                parentIndent = parentIndent + indent;
                indent = olMatcher.start(1) - parentIndent;
                currentTree = new HTMLElement("ol", (HTMLElement) null);
                try {
                    html.add(currentTree);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                inList++;
            } else if(olMatcher.start(1) < parentIndent + 3) {
                currentTree = currentTree.getParent();
                indent = 0;
                inList--;
            }
            i.remove();
            i.add(olMatcher.group(2));
            i.previous();
            parseMarkdown(currentTree, i, indent);
            if(i.hasNext())  if(!parseOL(currentTree, i, parentIndent, indent)) inList=0;
            return true;
        }
        i.previous();
        return false;
    }

    public boolean parseUL(HTMLElement html, ListIterator<String> i, int parentIndent, int indent) {
        String line = i.next();
        Matcher ulMatcher = uliPattern.matcher(line);
        if (ulMatcher.find()) {
            HTMLElement currentTree = html;
            if(inList == 0 || ulMatcher.start(1) >= parentIndent+indent + 2) {
                parentIndent = parentIndent + indent;
                indent = ulMatcher.start(1) - parentIndent;
                currentTree = new HTMLElement("ul", (HTMLElement) null);
                try {
                    html.add(currentTree);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                inList++;
            } else if(ulMatcher.start(1) < parentIndent + 2) {
                currentTree = currentTree.getParent();
                indent = 0;
                inList--;
            }
            i.remove();
            i.add(ulMatcher.group(2));
            i.previous();
            parseMarkdown(currentTree, i, indent);
            if(i.hasNext()) if(!parseUL(currentTree, i, parentIndent, indent)) inList=0;
            return true;
        }
        i.previous();
        return false;
    }

    private Pattern inlinePatterns() {
        StringBuilder inlineRegex = new StringBuilder();
        Predicate<Pattern> isNotEmpty = s -> !(s.pattern().isEmpty());
        Iterator<Pattern> i = Arrays.stream(inlinePatterns).filter(isNotEmpty).iterator();
        while(i.hasNext()) {
            String pattern =  i.next().pattern();
            inlineRegex.append(pattern+(i.hasNext()?"|":""));
        }
        return Pattern.compile(inlineRegex.toString());
    }

    private void parseParagraph(HTMLElement html, ListIterator<String> i) {
        String line = i.next();
        Matcher inlineMatcher = inlinePatterns().matcher(line);
        HTMLElement text = new HTMLElement((inList > 0) ? "li" : "p", (HTMLElement) null);
        int lastEnd = 0;
        while (inlineMatcher.find()) {
            String group = inlineMatcher.group(0);
            try {
                // Parse bold
                Matcher matcher = bldPattern.matcher(group);
                if(matcher.find()) { // if found group is bold
                    text.add(new HTMLElement(line.substring(lastEnd, inlineMatcher.start()))).add(new HTMLElement("strong", matcher.group(1)));
                    lastEnd = inlineMatcher.end();
                    continue;
                }

                // Parse italic
                matcher = itlPattern.matcher(group);
                if(matcher.find()) { // if found group is italic
                    text.add(new HTMLElement(line.substring(lastEnd, inlineMatcher.start()))).add(new HTMLElement("em", matcher.group(1)));
                    lastEnd = inlineMatcher.end();
                    continue;
                }

                // Parse link
                matcher = lnkPattern.matcher(group);
                if(matcher.find()) {
                    HTMLElement a = new HTMLElement("a", matcher.group(2));
                    a.setKey("href", matcher.group(1));
                    text.add(a);
                    lastEnd = inlineMatcher.end();
                    continue;
                }

                // Parse image
                matcher = imgPattern.matcher(group);
                if(matcher.find()) {
                    HTMLElement img = new HTMLElement("img", matcher.group(2));
                    img.setKey("src", matcher.group(1));
                    text.add(img);
                    lastEnd = inlineMatcher.end();
                    continue;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            text.add(new HTMLElement(line.substring(lastEnd)));
            html.add(text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void parseWords(HTMLElement html, ListIterator<String> i) {

    }

    private boolean parseHeader(HTMLElement html, ListIterator<String> i) {
        String text = i.next(); // get next line
        Matcher hMatcher = hdrPattern.matcher(text); // initializes the header matcher for md line
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
