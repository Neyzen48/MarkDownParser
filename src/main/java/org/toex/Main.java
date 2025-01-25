package org.toex;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println(new MDParser("- test\n- test").build());
    }


}