package org.toex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HTMLElement implements Iterable{
    HTMLElement parent;
    String tag;
    String data;
    ArrayList<HTMLElement> elements = new ArrayList<>();
    Map<String, String> keys = new HashMap<>();

    public HTMLElement(String tag, HTMLElement... elements) {
        this.tag = tag;
        for(HTMLElement e: elements) {
            if(e != null) {
                this.elements.add(e);
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
        this.elements.add(new HTMLElement(data));
    }

    public HTMLElement add(HTMLElement e) throws Exception {
        if(data != null) {
            throw new Exception("Diese HTML-Element enthält schon ein Data.");
        }
        e.parent = this;
        elements.add(e);
        return this;
    }

    public HTMLElement getParent() {
        return parent;
    }

    public String getKey(String key) {
        return keys.get(key);
    }

    public void setKey(String key, String value) {
        keys.put(key, value);
    }

    @Override
    public String toString() {
        if(elements.isEmpty()) {
            return data;
        }
        StringBuilder sb = new StringBuilder();
        String inlineTags = "(p|h\\d|strong|span|em|i|title|a|img)";

        sb.append("<").append(tag);
        for (Map.Entry<String, String> entry : keys.entrySet()) {
            sb.append(" ").append(entry.getKey())
                    .append(" =\"")
                    .append(entry.getValue())
                    .append("\"");
        }
        sb.append(">").append(!tag.matches(inlineTags) ? "\n" : "");
        Iterator<HTMLElement> i = elements.iterator();
        while(i.hasNext()) {
            sb.append(i.next().toString() + (!tag.matches(inlineTags) ? "\n" : ""));
        }
        sb.append("</").append(tag).append(">");
        return sb.toString();
    }

    @Override
    public Iterator<HTMLElement> iterator() {
        if(elements.isEmpty()) {
            return null;
        }
        return elements.iterator();
    }
}
