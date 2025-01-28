package org.toex;

public class Main {
    public static String testMarkdown = """
            # Header Example
            
            This is a paragraph with **strong text** and *italic text*. Markdown is a simple and versatile way to format text for web documents.
            
            ## Lists
            
            ### Ordered List
            1. First item
               1. Sub-item 1
               2. Sub-item 2
            2. Second item
               1. Sub-item 1
                  1. Sub-sub-item 1
               2. Sub-item 3   
            
            ### Unordered List
            - First item
              - Sub-item 1
              - Sub-item 2
            - Second item
              - Sub-item 1
              - Sub-item 2
            
            ## Code Block
            Here’s an example of a code block:
            
            ```javascript
            function greet(name) {
                console.log(`Hello, ${name}!`);
            }
            greet('World');
            ```
            
            ## Quote
            > "The only limit to our realization of tomorrow is our doubts of today."
            > — Franklin D. Roosevelt
            
            ## Links and Images
            Here’s a [link to OpenAI](https://www.openai.com).
            
            And here’s an image:
            ![Markdown Logo](https://markdown-here.com/img/icon256.png)
            """;

    public static void main(String[] args) {
        MDParser parser = new MDParser();
        HTMLElement html = parser.compile(testMarkdown);
        System.out.println(html.createDocument());
    }
}