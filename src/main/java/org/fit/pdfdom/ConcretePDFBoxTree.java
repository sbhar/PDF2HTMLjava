package org.fit.pdfdom;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
//import org.apache.pdfbox.pdmodel.PDPage;
//import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.fit.pdfdom.resource.HtmlResource;
import org.fit.pdfdom.resource.HtmlResourceHandler;
import org.fit.pdfdom.resource.ImageResource;
import org.fit.pdfdom.resource.SaveResourceToDirHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// import javafx.geometry.Point2D;
// import javafx.geometry.Rectangle2D;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

//import javafx.geometry.Point2D;
//import javafx.geometry.Rectangle2D;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDCIDFont;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1CFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import org.fit.pdfdom.PDFDomTreeConfig;

public abstract class ConcretePDFBoxTree extends PDFBoxTree {

    public List<TextLine> textLines; // List to hold text lines (each line is a list of TextPosition objects)
    private StringBuilder htmlOutput;
    private static final float PAGE_WIDTH = 595.276f;

    private String outputDir = "output";

    // protected PDFDomTreeConfig config;

    protected Document doc;

    public String imageElementsString;
    // public PDFDomTreeConfig config = PDFDomTreeConfig.createDefaultConfig();
    private PDFDomTreeConfig config;

    public String imgSrcString = "images/test.png";

    public String testHtmlOutput = "Bhar";

    String filePath = "test.pdf";

    public String globalStyle = "";

    public String inFile = "";

    private List<String> imgSrcList = new ArrayList<>(); // List to store multiple values

    public ConcretePDFBoxTree(String outputDir) throws IOException {
        super();
        // super().renderImage(path_x, path_start_y, PAGE_WIDTH, path_start_x, null);
        textLines = new ArrayList<>();
        htmlOutput = new StringBuilder();

        // this.outputDir = outputDir;
        outputDir = outputDir;

        // Create the output directory if it does not exist
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Constructor to initialize with a config
    public ConcretePDFBoxTree(PDFDomTreeConfig config) throws IOException {
        this.config = config;
    }

    // New method to retrieve testImgSrc
    public String retrieveTestImgSrc() {
        // System.out.println("retrieveTestImgSrc :" + path);
        // imgSrcString = path;
        if (config != null) {
            return config.getTestImgSrc(); // Access testImgSrc
        }
        return null; // Return null or handle as needed
    }

    // New method to retrieve testImgSrc
    public String retrieveTestHtmlOutput(String testHtmlOutput) {
        System.out.println("retrieveTestImgSrc :" + testHtmlOutput);
        // testHtmlOutput = testHtmlOutput;
        if (config != null) {
            return config.getHtmlOutput(); // Access testImgSrc
        }
        return null; // Return null or handle as needed
    }

    protected void renderImage(float x, float y, float width, float height, ImageResource resource) throws IOException {
        String outputFolder = "output/images";
        String imageFileName = "image_" + System.currentTimeMillis() + ".png";
        File outputFile = new File(outputFolder, imageFileName);

        // Save the image to the output folder
        saveImageToOutputFolder(resource, outputFile);
    }

    // Helper function to save the image resource to a file
    private void saveImageToOutputFolder(ImageResource resource, File outputFile) throws IOException {
        BufferedImage bufferedImage = resource.getImage(); // Convert to BufferedImage
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs(); // Create the output directory if it doesn't exist
        }
        ImageIO.write(bufferedImage, "png", outputFile); // Save the image as a PNG file
    }

    protected Element createPathImage(List<PathSegment> path) throws IOException {
        PathDrawer drawer = new PathDrawer(getGraphicsState());
        ImageResource renderedPath = drawer.drawPath(path);
        return createImageElement((float) renderedPath.getX(), (float) renderedPath.getY(), renderedPath.getWidth(),
                renderedPath.getHeight(), renderedPath);
    }

    protected Element createImageElement(float x, float y, float width, float height, ImageResource resource)
            throws IOException {
        StringBuilder pstyle = new StringBuilder("position:absolute;");
        pstyle.append("left:").append(x).append("pt").append(';');
        pstyle.append("top:").append(y).append("pt").append(';');
        pstyle.append("width:").append(width).append("pt").append(';');
        pstyle.append("height:").append(height).append("pt").append(';');
        Element el = this.doc.createElement("img");
        el.setAttribute("style", pstyle.toString());
        // //System.out.println(pstyle.toString());
        String imgSrc = this.config.getImageHandler().handleResource((HtmlResource) resource);
        if (!this.disableImageData && !imgSrc.isEmpty()) {
            el.setAttribute("src", imgSrc);
        } else {
            el.setAttribute("src", "");
        }
        return el;
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        // Group TextPosition by Y coordinate (to form lines of text)
        if (textLines.isEmpty() || !isSameLine(text, textLines.get(textLines.size() - 1))) {
            // New line starts
            textLines.add(new TextLine());
        }
        textLines.get(textLines.size() - 1).add(text);
        super.processTextPosition(text);
    }

