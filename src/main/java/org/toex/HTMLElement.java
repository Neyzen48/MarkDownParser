package org.toex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HTMLElement implements Iterable{

    private static final String SINGLE_TAGS = "(area|base|col|emberd|hr|img|input|keygen|link|meta|param|source|track|wbr)";
    private static final String INLINE_TAGS = "(p|h\\d|strong|span|em|i|title|a)" + ("|") +  SINGLE_TAGS;


    HTMLElement parent;
    String tag;
    String data;
    ArrayList<HTMLElement> children = new ArrayList<>();
    Map<String, String> keys = new HashMap<>();

    public HTMLElement(String tag, HTMLElement... elements) {
        this.tag = tag;
        for(HTMLElement e: elements) {
            if(e != null) {
                this.children.add(e);
            }
        }
    }

    public HTMLElement() {
        this.tag = "div";
    }

    public HTMLElement(String data) {
        this.data = data;
    }

    public HTMLElement(String tag, String data) {
        this.tag = tag;
        this.children.add(new HTMLElement(data));
    }

    public HTMLElement add(HTMLElement e) throws Exception {
        if(data != null) {
            throw new Exception("Diese HTML-Element enthält schon ein Data.");
        }
        e.parent = this;
        children.add(e);
        return this;
    }

    public HTMLElement getParent() {
        return parent;
    }

    public String getKey(String key) {
        return keys.get(key);
    }

    public void addKey(String key, String value) {
        keys.put(key, value);
    }

    public String createDocument() {
        StringBuilder document = new StringBuilder();
        HTMLElement meta = new HTMLElement("meta", (HTMLElement) null);
        meta.addKey("charset", "UTF-8");
        HTMLElement title = new HTMLElement("title", "Title");
        HTMLElement head = new HTMLElement("head", meta, title);
        HTMLElement body = new HTMLElement("body", this);
        HTMLElement html = new HTMLElement("html", head, body);
        html.addKey("lang", "en");
        document.append("<!DOCTYPE html>\n");
        return html.build(0);
    }

    private String build(int indent) {
        StringBuilder sb = new StringBuilder();
        boolean isInlineTag = tag == null || tag.matches(INLINE_TAGS);
        boolean isSingleTag = children.isEmpty() && (tag == null || tag.matches(SINGLE_TAGS));
        int currentIndent = isInlineTag ? 0 : indent;
        int nextIndent = currentIndent + (isInlineTag ? 0 : 4);
        if(tag == null) {
            return " ".repeat(indent) + data;
        }

        sb.append(" ".repeat(indent)).append("<").append(tag);

        // add keys and values
        for (Map.Entry<String, String> entry : keys.entrySet()) { // for all keys in key and value map
            sb.append(" ")
                    .append(entry.getKey()) // add key
                    .append(" =\"").append(entry.getValue()).append("\"");  // add key value
        }
        sb.append(">").append(isInlineTag
                ? "" : "\n"); // close tag, if it is a inline tag don't start with a new line

        if (isSingleTag) { // if the given tag is single, don't proceed further
            return sb.toString();
        }

        Iterator<HTMLElement> i = children.iterator(); // iterate child elements
        while(i.hasNext()) { // if list is not finished
            sb.append(i.next().build(nextIndent)) // add child element into current element code
                    .append(isInlineTag ? "" : "\n"); // append child html code
        }
        sb.append(" ".repeat(currentIndent)).append("</").append(tag).append(">"); // close tag
        return sb.toString();
    }

    @Override
    public String toString() {
        return build(0);
    }

    @Override
    public Iterator<HTMLElement> iterator() {
        if(children.isEmpty()) {
            return null;
        }
        return children.iterator();
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
