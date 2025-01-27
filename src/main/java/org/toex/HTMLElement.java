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

        String toReturn = (!tag.matches("(p|h\\d|strong|span)") ? "\n" : "");
        Iterator<HTMLElement> i = elements.iterator();
        while(i.hasNext()) {
            toReturn = toReturn + i.next().toString() + (!tag.matches("(p|h\\d)") ? "\n" : "");
        }
        return "<" + tag + ">" + toReturn + "</" + tag + ">";
    }

    @Override
    public Iterator<HTMLElement> iterator() {
        if(elements.isEmpty()) {
            return null;
        }
        return elements.iterator();
    }
}
