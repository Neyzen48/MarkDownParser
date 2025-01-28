# Markdown Documentation Example
Markdown is a lightweight markup language used to create formatted text using a plain-text editor. It’s easy to learn and widely used in documentation, wikis, and more. ~~**Old Text**~~

## Features of Markdown
### Lists
#### Ordered List Example
1. *test*
    1. Markdown Basics
    2. Benefits of Markdown
2. **Intermediate**
    1. Lists and Links
    2. Code Blocks
3. **Advanced**
    1. Embedding Images
    2. Combining Features
#### Unordered List Example
- **Getting Started**
    - Installation
    - Syntax Overview
- **Advanced Features**
    - Embedding HTML
    - Using Plugins
- **Best Practices**
    - Clean Formatting
    - Reusability

## Code Blocks
You can use Markdown to write and display code. Here’s an example of a Python function:

```python
def factorial(n):
    if n == 0 or n == 1:
        return 1
    else:
        return n * factorial(n - 1)
print(factorial(5))  # Output: 120
```
You can also inline code like this: `console.log('Hello, Markdown!');`.

## Blockquotes
Blockquotes are useful for emphasizing text or sharing quotes:
> "Write programs that do one thing and do it well."  
> — Doug McIlroy
> **Tip:** Use blockquotes to make important notes stand out.

## Text Formatting
Here’s how you can format text:
- **Bold text**: Use `**` or `__` (e.g., `**Bold**` → **Bold**).
- *Italic text*: Use `*` or `_` (e.g., `*Italic*` → *Italic*).
- ~~Strikethrough~~: Use `~~` (e.g., `~~Strikethrough~~` → ~~Strikethrough~~).

## Links and Images
### Links
Links are easy to add in Markdown:
- [Visit OpenAI](https://www.openai.com)
- [Markdown Guide](https://www.markdownguide.org)

### Images
You can embed images directly:
![Markdown Logo](https://markdown-here.com/img/icon256.png)
Here’s an example of embedding an image with alt text:
![Description of the image](https://example.com/image.png)

## Advanced Usage: Combining Features
### A Real-World Example
Here’s how you can combine various Markdown features in a documentation-like format:

> ## Project Overview
>
> The **Markdown Enhancer** is an open-source project aimed at simplifying text formatting for developers.
>
> ### Features:
> 1. **Simple Syntax**:
>   - Easy to learn for beginners.
>   - Great for quick documentation.
> 2. **Cross-Platform**:
>   - Works on all modern devices.
> 3. **Open-Source**:
>   - Contributions are welcome!
>
> ### Example Code:
> ```javascript
> function sayHello() {
>     console.log("Hello, World!");
> }
> sayHello();
> ```

## Conclusion
Markdown is a versatile and powerful tool for creating clean, readable, and maintainable documents. By combining elements such as **text formatting**, *lists*, `code`, and images, you can create professional-looking documentation quickly and efficiently.
> "Start writing with Markdown today, and let your ideas shine!"