    private boolean isSameLine(TextPosition text, TextLine lastLine) {
        // Check if the current text position is in the same line based on Y-coordinate
        // (allow some margin)
        final float margin = 2.0f; // Margin between lines of text, tweak as needed
        float lastY = lastLine.getLastTextPosition().getYDirAdj();
        return Math.abs(lastY - text.getYDirAdj()) < margin;
    }

    @Override
    protected void endDocument(PDDocument document) throws IOException {
        super.endDocument(document); // Ensure the original endDocument is called

        // Sort lines based on the Y-coordinate (top to bottom)
        Collections.sort(textLines, new Comparator<TextLine>() {
            @Override
            public int compare(TextLine line1, TextLine line2) {
                // Compare by Y position of the first text in the line
                return Float.compare(line1.getFirstTextPosition().getYDirAdj(),
                        line2.getFirstTextPosition().getYDirAdj());
            }
        });

        // Process styles and generate HTML
        postProcessStyles("images/test.png");

        // Print the generated HTML (or write it to a file)
        // System.out.println(htmlOutput.toString());
    }

    /**
     * Post-process text lines to extract and apply styles, and generate HTML.
     * 
     * @throws IOException
     */
    protected void postProcessStyles(String path) throws IOException {
        try {
            float containerWidth = PAGE_WIDTH;

            document = PDDocument.load(new File(PDFToHTML.inFile));

            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            PDPageTree pages = document.getPages();

            // Define the initial CSS styles
            StringBuilder cssStyles = new StringBuilder("");
            cssStyles.append(
                    "<style>@media(min-width: 1280px){.custom-class-0{margin-top:4pt}.page{border:1px solid blue;width:"
                            + containerWidth + "pt;}}");
            // cssStyles.append("<style>@media(min-width: 1280px){.page{border:1px solid
            // blue;width:612pt;}}</style>");

            // imgSrcString = "<img src=\"" + path.replace("output\\", "") + "\"/>";

            String testHtmlOutputTmp = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'/><meta name='viewport' content='width=device-width, initial-scale=1.0'/><title>PDF Extracted Text</title></head><body><div class=\"mx-auto sm:max-w-screen-sm md:max-w-screen-md lg:max-w-screen-lg xl:max-w-screen-xl p-4 my-10 page\">Test</div><script src=\"https://cdn.tailwindcss.com\"></script>";// </body></html>

            // htmlOutput.append(testHtmlOutputTmp);

            htmlOutput.append(
                    "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/><title>PDF Extracted Text</title></head><body>");
            // ;// Tailwind CSS CDN
            // .append(cssStyles); // Append the CSS styles here
            // .append("</head><body><div></div></body></html>");

            for (int i = 0; i < textLines.size(); i++) {
                TextLine line = textLines.get(i);
                TextLine nextLine = (i + 1 < textLines.size()) ? textLines.get(i + 1) : null;
                // Extract font-family and styles
                String fontFamily = line.getFontFamily();
                String fontWeight = "normal";
                String fontStyle = "normal";

                // // Check for style keywords in the font-family string
                // if (fontFamily.toLowerCase().contains("bold")) {
                // fontWeight = "bold";
                // }
                // if (fontFamily.toLowerCase().contains("italic")) {
                // fontStyle = "italic";
                // }

                // // Clean up the font-family by removing known style keywords
                // fontFamily = fontFamily.replaceAll("(?i)(bold|italic)", "").trim();
                // fontFamily = fontFamily.replaceAll("-$", ""); // Remove trailing hyphen, if
                // // any

                // // Handle font-family names with '+' character
                // if (fontFamily.contains("+")) {
                // fontFamily = fontFamily.substring(fontFamily.indexOf('+') + 1);
                // }
                if (fontFamily.contains("+")) {
                    fontFamily = fontFamily.replace("+", " ");
                }

                // // Wrap the font-family in quotes for CSS compatibility
                // fontFamily = "\"" + fontFamily + "\"";

                // float marginBottom = line.getMarginBottom(nextLine);

                float marginBottom = line.getMarginBottom(nextLine) - 8.0f;
                if (marginBottom < 0.0f) {
                    marginBottom = 0.0f; // Ensure margin-bottom is never negative
                }

                float lineWidth = line.getLineWidth();
                if (lineWidth > PAGE_WIDTH) {
                    System.out.println("Warning: Text exceeds the page width. Line width: " +
                            lineWidth);
                }

                // float containerWidth = PAGE_WIDTH;
                float leftPaddingPercent = (line.getLeftPadding() / containerWidth) * 100;
                leftPaddingPercent = Math.max(leftPaddingPercent, 0);

                // Assume containerWidth is the width of the page or container in points
                // float containerWidth = 612.0f; // Example: Letter page width in points

                // Calculate padding-left as a percentage
                // float leftPaddingPercent = (line.getLeftPadding() / containerWidth) * 100;

                // Ensure the percentage value is valid (clamp to minimum of 0)
                leftPaddingPercent = Math.max(leftPaddingPercent, 0);

                String inlineStyle = "font-family: " + fontFamily +
                        "; font-weight: " + fontWeight +
                        // "; font-style: " + fontStyle +
                        "; font-size: " + Math.max(line.getFontSize(), 8) + "pt; " +
                        // "color: " + line.getFontColor() + ";" +
                        //"margin-bottom: " + marginBottom + "pt;" +
                        "margin-bottom: " + marginBottom + "pt;" +
                        // "padding-left: " + line.getLeftPadding() + "pt;" +
                        "padding-left: " + leftPaddingPercent + "%;";

                // Add the inline style to cssStyles as a dynamic class
                cssStyles.append(".custom-class-")
                        .append(i)
                        .append(" { ")
                        .append(inlineStyle)
                        .append(" }");
                // Append styled text with dynamic classes
                htmlOutput.append("<div class='custom-class-").append(i).append("'>");
                for (TextPosition text : line.getTextPositions()) {
                    htmlOutput.append(text.getUnicode()); // Add the text content
                }
                htmlOutput.append("</div>");
            }

            String padLeftpc = "@media(max-width:539px){[class^=\"custom-class-\"]{padding-left:0;}img{position:relative!important;left:0!important;top:0!important}.page{border:none!important}}</style>";

            globalStyle = cssStyles.toString() + padLeftpc;

            // PDFDomTreeConfig config = PDFDomTreeConfig.createDefaultConfig();

            // PDDocument document = null;
            // try {
            // PDFDomTree parser = new PDFDomTree(config);
            // // Load the PDF document
            // document = PDDocument.load(new File(filePath));
            // System.out.println("PDF loaded successfully.");

            // parser.createDOM(document);
            // }catch (Exception e) {
            // System.err.println("Error: " + e.getMessage());
            // e.printStackTrace();
            // } finally {
            // if (document != null) {
            // try {
            // document.close();
            // } catch (IOException e) {
            // System.err.println("Error: " + e.getMessage());
            // // e.printStackTrace();
            // }
            // }
            // }

            PDFDomTreeOld parser = new PDFDomTreeOld(config);
            // Load the PDF document
            document = PDDocument.load(new File(PDFToHTML.inFile));

            PDFToHTML pdftohtml = new PDFToHTML();
            //System.out.println(pdftohtml.infile);
            String[] hardcodedArgs = {
                    PDFToHTML.inFile, // Input file
                    "output.html", // Output file
                    "-fm=EMBED_BASE64", // Font handler mode
                    "-fdir=output/fonts", // Font directory
                    "-im=SAVE_TO_DIR", // Image handler mode
                    "-idir=output/images" // Image directory
            };

            // Call the function with hardcoded arguments
            pdftohtml.callFromOutside(hardcodedArgs);

            // htmlOutput.append("</div></body></html>");
            parser.createDOM(document);
            // imgSrcString2(parser.nimgsrcstring);
            htmlOutput.append(cssStyles.toString() + padLeftpc + "</body></html>");
            // htmlOutput.append(cssStyles.toString()+padLeftpc);
            testHtmlOutput = htmlOutput.toString();
            parser.testHtmlOutput = htmlOutput.toString();
            // pdftohtml.main(hardcodedArgs);
            // parser.endDocument(document);
            // for (String imgTag : parser.ImageTags) {
            // System.out.println("ImageTags @concrete: " + imgTag);
            // }
            

            parser.textLinesParser = textLines;
            parser.writeText(document, output);
            printImgSrcList();
            // System.out.println("HTML file generated: " + testHtmlOutput);
            // Write the HTML output to a file
            String htmlFilePath = outputDir + File.separator + "output.html";
            // testHtmlOutput=path;

            setHtmlOutput(htmlFilePath);
            // try (FileWriter writer = new FileWriter(htmlFilePath)) {
            // try (FileWriter writer = new FileWriter("output/Output_Responsive1.html")) {    
            //     writer.write(htmlOutput.toString());
            //     // this.config.setHtmlOutput(path);
            //     //System.out.println("HTML file generated successfully: " + testHtmlOutput);
            //     document.close();
            // }

        } catch (

        IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // setHtmlOutput(htmlFilePath);
        // try (FileWriter writer = new FileWriter(htmlFilePath)) {
        // writer.write(htmlOutput.toString());
        // // this.config.setHtmlOutput(path);
        // // System.out.println("HTML file generated: " + testHtmlOutput);
        // }
    }

    public void setHtmlOutput(String stringA) {
        testHtmlOutput = htmlOutput.toString();
        // System.out.println("in domtree file : "+ stringA);
        // this.config.setHtmlOutput(stringA);
        // System.out.println("in domtree file : "+ this.config.getTestImgSrc());
    }

    public String imgSrcString2(String stringA) {
        stringA = stringA.replace("<?xml version=\"1.0\" encoding=\"UTF-16\"?>", "");
        // System.out.println("Test imgsrc @concrete " + stringA);
        imgSrcList.add(stringA); // Add each incoming value to the list
        // System.out.println("Added value to list: " + stringA);
        // System.out.println("htmlOutput @concrete imgSrcString2 " + testHtmlOutput);
        return stringA;
    }

    // Method to get all stored values as an array
    public String[] getImgSrcArray() {
        return imgSrcList.toArray(new String[0]); // Convert List to Array
    }

    // Method to print all stored values
    public void printImgSrcList() {
        // System.out.println("All stored values: " + imgSrcList);
    }

    // This method can be used to process the document and return the text as HTML
    public String processDocument(PDDocument document) throws IOException {
        // postProcessStyles("dummy");
        StringWriter writer = new StringWriter();
        writeText(document, writer); // Invoke the writeText method from PDFTextStripper
        return writer.toString();
    }

    public static class TextLine {
        private List<TextPosition> textPositions;
        private String fontFamily;
        private float fontSize;
        private String fontColor;
        private float leftPadding; // Calculated from the X-coordinate of the first text position
        private float marginBottom; // Calculated based on the difference between Y-coordinates of lines

        public TextLine() {
            textPositions = new ArrayList<>();
            fontFamily = "";
            fontSize = 0;
            fontColor = "#000000"; // Default color (black)
            leftPadding = 0;
            marginBottom = 0;
        }

        public void add(TextPosition textPosition) {
            textPositions.add(textPosition);
            extractFontInfo(textPosition);
            if (textPositions.size() == 1) {
                // Set left padding based on the X-coordinate of the first TextPosition
                leftPadding = textPosition.getXDirAdj();
            }
            fontColor = textPosition.getFont().getName().toLowerCase().contains("bold") ? "#000000" : "#666666";
        }

        private void extractFontInfo(TextPosition textPosition) {
            if (fontSize == 0) {
                fontSize = textPosition.getFontSize();
            }
            if (fontFamily.isEmpty()) {
                fontFamily = textPosition.getFont().getName();
            }
        }

        public List<TextPosition> getTextPositions() {
            return textPositions;
        }

        public float getLineWidth() {
            float width = 0.0f;
            for (TextPosition text : textPositions) {
                width += text.getWidth();
            }
            return width;
        }

        public TextPosition getLastTextPosition() {
            return textPositions.isEmpty() ? null : textPositions.get(textPositions.size() - 1);
        }

        public TextPosition getFirstTextPosition() {
            return textPositions.isEmpty() ? null : textPositions.get(0);
        }

        public String getFontFamily() {
            return fontFamily;
        }

        public float getFontSize() {
            return fontSize;
        }

        public String getFontColor() {
            return fontColor;
        }

        public float getLeftPadding() {
            return leftPadding;
        }

        public float getMarginBottom(TextLine nextLine) {
            // Calculate margin bottom as the vertical gap between this line and the next
            if (nextLine != null) {
                float thisLineBottom = this.getFirstTextPosition().getYDirAdj();
                float nextLineTop = nextLine.getFirstTextPosition().getYDirAdj();
                return Math.abs(nextLineTop - thisLineBottom);
            }
            return 0;
        }

        // Method to get the Y-coordinate of the first TextPosition in the line
        public float getYCoordinate() {
            if (!textPositions.isEmpty()) {
                return textPositions.get(0).getYDirAdj();
            }
            return 0; // Return 0 if the line has no text
        }

        // Method to concatenate all text from the TextPositions
        public String getText() {
            StringBuilder textBuilder = new StringBuilder();
            for (TextPosition textPosition : textPositions) {
                textBuilder.append(textPosition.getUnicode());
            }
            return textBuilder.toString();
        }
    }

}
