/**
 * PDFToHTML.java
 * (c) Radek Burget, 2011
 *
 * Pdf2Dom is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * Pdf2Dom is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public License
 * along with CSSBox. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Created on 19.9.2011, 13:34:54 by burgetr
 */
package org.fit.pdfdom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
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

        // Example logging to verify
        // Logger appLogger = LoggerFactory.getLogger(Main.class);
        // appLogger.info("This is an info log."); // This will still log
        // appLogger.error("This is an error log."); // This will still log

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
            System.out.println(infile);

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

            // Read the output of the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

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

                // Print the extracted text lines
                // for (TextLine line2 : textLines) {
                // System.out.println("Line: " + line2.getText() + ", Y: " +
                // line2.getYCoordinate());

                // }

                // List<ConcretePDFBoxTree.TextLine> convertedTextLines = new ArrayList<>();
                // for (PDFToHTML.TextLine textLine : textLines) {
                // ConcretePDFBoxTree.TextLine convertedLine = new
                // ConcretePDFBoxTree.TextLine();
                // // Map properties from PDFToHTML.TextLine to ConcretePDFBoxTree.TextLine
                // // Example:
                // // convertedLine.setSomeProperty(textLine.getSomeProperty());
                // convertedTextLines.add(convertedLine);

                // }
                // parser2.textLinesParser = convertedTextLines;
                // parser2.saveHtmlContentToFile2(htmlContent, convertedTextLines);

                // Assuming containerWidth is defined somewhere earlier in the code

                StringBuilder finalHtmlBuilder = new StringBuilder();
                StringBuilder cssStyles = new StringBuilder();

                // Define some CSS for the page
                cssStyles.append(
                        "<style>.r{color:white;}[class^=\"custom-class-\"]{min-height:16pt}@media(min-width: 1280px){.custom-class-0{margin-top:4pt}.page{border:1px solid blue;width:"
                                + "560pt;}}");

                // Start building the HTML
                finalHtmlBuilder.append(
                        "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\"/>\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n<title>PDF Extracted Text</title>\n<script src=\"https://cdn.tailwindcss.com\"></script>\n<style>")
                        // .append(createFontFaces())
                        .append(
                                "@media(min-width: 1280px){.r{display:block}.custom-class-0{margin-top:4pt}.page{border:1px solid blue;width:"
                                        + "560pt;}}")
                        .append("</style>")
                        // .append(styleContent)
                        .append("</head>\n<body>\n<div class=\"mx-auto sm:max-w-screen-sm md:max-w-screen-md lg:max-w-screen-lg xl:max-w-screen-xl relative p-4 my-10 page\">");

                // Loop through text lines and build HTML structure
                float lineYCoordinate = 0.0f;

                // Assuming textLines is a List<TextLine>
                for (int i = 0; i < textLines.size(); i++) {
                    TextLine line2 = textLines.get(i); // Using textLines instead of textLinesParser
                    lineYCoordinate = line2.getYCoordinate();
                    float leftPaddingPercent = Math.max((line2.getLeftPadding() / 560) * 100, 0);

                    // Define font size range
                    final float MIN_FONT_SIZE = 8f;
                    final float MAX_FONT_SIZE = 32f;

                    // Clamp the font size to a range
                    float fontSize = 12f; // Assuming a static value here for demonstration
                    float clampedFontSize = Math.min(MAX_FONT_SIZE, Math.max(fontSize, MIN_FONT_SIZE));

                    int leftGridSpan = (int) Math.round((leftPaddingPercent / 100) * 12); // Convert percentage to grid
                                                                                          // span
                    int remainingGridSpan = Math.max(12 - leftGridSpan, 1);

                    // Generate the inline style for the text line
                    String inlineStyle = "font-family: Arial, sans-serif; font-weight: normal; font-size: "
                            + Math.round(clampedFontSize) + "pt; height: auto; padding-left: " + leftPaddingPercent
                            + "%;";

                    cssStyles.append(".custom-class-")
                            .append(i)
                            .append(" { ")
                            .append(inlineStyle)
                            .append(" }");

                    // Start building the HTML for this text line
                    finalHtmlBuilder.append("<div class=\"custom-class-").append(i).append("\" data-top=\"")
                            .append(lineYCoordinate).append("\">");

                    finalHtmlBuilder.append("<div class=\"grid grid-cols-12\">");

                    // Add the empty column (left padding equivalent)
                    if (leftGridSpan > 0) {
                        finalHtmlBuilder.append("  <div class=\"col-span-").append(leftGridSpan)
                                .append(" bg-transparent\"></div>");
                    }

                    // Add the text column
                    finalHtmlBuilder.append("<div class=\"col-span-").append(remainingGridSpan)
                            .append(" bg-transparent\">")
                            .append(line2.getText())
                            .append("</div>");

                    finalHtmlBuilder.append("</div></div>");
                }

                // Closing the HTML tags
                finalHtmlBuilder.append("</div></body></html>");

                // Add media queries and finalize the HTML output
                String padLeftpc = "@media(max-width:539px){[class^=\"custom-class-\"]{padding-left:0;height:auto}img{position:relative!important;left:0!important;top:0!important;}.page{width:100%;border:none!important}.r{left:0!important;width:94%!important;}}@media(max-width:1280px){.r{display:none;}}</style>";

                finalHtmlBuilder.append(cssStyles.toString() + padLeftpc);

                String finalHtmlContent = finalHtmlBuilder.toString();

                try (FileWriter fileWriter = new FileWriter("output/" + PDFToHTML.outFile)) {
                    fileWriter.write(finalHtmlContent);
                }

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
            // System.out.println("PDF loaded successfully in PDFToHTML.");

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
        if (lastLine.isEmpty()) return false; // If lastLine is empty, it's a new line

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