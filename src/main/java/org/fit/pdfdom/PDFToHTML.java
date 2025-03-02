
package org.fit.pdfdom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
//import org.fit.pdfdom.ConcretePDFBoxTree.TextLine;
import org.fit.pdfdom.resource.HtmlResourceHandler;
import org.fit.pdfdom.resource.IgnoreResourceHandler;
import org.fit.pdfdom.resource.SaveResourceToDirHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
//import org.w3c.dom.Document;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author burgetr
 *
 */
public class PDFToHTML {
    public static PDFDomTreeConfig globalConfig;

    // Path to the PDF file
    String filePath = "test.pdf";
    String outputDir = "output";
    // globalConfig = PDFDomTreeConfig.createDefaultConfig();
    public String newImgSrcString = "newImage";

    public static String inFile = "";
    public static String outFile = "";

    static List<TextLine> textLines = new ArrayList<>();
    static float leftPadding = 0.0f;

    public static void main(String[] args) {
        // Path to the PDF file
        String filePath = "test.pdf";
        String outputDir = "output";
        globalConfig = PDFDomTreeConfig.createDefaultConfig();
        

        // Set the logging level for the specific logger
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext
                .getLogger("org.mabb.fontverter.opentype.TtfInstructions.TtfInstructionParser");

        // Set the level to ERROR or OFF
        logger.setLevel(Level.OFF);

        if (args.length < 1) {
            System.out.println("Usage: PDFToHTML <infile> [<outfile>] [<options>]");
            System.out.println("Options: ");
            System.out.println("-fm=[mode] Font handler mode. [mode] = EMBED_BASE64, SAVE_TO_DIR, IGNORE");
            System.out.println(
                    "-fdir=[path] Directory to extract fonts to. [path] = font extract directory ie dir/my-font-dir");
            System.out.println();
            System.out.println("-im=[mode] Image handler mode. [mode] = EMBED_BASE64, SAVE_TO_DIR, IGNORE");
            System.out.println(
                    "-idir=[path] Directory to extract images to. [path] = image extract directory ie dir/my-image-dir");

            System.exit(1);
        }

        String infile = args[0];
        inFile = infile;
        String outfile;
        if (args.length > 1 && !args[1].startsWith("-")) {
            outfile = args[1];
            outFile = outfile;
        } else {
            String base = args[0];
            if (base.toLowerCase().endsWith(".pdf"))
                base = base.substring(0, base.length() - 4);
            outfile = base + ".html";
        }

        PDFDomTreeConfig config = parseOptions(args);

        PDDocument document = null;
        org.fit.pdfdom.PDFDomTree parser = null;
        // List<TextLine> textLines=null;

        try {
            document = PDDocument.load(new File(infile));
            
            // try (Writer output = new PrintWriter("test.html", "utf-8")) {
            //     document = PDDocument.load(new File(infile));
            //     //parserOldx.createDOM(document);
            //     parserNew.writeText(document, output);
            //     System.out.println("HTML file generated: " + outfile);
            // }
           
        //   } catch (Exception e) {
        //     System.err.println("Error: " + e.getMessage());
        //     e.printStackTrace();
        //   } finally {
        //     if (document != null)
        //       try {
        //         document.close();
        //       } catch (IOException e) {
        //         System.err.println("Error: " + e.getMessage());
        //       }  
        //   } 
        // try {
            //System.out.println(infile);

            // MyConcretePDFBoxTree processor = new MyConcretePDFBoxTree(outputDir);

            // processor.processDocument(document);

            // Build the command
            String[] command = {
                    "java",
                    "-jar",
                    "PDFToHTML.jar",
                    infile,
                    outfile,
            };

            // Create a ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Start the process
            Process process = processBuilder.start();

            // Check if the output file exists
            File outputFile = new File(outfile);
            if (outputFile.exists() && outputFile.length() > 0) {
                System.out.println("Output file created successfully: " + outfile);

                // Read the content of the output file
                String htmlContent = new String(Files.readAllBytes(Paths.get(outfile)));

                // Use the output content
                PDFDomTreeOld parser2 = new PDFDomTreeOld(globalConfig);
                // parser2.saveHtmlContentToFile(htmlContent);
                System.out.println("HTML content saved successfully.");

                // Parse HTML content
                Document docx = Jsoup.parse(htmlContent);

                // Extract text elements and group them into text lines
                extractTextLines(docx);

                // Sort the lines by their Y-coordinate
                sortTextLines();

                PDFTextProcessor processor = new PDFTextProcessor();

                // Mock input data
                List<TextPositionMock> textPositions = new ArrayList<>();
                float linePadding = 0.0f;

                for (TextLine line : textLines) {
                    //parser2.textLiner = line.getText();
                    textPositions.add(new TextPositionMock(line.getText(), 10, 20));
                    linePadding = line.getLeftPadding();
                }

                // Process the text positions into lines
                processor.processTextPositions(textPositions);

                // Assuming processor is an instance of PDFTextProcessor
                List<String> lines = processor.getTextLinesAsList();
                //parser2.liner = lines;

                // for (String line : lines ) {
                //     System.out.println(line);
                //     }

                // HTML generation
                StringBuilder finalHtmlBuilder = new StringBuilder();
                StringBuilder cssStyles = new StringBuilder();

                cssStyles.append(
                        "<style>.r{color:white;}[class^=\"custom-class-\"]{min-height:16pt;}@media(min-width:1280px){.custom-class-0{margin-top:4pt}.page{border:1px solid blue;width:560pt;}}</style>");

                finalHtmlBuilder.append(
                        "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\"/>\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n<title>PDF Extracted Text</title>\n<script src=\"https://cdn.tailwindcss.com\"></script>\n<style>")
                        .append(cssStyles)
                        .append("</head>\n<body>\n<div class=\"mx-auto sm:max-w-screen-sm md:max-w-screen-md lg:max-w-screen-lg xl:max-w-screen-xl relative p-4 my-10 page\">");

                for (int i = 0; i < lines.size(); i++) {
                    String textLine = lines.get(i);
                    float leftPaddingPercent = 0; // Replace with logic to calculate padding
                    final float MIN_FONT_SIZE = 8f;
                    final float MAX_FONT_SIZE = 32f;
                    float fontSize = 12f; // Replace with logic to determine font size
                    float clampedFontSize = Math.min(MAX_FONT_SIZE, Math.max(fontSize, MIN_FONT_SIZE));
                    //TextLine line = textLine;

                    //leftPaddingPercent = Math.max((linePadding / 560) * 100, 0);

                    int leftGridSpan = (int) Math.round((leftPaddingPercent / 100) * 12);
                    int remainingGridSpan = Math.max(12 - leftGridSpan, 1);

                    String inlineStyle = "font-family: Arial, sans-serif; font-weight: normal; font-size: "
                            + Math.round(clampedFontSize) + "pt; height: auto; padding-left: " + leftPaddingPercent
                            + "%;";

                    cssStyles.append(".custom-class-").append(i).append(" { ").append(inlineStyle).append(" }");

                    finalHtmlBuilder.append("<div class=\"custom-class-").append(i).append("\" data-top=\"").append(i)
                            .append("\">");

                    finalHtmlBuilder.append("<div class=\"grid grid-cols-12\">");

                    if (leftGridSpan > 0) {
                        finalHtmlBuilder.append("<div class=\"col-span-").append(leftGridSpan)
                                .append(" bg-transparent\"></div>");
                    }

                    finalHtmlBuilder.append("<div class=\"col-span-").append(remainingGridSpan)
                            .append(" bg-transparent\">")
                            .append(textLine).append("</div>");

                    finalHtmlBuilder.append("</div></div>");
                }

                finalHtmlBuilder.append("</div></body></html>");

                String finalHtmlContent = finalHtmlBuilder.toString();

                try (FileWriter fileWriter = new FileWriter("output/Responsive_" + PDFToHTML.outFile )) {
                    fileWriter.write(finalHtmlContent);
                }

            //     PDFDomTree parserOld = new PDFDomTree(config);
            // // Writer output = new PrintWriter(outfile, "utf-8");
            // // Custom Writer that filters invalid XML characters
            // System.out.println(outfile);

            // PDFTextStripper stripper = new PDFTextStripper();
            // String rawText = stripper.getText(document);

            // // Sanitize the text for invalid XML characters
            // String sanitizedText = sanitizeText(rawText);
            // // Use SanitizingWriter with parserOld
            // // Use custom PDFDomTree with sanitized text
            // CustomPDFDomTree parserOldx = new CustomPDFDomTree();

            // PDFDomTreeOld parserNew = new PDFDomTreeOld(config);
            // Writer output = new PrintWriter(outfile, "utf-8");
            // document = PDDocument.load(new File(infile));
            // parserNew.writeText(document, output);

            MyConcretePDFBoxTree processorConcrete = new MyConcretePDFBoxTree(outputDir);

            processorConcrete.processDocument(document);


            } else {
                System.out.println("Output file was not created or is empty.");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    System.err.println("Error: " + e.getMessage());
                    // e.printStackTrace();
                }
            }
        }
    }

    static String sanitizeText(String input) {
        return input.replaceAll("[^\\x20-\\x7E\\xA0-\\uD7FF\\uE000-\\uFFFD]", ""); // Remove invalid characters
    }

    // Custom PDFDomTree that sanitizes invalid XML characters
    static class CustomPDFDomTree extends PDFDomTree {
        public CustomPDFDomTree() throws IOException, ParserConfigurationException {
            super();
            //TODO Auto-generated constructor stub
        }

        private static final Pattern INVALID_XML_CHAR_PATTERN = Pattern.compile("[^\\x20-\\x7E\\xA0-\\uD7FF\\uE000-\\uFFFD]");

       // Override the writeText method to filter invalid XML characters
       @Override
       public void writeText(PDDocument document, Writer output) throws IOException {
           // Use PDFTextStripper to extract raw text
           PDFTextStripper stripper = new PDFTextStripper();
           String rawText = stripper.getText(document);

           // Sanitize the raw text
           String sanitizedText = sanitizeText(rawText);

           // Write sanitized text to output
           output.write(sanitizedText);
       }

        // Helper method to sanitize invalid XML characters
        private String sanitizeText(String input) {
            return INVALID_XML_CHAR_PATTERN.matcher(input).replaceAll("");
        }
    }

    
    // Custom Writer to filter invalid XML characters
    static class SanitizingWriter extends Writer {
        private final Writer wrappedWriter;
        private static final Pattern INVALID_XML_CHAR_PATTERN = Pattern.compile("[^\\x20-\\x7E\\xA0-\\uD7FF\\uE000-\\uFFFD]");

        public SanitizingWriter(Writer wrappedWriter) {
            this.wrappedWriter = wrappedWriter;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            // Sanitize text and write to wrappedWriter
            String sanitized = sanitizeText(new String(cbuf, off, len));
            wrappedWriter.write(sanitized);
        }

        @Override
        public void write(String str) throws IOException {
            // Sanitize text and write to wrappedWriter
            String sanitized = sanitizeText(str);
            wrappedWriter.write(sanitized);
        }

        @Override
        public void flush() throws IOException {
            wrappedWriter.flush();
        }

        @Override
        public void close() throws IOException {
            wrappedWriter.close();
        }

        // Helper method to sanitize text
        private String sanitizeText(String input) {
            return INVALID_XML_CHAR_PATTERN.matcher(input).replaceAll("");
        }
    }

    public void callFromOutside(String[] args) {
        // Path to the PDF file
        String filePath = "test.pdf";
        String outputDir = "output";
        globalConfig = PDFDomTreeConfig.createDefaultConfig();

        if (args.length < 1) {
            System.out.println("Usage: PDFToHTML <infile> [<outfile>] [<options>]");
            System.out.println("Options: ");
            System.out.println("-fm=[mode] Font handler mode. [mode] = EMBED_BASE64, SAVE_TO_DIR, IGNORE");
            System.out.println(
                    "-fdir=[path] Directory to extract fonts to. [path] = font extract directory ie dir/my-font-dir");
            System.out.println();
            System.out.println("-im=[mode] Image handler mode. [mode] = EMBED_BASE64, SAVE_TO_DIR, IGNORE");
            System.out.println(
                    "-idir=[path] Directory to extract images to. [path] = image extract directory ie dir/my-image-dir");

            System.exit(1);
        }

        String infile = args[0];
        String outfile;
        if (args.length > 1 && !args[1].startsWith("-"))
            outfile = args[1];
        else {
            String base = args[0];
            if (base.toLowerCase().endsWith(".pdf"))
                base = base.substring(0, base.length() - 4);
            outfile = base + ".html";
        }

        PDFDomTreeConfig config = parseOptions(args);

        PDDocument document = null;
        try {
            PDFDomTreeOld parser = new PDFDomTreeOld(config);
            // Load the PDF document
            document = PDDocument.load(new File(infile));
            System.out.println("PDF loaded successfully in PDFToHTML.");

            parser.createDOM(document);
            // System.out.println("html content @main: " + config.getHtmlOutput());
            newImgSrcString = config.getTestImgSrc();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    System.err.println("Error: " + e.getMessage());
                    // e.printStackTrace();
                }
            }
        }

        String[] hardcodedArgs = {
                PDFToHTML.inFile, // Input file
                "output.html", // Output file
                "-fm=EMBED_BASE64", // Font handler mode
                "-fdir=output/fonts", // Font directory
                "-im=SAVE_TO_DIR", // Image handler mode
                "-idir=output/images" // Image directory
        };

        callPDFToHTML(hardcodedArgs);
    }

    public void callPDFToHTML(String[] args) {
        // Path to the PDF file
        String filePath = "test.pdf";
        String outputDir = "output";
        globalConfig = PDFDomTreeConfig.createDefaultConfig();

        if (args.length < 1) {
            System.out.println("Usage: PDFToHTML <infile> [<outfile>] [<options>]");
            System.out.println("Options: ");
            System.out.println("-fm=[mode] Font handler mode. [mode] = EMBED_BASE64, SAVE_TO_DIR, IGNORE");
            System.out.println(
                    "-fdir=[path] Directory to extract fonts to. [path] = font extract directory ie dir/my-font-dir");
            System.out.println();
            System.out.println("-im=[mode] Image handler mode. [mode] = EMBED_BASE64, SAVE_TO_DIR, IGNORE");
            System.out.println(
                    "-idir=[path] Directory to extract images to. [path] = image extract directory ie dir/my-image-dir");

            System.exit(1);
        }

        String infile = args[0];
        String outfile;
        if (args.length > 1 && !args[1].startsWith("-"))
            outfile = args[1];
        else {
            String base = args[0];
            if (base.toLowerCase().endsWith(".pdf"))
                base = base.substring(0, base.length() - 4);
            outfile = base + ".html";
        }

    }

    private static PDFDomTreeConfig parseOptions(String[] args) {
        PDFDomTreeConfig config = PDFDomTreeConfig.createDefaultConfig();

        List<CommandLineFlag> flags = parseFlags(args);
        for (CommandLineFlag flagOn : flags) {
            if (flagOn.flagName.equals("fm")) {
                HtmlResourceHandler handler = createResourceHandlerFor(flagOn.value);
                config.setFontHandler(handler);
            } else if (flagOn.flagName.equals("fdir"))
                config.setFontHandler(new SaveResourceToDirHandler(new File(flagOn.value)));

            else if (flagOn.flagName.equals("im")) {
                HtmlResourceHandler handler = createResourceHandlerFor(flagOn.value);
                config.setImageHandler(handler);
            } else if (flagOn.flagName.equals("idir"))
                config.setImageHandler(new SaveResourceToDirHandler(new File(flagOn.value)));
        }

        return config;
    }

    private static HtmlResourceHandler createResourceHandlerFor(String value) {
        HtmlResourceHandler handler = PDFDomTreeConfig.embedAsBase64();
        if (value.equalsIgnoreCase("EMBED_BASE64"))
            handler = PDFDomTreeConfig.embedAsBase64();
        else if (value.equalsIgnoreCase("SAVE_TO_DIR"))
            handler = new SaveResourceToDirHandler();
        else if (value.equalsIgnoreCase("IGNORE"))
            handler = new IgnoreResourceHandler();

        return handler;
    }

    private static List<CommandLineFlag> parseFlags(String[] args) {
        List<CommandLineFlag> flags = new ArrayList<CommandLineFlag>();
        for (String argOn : args) {
            if (argOn.startsWith("-"))
                flags.add(CommandLineFlag.parse(argOn));
        }
        return flags;
    }

    private static class CommandLineFlag {
        public String flagName;
        public String value = "";

        public static CommandLineFlag parse(String argOn) {
            CommandLineFlag flag = new CommandLineFlag();
            String[] flagSplit = argOn.split("=");
            flag.flagName = flagSplit[0].replace("-", "");
            if (flagSplit.length > 1)
                flag.value = flagSplit[1].replace("=", "");

            return flag;
        }
    }

    private static void extractTextLines(Document document) {
        Elements elements = document.select("*[style]"); // Select all elements with style attributes
        for (Element element : elements) {
            String style = element.attr("style");

            // Extract 'top' and 'left' values from the style attribute
            float top = extractStyleValue(style, "top");
            float left = extractStyleValue(style, "left");

            if (top != -1) { // If 'top' is found
                TextLine line = new TextLine();
                line.add(new TextPositionMock(element.text(), top, left));
                textLines.add(line);
            }
        }
    }

    private static float extractStyleValue(String style, String property) {
        try {
            String[] styles = style.split(";");
            for (String s : styles) {
                if (s.trim().startsWith(property)) {
                    String value = s.split(":")[1].trim().replaceAll("[^\\d.]", ""); // Extract numeric part
                    return Float.parseFloat(value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if the property is not found
    }

    private static void sortTextLines() {
        // Sort the text lines based on their Y-coordinate
        Collections.sort(textLines, new Comparator<TextLine>() {
            @Override
            public int compare(TextLine line1, TextLine line2) {
                return Float.compare(line1.getYCoordinate(), line2.getYCoordinate());
            }
        });
    }

    public void processTextPositions(List<TextPositionMock> textPositions) {
        // Iterate over the text positions to group them into lines
        for (TextPositionMock textPosition : textPositions) {
            if (textLines.isEmpty() || !isSameLine(textPosition, textLines.get(textLines.size() - 1))) {
                // New line starts
                textLines.add(new TextLine());
            }
            // Add the text position to the current line
            textLines.get(textLines.size() - 1).add(textPosition);
        }

        // Sort lines by Y-coordinate (top to bottom)
        sortTextLinesByYCoordinate();
    }

    private boolean isSameLine(TextPositionMock textPosition, TextLine lastLine) {
        // Check if the text position belongs to the same line (based on Y-coordinate)
        final float margin = 2.0f; // Adjust margin as needed
        if (lastLine.isEmpty())
            return false; // If lastLine is empty, it's a new line

        float lastY = lastLine.getLastTextPosition().getYDirAdj();
        return Math.abs(lastY - textPosition.getYDirAdj()) < margin;
    }

    private void sortTextLinesByYCoordinate() {
        // Sort the lines based on the Y-coordinate of the first text position
        Collections.sort(textLines, new Comparator<TextLine>() {
            @Override
            public int compare(TextLine line1, TextLine line2) {
                return Float.compare(line1.getYCoordinate(), line2.getYCoordinate());
            }
        });
    }

    public void printTextLines() {
        for (TextLine line : textLines) {
            System.out.println(line.getText());
        }
    }

}

class PDFTextProcessor {

    private List<TextLine> textLines = new ArrayList<>();

    public void processTextPositions(List<TextPositionMock> textPositions) {
        // Iterate over the text positions to group them into lines
        for (TextPositionMock textPosition : textPositions) {
            if (textLines.isEmpty() || !isSameLine(textPosition, textLines.get(textLines.size() - 1))) {
                // New line starts
                textLines.add(new TextLine());
            }
            // Add the text position to the current line
            textLines.get(textLines.size() - 1).add(textPosition);
        }

        // Sort lines by Y-coordinate (top to bottom)
        sortTextLinesByYCoordinate();
    }

    private boolean isSameLine(TextPositionMock textPosition, TextLine lastLine) {
        // Check if the text position belongs to the same line (based on Y-coordinate)
        final float margin = 2.0f; // Adjust margin as needed
        if (lastLine.isEmpty())
            return false; // If lastLine is empty, it's a new line

        float lastY = lastLine.getLastTextPosition().getYDirAdj();
        return Math.abs(lastY - textPosition.getYDirAdj()) < margin;
    }

    private void sortTextLinesByYCoordinate() {
        // Sort the lines based on the Y-coordinate of the first text position
        Collections.sort(textLines, new Comparator<TextLine>() {
            @Override
            public int compare(TextLine line1, TextLine line2) {
                return Float.compare(line1.getYCoordinate(), line2.getYCoordinate());
            }
        });
    }

    public void printTextLines() {
        for (TextLine line : textLines) {
            System.out.println(line.getText());
        }
    }

    public List<String> getTextLinesAsList() {
        List<String> lines = new ArrayList<>();
        for (TextLine line : textLines) {
            lines.add(line.getText());
        }
        return lines;
    }
}

// Supporting Classes
class TextPositionMock {
    private String text;
    private float yDirAdj;
    private float xDirAdj;

    public TextPositionMock(String text, float yDirAdj, float xDirAdj) {
        this.text = text;
        this.yDirAdj = yDirAdj;
        this.xDirAdj = xDirAdj;
    }

    public String getText() {
        return text;
    }

    public float getYDirAdj() {
        return yDirAdj;
    }

    public float getXDirAdj() {
        return xDirAdj;
    }
}

class TextLine {
    private List<TextPositionMock> textPositions = new ArrayList<>();

    public void add(TextPositionMock textPosition) {
        textPositions.add(textPosition);
    }

    public String getText() {
        StringBuilder textBuilder = new StringBuilder();
        for (TextPositionMock textPosition : textPositions) {
            textBuilder.append(textPosition.getText()).append(" ");
        }
        return textBuilder.toString().trim();
    }

    public float getYCoordinate() {
        return textPositions.isEmpty() ? 0 : textPositions.get(0).getYDirAdj();
    }

    public TextPositionMock getLastTextPosition() {
        return textPositions.isEmpty() ? null : textPositions.get(textPositions.size() - 1);
    }

    public boolean isEmpty() {
        return textPositions.isEmpty();
    }

    public float getLeftPadding() {
        // You could calculate left padding based on xDirAdj or any other criteria
        return textPositions.isEmpty() ? 0 : textPositions.get(0).getXDirAdj();
    }

}



