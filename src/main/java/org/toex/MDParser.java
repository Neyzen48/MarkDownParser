package org.toex;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MDParser {
    private final Pattern hdrPattern = Pattern.compile("^( {0,3}#{1,6}) +(.*)"); // Pattern to identify Markdown headers with different levels
    private final Pattern oliPattern = Pattern.compile("^ *(\\d)[.)] (.*)"); // Pattern to identify ordered list items in Markdown
    private final Pattern uliPattern = Pattern.compile("^ *([-+*]) (.*)"); // Pattern to identify unordered list items in Markdown
    private final Pattern bdiPattern = Pattern.compile("(\\*+)(.*?)\\1"); // Pattern to identify bold and italic text in Markdown
    private final Pattern strPattern = Pattern.compile("(~{2})(.*?)(~{2})"); // Pattern to identify bold and italic text in Markdown
    private final Pattern lnkPattern = Pattern.compile("(?<!!)\\[(.*?)\\]\\((.*?)\\)"); // Pattern to identify links in Markdown
    private final Pattern imgPattern = Pattern.compile("!\\[(.*?)]\\((.*?)\\)"); // Pattern to identify images in Markdown
    private final Pattern quoPattern = Pattern.compile("^ {0,4}>(.*)"); // Pattern to identify blockquotes in Markdown
    private final Pattern codePattern = Pattern.compile("\\`(.*?)\\`");
    private final Pattern doubleNLPattern = Pattern.compile("(?m)(\\s*\\n){2,}"); // Pattern to remove multiple consecutive newlines
    private final Pattern continuePattern = Pattern.compile("(?m)^(?!`{3})(?!\\s*#{1,6}\\s)(.+)\\n(?!\\s*(?:[\\-*+]|\\#{1,6})\\s)(?!\\s*\\d[.)]\\s)(?!\\s*>)(?!`{3})(.+)"); // Pattern to merge lines that belong to the same paragraph
    private final Pattern blockCodePattern = Pattern.compile("(?m)^ {0,3}`{3} *(.*)\\n((?:.*|\\n)+)\\n {0,3}`{3,}"); // Pattern to identify code blocks in Markdown

    private final Pattern[] inlinePatterns = { // Array of inline patterns used for parsing inline Markdown elements
            bdiPattern, strPattern, lnkPattern, imgPattern, codePattern,
    };

    private final List<BiFunction<HTMLElement, String, Boolean>> inlineParsers = Arrays.asList(
            this::parseCode,
            this::parseBoldItalic,
            this::parseStriketrough,
            this::parseImage,
            this::parseLink
    );

    private final List<BiFunction<HTMLElement, ListIterator<String>, Boolean>> parsers = Arrays.asList(
            this::parseHeader,
            this::parseQuote,
            this::parseBlockCode,
            (html, i) -> parseOL(html, i, 0, 0),
            (html, i) -> parseUL(html, i, 0, 0)
    );

    int inList = 0; // Tracks the current depth of list nesting

    private String mergeLines(String mdText) { // Tested
        while(continuePattern.matcher(mdText).find()) { // finds any line that is not a new paragraph
            mdText = continuePattern.matcher(mdText).replaceFirst("$1 $2"); // and merges them, hope not merging something it shouldn't
        }
        return doubleNLPattern.matcher(mdText).replaceAll("\n"); // removes all empty lines and returns it back
    }

    private String precompile(String md) { // Tested
        StringBuilder markdown = new StringBuilder(); // string builder for markdown simplify process
        Matcher blockCodeMatcher = blockCodePattern.matcher(md); // matcher for code block
        int lastEnd = 0; // last string position of code block
        while(blockCodeMatcher.find()) { // find code blocks
            markdown.append(mergeLines(md.substring(lastEnd, blockCodeMatcher.start()))) // change code blocks and adds them into new markdown
                    .append(blockCodeMatcher.group()); // don't change code blocks and adds them into new markdown
            lastEnd = blockCodeMatcher.end(); // reset last end position of code block
        }
        return markdown.append(mergeLines(md.substring(lastEnd))).toString(); // add left markdown after code block and return it back
    }

    public HTMLElement compile(String md) { // Tested
        HTMLElement html = new HTMLElement(); // creates a new html tree
        String precompiledMarkdown = precompile(md); // run precompiler to get rid of empty lines and merge continuously paragraphs
        LinkedList<String> lines = new LinkedList<>(Arrays.asList(precompiledMarkdown.split("\n"))); // separates markdown into lines
        ListIterator<String> lineIterator = lines.listIterator(); // initializes iterator to iterate between lines
        parseMarkdown(html, lineIterator); // calls recursive version of this parser function to build html
        return html; // returns built tree back to
    }

    private void parseMarkdown(HTMLElement html, ListIterator<String> i) {
        if(i.hasNext()) {
            boolean parsed = parsers.stream().anyMatch(parser -> parser.apply(html, i));
            if(!parsed) {
                parseParagraph(html, i); // parse the line as a paragraph if no other pattern matches
            }
            if (inList == 0) {
                parseMarkdown(html, i); // if something parsed then continue to parse next lines
            }
        }
    }

    private boolean parseQuote(HTMLElement html, ListIterator<String> i) {
        String line = i.next(); // get the current line
        Matcher quoMatcher = quoPattern.matcher(line); // match the line with blockquote pattern
        if(quoMatcher.find()) { // if blockquote pattern matches
            StringBuilder sb = new StringBuilder(); // initialize a StringBuilder to accumulate blockquote content
            sb.append(quoMatcher.group(1)); // append the matched blockquote content
            quoMatcher.group(); // call group to ensure matching
            while(i.hasNext()) {
                String quoLine = i.next(); // move to the next line
                quoMatcher = quoPattern.matcher(quoLine); // check if it's still part of the blockquote
                if(quoMatcher.find()) {
                    sb.append("\n").append(quoMatcher.group(1)); // add content to the blockquote
                } else {
                    break; // exit the loop when blockquote ends
                }
            }
            HTMLElement quote = compile(sb.toString()); // compile the blockquote content recursively
            quote.setTag("blockquote"); // set the tag as blockquote
            try {
                html.add(quote); // add the blockquote element to the HTML tree
            } catch (Exception e) {
                throw new RuntimeException(e); // handle runtime exceptions
            }
            if(i.hasNext()) i.previous(); // move back one step after exiting the blockquote
            return true; // indicate a successful match
        }
        i.previous(); // revert iterator if no match found
        return false; // indicate no match found
    }

    private boolean parseBlockCode(HTMLElement html, ListIterator<String> i) {
        String line = i.next(); // get the current line

        if (line.stripTrailing().matches("^ {0,4}```(.*)")) {
            StringBuilder sb = new StringBuilder();
            sb.append(line).append("\n");
            while(i.hasNext()) {
                String codeLine = i.next();
                if(codeLine.stripTrailing().matches("^ {0,4}```")) {
                    break;
                } else {
                    sb.append(codeLine).append("\n");
                }
            }
            sb.append(line).append("```");
            Matcher blockCodeMatcher = blockCodePattern.matcher(sb.toString().stripIndent());
            if(blockCodeMatcher.find()) {
                HTMLElement pre = new HTMLElement("pre", blockCodeMatcher.group(2));
                pre.addKey("class", blockCodeMatcher.group(1));
                try {
                    html.add(pre);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return true; // indicate a successful match
        }
        i.previous(); // revert iterator if no match found
        return false; // indicate no match found
    }

    private boolean parseOL(HTMLElement html, ListIterator<String> i, int parentIndent, int indent) {
        String line = i.next(); // get the current line
        Matcher olMatcher = oliPattern.matcher(line); // match the line with the ordered list pattern
        if (olMatcher.find()) { // if the line matches an ordered list item
            HTMLElement currentTree = html; // initialize the current HTML element
            if(inList == 0 || olMatcher.start(1) >= parentIndent+indent + 3) { // if not in a list or it's a nested list
                parentIndent = parentIndent + indent; // update parent indentation
                indent = olMatcher.start(1) - parentIndent; // calculate the indentation for the current list
                currentTree = new HTMLElement("ol", (HTMLElement) null); // create a new ordered list element
                try {
                    html.add(currentTree); // add the ordered list to the HTML tree
                } catch (Exception e) {
                    throw new RuntimeException(e); // handle exceptions
                }
                inList++; // increment the list nesting level
            } else if(inList > 1 && olMatcher.start(1) < parentIndent + 3) { // if the list is not nested
                currentTree = currentTree.getParent(); // move to the parent HTML element
                indent = 0; // reset the indentation
                inList--; // decrement the list nesting level
            }
            i.remove(); // remove the current line from the iterator
            i.add(olMatcher.group(2)); // add the list item's content to the iterator
            i.previous(); // move back to process the added content
            parseMarkdown(currentTree, i); // parse the content of the list item
            if(i.hasNext()) if(!parseOL(currentTree, i, parentIndent, indent)) inList=0; // check for nested lists
            return true; // indicate a successful match
        }
        i.previous(); // revert iterator if no match found
        return false; // indicate no match found
    }

    private boolean parseUL(HTMLElement html, ListIterator<String> i, int parentIndent, int indent) {
        String line = i.next(); // get the current line
        Matcher ulMatcher = uliPattern.matcher(line); // match the line with the unordered list pattern
        if (ulMatcher.find()) { // if the line matches an unordered list item
            HTMLElement currentTree = html; // initialize the current HTML element
            if(inList == 0 || ulMatcher.start(1) >= parentIndent+indent + 2) { // if not in a list or it's a nested list
                parentIndent = parentIndent + indent; // update parent indentation
                indent = ulMatcher.start(1) - parentIndent; // calculate the indentation for the current list
                currentTree = new HTMLElement("ul", (HTMLElement) null); // create a new unordered list element
                try {
                    html.add(currentTree); // add the unordered list to the HTML tree
                } catch (Exception e) {
                    throw new RuntimeException(e); // handle exceptions
                }
                inList++; // increment the list nesting level
            } else if(inList > 1 && ulMatcher.start(1) < parentIndent + 2) { // if the list is not nested
                currentTree = currentTree.getParent(); // move to the parent HTML element
                indent = 0; // reset the indentation
                inList--; // decrement the list nesting level
            }
            i.remove(); // remove the current line from the iterator
            i.add(ulMatcher.group(2)); // add the list item's content to the iterator
            i.previous(); // move back to process the added content
            parseMarkdown(currentTree, i); // parse the content of the list item
            if(i.hasNext()) if(!parseUL(currentTree, i, parentIndent, indent)) inList=0; // check for nested lists
            return true; // indicate a successful match
        }
        i.previous(); // revert iterator if no match found
        return false; // indicate no match found
    }

    private Pattern inlinePatterns() {
        StringBuilder inlineRegex = new StringBuilder(); // initialize a StringBuilder for inline patterns
        Predicate<Pattern> isNotEmpty = s -> !(s.pattern().isEmpty()); // define a predicate to check if a pattern is non-empty
        Iterator<Pattern> i = Arrays.stream(inlinePatterns).filter(isNotEmpty).iterator(); // filter non-empty patterns
        while(i.hasNext()) { // iterate through the filtered patterns
            String pattern =  i.next().pattern(); // get the pattern string
            inlineRegex.append(pattern+(i.hasNext()?"|":"")); // append the pattern with a separator if more patterns exist
        }
        return Pattern.compile(inlineRegex.toString()); // return the compiled regex for inline patterns
    }

    private boolean parseBoldItalic(HTMLElement html, String text) {
        Matcher matcher = bdiPattern.matcher(text);
        if(matcher.find()) {
            int starCount = matcher.group(1).length();
            try {
                if (starCount % 2 == 1) {
                    if(starCount > 1) {
                        html.add(new HTMLElement("em", new HTMLElement("strong", matcher.group(2).trim())));
                    } else {
                        html.add(new HTMLElement("em", matcher.group(2).trim()));
                    }
                } else if(starCount % 2 == 0){
                    html.add(new HTMLElement("strong", matcher.group(2).trim()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }

    private boolean parseStriketrough(HTMLElement html, String text) {
        Matcher matcher = strPattern.matcher(text);
        if(matcher.find()) {
            try {
                html.add(new HTMLElement("s", matcher.group(2).trim()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }

    private boolean parseCode(HTMLElement html, String text) {
        Matcher matcher = codePattern.matcher(text);
        if(matcher.find()) {
            try {
                html.add(new HTMLElement("code", matcher.group(1)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }

    private boolean parseLink(HTMLElement html, String text) {
        Matcher matcher = lnkPattern.matcher(text); // check for links
        if(matcher.find()) { // if a link is found
            HTMLElement a = new HTMLElement("a", matcher.group(1)); // create an anchor element with link text
            a.addKey("href", matcher.group(2)); // add the href attribute with the link URL
            try {
                html.add(a); // add the link element to the text
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }

    private boolean parseImage(HTMLElement html, String text) {
        Matcher matcher = imgPattern.matcher(text); // check for images
        if(matcher.find()) { // if an image is found
            HTMLElement img = new HTMLElement("img", (HTMLElement) null); // create an image element
            img.addKey("src", matcher.group(2)); // add the src attribute with the image URL
            img.addKey("alt", matcher.group(1)); // add the alt attribute with the image description
            try {
                html.add(img); // add the image element to the text
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }

    private void parseParagraph(HTMLElement html, ListIterator<String> i) {
        String line = i.next().trim(); // get the current line
        Matcher inlineMatcher = inlinePatterns().matcher(line); // match the line with inline patterns
        HTMLElement text = new HTMLElement((inList > 0) ? "li" : "p", (HTMLElement) null); // create a list item or paragraph element based on context
        int lastEnd = 0; // track the last match's end position
        try {
            while (inlineMatcher.find()) { // iterate through all inline matches
                String group = inlineMatcher.group(0); // get the matched group
                text.add(new HTMLElement(line.substring(lastEnd, inlineMatcher.start()))); // add the text before the bold match
                boolean parsed = inlineParsers.stream().anyMatch(parser -> parser.apply(text, group));
                if (parsed) {
                    lastEnd = inlineMatcher.end(); // update the last match's end position
                }
            }
            text.add(new HTMLElement(line.substring(lastEnd))); // add any remaining text after the last inline match
            html.add(text); // add the paragraph or list item to the HTML tree
        } catch (Exception e) {
            throw new RuntimeException(e); // handle exceptions
        }
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
        }
        i.previous(); // if no header with # found then go to previous line
        return false;
    }
}
