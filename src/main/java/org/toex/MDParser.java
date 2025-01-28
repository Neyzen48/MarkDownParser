package org.toex;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MDParser {
    private final Pattern hdrPattern = Pattern.compile("^( {0,3}#{1,6}) +(.*)"); // Pattern to identify Markdown headers with different levels
    private final Pattern oliPattern = Pattern.compile("^ *(\\d)[.)] (.*)"); // Pattern to identify ordered list items in Markdown
    private final Pattern uliPattern = Pattern.compile("^ *([-+*]) (.*)"); // Pattern to identify unordered list items in Markdown
    private final Pattern tb1Pattern = Pattern.compile("(?m)^( {0,3}-= *)(?!.)"); // Pattern to identify horizontal lines
    private final Pattern bldPattern = Pattern.compile("\\*{2}([^ ].+?[^ ])\\*{2}"); // Pattern to identify bold text in Markdown
    private final Pattern itlPattern = Pattern.compile("[^*]\\*([^*].+?[^*])\\*[^*]"); // Pattern to identify italic text in Markdown
    private final Pattern lnkPattern = Pattern.compile("[^!]\\[(.*?)]\\((.*?)\\)"); // Pattern to identify links in Markdown
    private final Pattern imgPattern = Pattern.compile("!\\[(.*?)]\\((.*?)\\)"); // Pattern to identify images in Markdown
    private final Pattern quoPattern = Pattern.compile("^> (.*)"); // Pattern to identify blockquotes in Markdown
    private final Pattern doubleNLPattern = Pattern.compile("(?m)(\\s*\\n){2,}"); // Pattern to remove multiple consecutive newlines
    private final Pattern continuePattern = Pattern.compile("(?m)^(?!`{3})(?!\\s*#{1,6}\\s)(.+)\\n(?!\\s*(?:[\\-*+]|\\#{1,6})\\s)(?!\\s*\\d[.)]\\s)(?!\\s*>)(?!`{3})(.+)"); // Pattern to merge lines that belong to the same paragraph
    private final Pattern blockCodePattern = Pattern.compile("(?m)^ {0,3}`{3} *(.*)\\n((?:.*|\\n)+)\\n {0,3}`{3,}"); // Pattern to identify code blocks in Markdown

    private Pattern[] inlinePatterns = { // Array of inline patterns used for parsing inline Markdown elements
            bldPattern, itlPattern, lnkPattern, imgPattern
    };

    int inList = 0; // Tracks the current depth of list nesting

    private String mergeLines(String mdText) { // Tested
        while(continuePattern.matcher(mdText).find()) { // finds any line that is not a new paragraph
            mdText = continuePattern.matcher(mdText).replaceFirst("$1 $2"); // and merges them, hope not merging something it shouldn't
        }
        return doubleNLPattern.matcher(mdText).replaceAll("\n"); // removes all empty lines and returns it back
    }

    private String precompile(String mdText) { // Tested
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
        parseMarkdown(html, precompiledMarkdown); // Starts to parse, html tree will be built
        return html; // returns built tree back to
    }

    private void parseMarkdown(HTMLElement html, String md) {
        LinkedList<String> lines = new LinkedList<>(Arrays.asList(md.split("\n"))); // separates markdown into lines
        ListIterator<String> lineIterator = lines.listIterator(); // initializes iterator to iterate between lines
        parseMarkdown(html, lineIterator, 0); // calls recursive version of this parser function to build html
    }

    private void parseMarkdown(HTMLElement html, ListIterator<String> i, int indent) {
        if(i.hasNext()) {
            if(parseHeader(html, i) || parseQuote(html, i) || parseCode(html, i) ||parseOL(html, i, 0, indent) || parseUL(html, i, 0, indent)) {
                parseMarkdown(html, i, indent); // if something parsed then continue to parse next lines
            } else parseParagraph(html, i); // parse the line as a paragraph if no other pattern matches
            if (inList == 0) {
                parseMarkdown(html, i, indent); // if something parsed then continue to parse next lines
            }
        }
    }

    private boolean parseQuote(HTMLElement html, ListIterator<String> i) {
        String line = i.next(); // get the current line
        Matcher quoMatcher = quoPattern.matcher(line); // match the line with blockquote pattern
        if(quoMatcher.find()) { // if blockquote pattern matches
            StringBuilder sb = new StringBuilder(); // initialize a StringBuilder to accumulate blockquote content
            sb.append(quoMatcher.group(1)).append("\n"); // append the matched blockquote content
            quoMatcher.group(); // call group to ensure matching
            while(i.hasNext()) {
                String quoLine = i.next(); // move to the next line
                quoMatcher = quoPattern.matcher(quoLine); // check if it's still part of the blockquote
                if(quoMatcher.find()) {
                    sb.append(quoMatcher.group(1)).append("\n"); // add content to the blockquote
                } else {
                    HTMLElement quote = compile(sb.toString()); // compile the blockquote content recursively
                    quote.setTag("blockquote"); // set the tag as blockquote
                    try {
                        html.add(quote); // add the blockquote element to the HTML tree
                    } catch (Exception e) {
                        throw new RuntimeException(e); // handle runtime exceptions
                    }
                    break; // exit the loop when blockquote ends
                }
            }
            i.previous(); // move back one step after exiting the blockquote
            return true; // indicate a successful match
        }
        i.previous(); // revert iterator if no match found
        return false; // indicate no match found
    }

    private boolean parseCode(HTMLElement html, ListIterator<String> i) {
        String line = i.next(); // get the current line
        if (line.matches("^ {0,4}```(.*)")) {
            StringBuilder sb = new StringBuilder();
            sb.append(line).append("\n");
            while(i.hasNext()) {
                String codeLine = i.next();
                if(codeLine.matches("^ {0,4}```")) {
                    break;
                } else {
                    sb.append(codeLine).append("\n");
                }
            }
            sb.append(line).append("```");
            Matcher blockCodeMatcher = blockCodePattern.matcher(sb);
            if(blockCodeMatcher.find()) {
                HTMLElement code = new HTMLElement("code", blockCodeMatcher.group(2));
                code.addKey("class", blockCodeMatcher.group(1));
                try {
                    html.add(new HTMLElement("pre", code));
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
            } else if(olMatcher.start(1) < parentIndent + 3) { // if the list is not nested
                currentTree = currentTree.getParent(); // move to the parent HTML element
                indent = 0; // reset the indentation
                inList--; // decrement the list nesting level
            }
            i.remove(); // remove the current line from the iterator
            i.add(olMatcher.group(2)); // add the list item's content to the iterator
            i.previous(); // move back to process the added content
            parseMarkdown(currentTree, i, indent); // parse the content of the list item
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
            } else if(ulMatcher.start(1) < parentIndent + 2) { // if the list is not nested
                currentTree = currentTree.getParent(); // move to the parent HTML element
                indent = 0; // reset the indentation
                inList--; // decrement the list nesting level
            }
            i.remove(); // remove the current line from the iterator
            i.add(ulMatcher.group(2)); // add the list item's content to the iterator
            i.previous(); // move back to process the added content
            parseMarkdown(currentTree, i, indent); // parse the content of the list item
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

    private void parseParagraph(HTMLElement html, ListIterator<String> i) {
        String line = i.next(); // get the current line
        Matcher inlineMatcher = inlinePatterns().matcher(line); // match the line with inline patterns
        HTMLElement text = new HTMLElement((inList > 0) ? "li" : "p", (HTMLElement) null); // create a list item or paragraph element based on context
        int lastEnd = 0; // track the last match's end position
        while (inlineMatcher.find()) { // iterate through all inline matches
            String group = inlineMatcher.group(0); // get the matched group
            try {
                Matcher matcher = bldPattern.matcher(group); // check for bold text
                if(matcher.find()) { // if bold text is found
                    text.add(new HTMLElement(line.substring(lastEnd, inlineMatcher.start()))) // add the text before the bold match
                            .add(new HTMLElement("strong", matcher.group(1))); // add the bold text
                    lastEnd = inlineMatcher.end(); // update the last match's end position
                    continue; // move to the next match
                }

                matcher = itlPattern.matcher(group); // check for italic text
                if(matcher.find()) { // if italic text is found
                    text.add(new HTMLElement(line.substring(lastEnd, inlineMatcher.start()+1))) // add the text before the italic match
                            .add(new HTMLElement("em", matcher.group(1))); // add the italic text
                    lastEnd = inlineMatcher.end()-1; // update the last match's end position
                    continue; // move to the next match
                }

                matcher = lnkPattern.matcher(group); // check for links
                if(matcher.find()) { // if a link is found
                    HTMLElement a = new HTMLElement("a", matcher.group(1)); // create an anchor element with link text
                    a.addKey("href", matcher.group(2)); // add the href attribute with the link URL
                    text.add(a); // add the link element to the text
                    lastEnd = inlineMatcher.end(); // update the last match's end position
                    continue; // move to the next match
                }

                matcher = imgPattern.matcher(group); // check for images
                if(matcher.find()) { // if an image is found
                    HTMLElement img = new HTMLElement("img", (HTMLElement) null); // create an image element
                    img.addKey("src", matcher.group(2)); // add the src attribute with the image URL
                    img.addKey("alt", matcher.group(1)); // add the alt attribute with the image description
                    text.add(img); // add the image element to the text
                    lastEnd = inlineMatcher.end(); // update the last match's end position
                    continue; // move to the next match
                }
            } catch (Exception e) {
                throw new RuntimeException(e); // handle exceptions
            }
        }
        try {
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
