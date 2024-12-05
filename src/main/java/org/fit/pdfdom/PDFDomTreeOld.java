package org.fit.pdfdom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.TextPosition;
import org.fit.pdfdom.ConcretePDFBoxTree.TextLine;
import org.fit.pdfdom.resource.HtmlResource;
import org.fit.pdfdom.resource.ImageResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import java.util.regex.Matcher;
import java.awt.image.BufferedImage;

public class PDFDomTreeOld extends PDFBoxTree {

  // @Override
  // public void processTree() {
  // // Example usage of imgSrc
  // String imgSrc = getImgSrc();
  // System.out.println("Processing tree with image source: " + imgSrc);
  // }

  private static Logger log = LoggerFactory.getLogger(PDFDomTreeOld.class);

  private Double lastTopValue = null; // Variable to store the last 'top' value

  // StringBuilder to capture the entire HTML string
  private StringBuilder htmlContent = new StringBuilder();

  protected String defaultStyle = ".positionabs{position:absolute}.page{position:relative; border:1px solid blue;margin:0.5em}\n.p,.r{position:absolute;}\n@supports(-webkit-text-stroke: 1px black) {.p{text-shadow:none !important;}}";

  protected Document doc;
  protected Element head;
  protected Element body;
  protected Element title;
  protected Element globalStyle;
  protected Element curpage;
  protected int pagecnt;
  protected PDFDomTreeConfig config;
  protected float lastTop = -1; // To track top values and create new rows when needed
  private int lastWholeTop = -1; // Initialize with a value that won't match any possible `top`
  public String testImgSrc = "";
  public String testHtmlOutput = "";
  public String outputDir = "output";
  public String nimgsrcstring = "Sugata";
  public String ImageContent = "";
  public String FullhtmlContentOld = "";
  protected int textcnt;
  public float dataTopValue = 0.0f;
  public String[] ImageTags;
  public StringBuilder cssStyles = new StringBuilder("");
  public float PAGE_WIDTH = 595.276f;
  public float containerWidth = PAGE_WIDTH;
  private StringBuilder htmlOutput2;

  public List<TextLine> textLinesParser;

  public PDFDomTreeOld() throws IOException, ParserConfigurationException {
    init();
    // //System.out.println(super.getPageStart()) ;
  }

  public PDFDomTreeOld(PDFDomTreeConfig config) throws IOException, ParserConfigurationException {
    this();
    if (config != null)
      this.config = config;
  }

  private void init() throws ParserConfigurationException {
    this.pagecnt = 0;
    this.config = PDFDomTreeConfig.createDefaultConfig();
  }

  protected void createDocument() throws ParserConfigurationException {
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    DocumentType doctype = builder.getDOMImplementation().createDocumentType("html", "-//W3C//DTD XHTML 1.1//EN",
        "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd");
    this.doc = builder.getDOMImplementation().createDocument("http://www.w3.org/1999/xhtml", "html", doctype);
    this.head = this.doc.createElement("head");
    Element meta = this.doc.createElement("meta");
    meta.setAttribute("http-equiv", "content-type");
    meta.setAttribute("content", "text/html;charset=utf-8");
    this.head.appendChild(meta);
    this.title = this.doc.createElement("title");
    this.title.setTextContent("PDF Document");
    this.head.appendChild(this.title);
    this.globalStyle = this.doc.createElement("style");
    this.globalStyle.setAttribute("type", "text/css");
    this.head.appendChild(this.globalStyle);
    this.body = this.doc.createElement("body");
    Element root = this.doc.getDocumentElement();
    root.appendChild(this.head);
    root.appendChild(this.body);
  }

  public void startDocument(PDDocument document) throws IOException {
    try {
      createDocument();
    } catch (ParserConfigurationException e) {
      throw new IOException("Error: parser configuration error", e);
    }
  }

  protected void endDocument(PDDocument document) throws IOException {
    super.endDocument(document); // Call the parent endDocument logic if needed
    String doctitle = document.getDocumentInformation().getTitle();
    if (doctitle != null && doctitle.trim().length() > 0)
      this.title.setTextContent(doctitle);
    this.globalStyle.setTextContent(createGlobalStyle());
  }

  protected void startNewPage() {
    String pstyle = "";
    PDRectangle layout = getCurrentMediaBox();
    if (layout != null) {
      float w = layout.getWidth();
      float h = layout.getHeight();
      int rot = this.pdpage.getRotation();
      if (rot == 90 || rot == 270) {
        float x = w;
        w = h;
        h = x;
      }
      pstyle = "width:" + w + "pt" + ";height:" + h + "pt;";
      pstyle = pstyle + "overflow:hidden;";
    } else {
      log.warn("No media box found");
    }
    this.curpage = this.doc.createElement("div");
    this.curpage.setAttribute("class",
        "mx-auto sm:max-w-screen-sm md:max-w-screen-md lg:max-w-screen-lg xl:max-w-screen-xl p-4 my-10 page");
    this.curpage.setAttribute("style", pstyle);
    this.body.appendChild(this.curpage);
  }

  public double extractTopValue(String style) {
    // Use a regular expression to find the 'top' value in the style string
    Pattern pattern = Pattern.compile("top:([0-9.]+)pt;");
    Matcher matcher = pattern.matcher(style);
    if (matcher.find()) {
      return Double.parseDouble(matcher.group(1));
    }
    return 0.0; // Default value if 'top' is not found
  }

  // Function to create a row element in the current page
  protected Element createRowElement() {
    Element row = this.doc.createElement("div");
    row.setAttribute("class", "d-flex");
    return row;
  }

  // Function to create a column element based on text width
  // Update the createColElement method to accept a String for style
  protected Element createColElement(String style) {
    Element col = this.doc.createElement("div");
    col.setAttribute("class", "");
    col.setAttribute("style", style); // Apply the style directly
    return col;
  }

  // Updated alterRowsBasedOnTopValue method
  protected void alterRowsBasedOnTopValue(float top, String data, String style) {
    // Convert top to an integer to only consider whole number changes
    int wholeNumberTop = (int) top;

    // Check if we need to create a new row based on the integer part of the top
    // value
    if (lastTop != wholeNumberTop) {
      Element newRow = createRowElement();
      newRow.setAttribute("style", "margin-top:" + lastTop + "pt");

      curpage.appendChild(newRow);
      lastTop = wholeNumberTop; // Update lastTop with the integer part of the current top

    }

    // Add content to a new column in the current row
    Element col = createColElement(style);
    Text textNode = this.doc.createTextNode(data);
    col.appendChild(textNode);
    // Append the column to the most recent row
    curpage.getLastChild().appendChild(col);
  }

  // Updated renderText method
  protected void renderText(String data, TextMetrics metrics) {
    this.curpage.appendChild(createTextElement(data, metrics.getWidth()));
  }

  protected Element createTextElement(float width) {
    Element el = this.doc.createElement("div");
    el.setAttribute("id", "p" + this.textcnt++);
    el.setAttribute("class", "p");
    String style = this.curstyle.toString();
    style = style + "width:" + width + "pt" + ";";
    el.setAttribute("style", style);
    return el;
  }

  protected Element createTextElement(String data, float width) {
    Element el = createTextElement(width);
    Text text = this.doc.createTextNode(data);
    el.appendChild(text);
    return el;
  }

  public void writeText(PDDocument doc, Writer outputStream) throws IOException {
    try {
      // System.out.println(inFile);
      PDDocument document = null;
      document = PDDocument.load(new File(PDFToHTML.inFile));
      createDOM(doc);
      // System.out.println("img content @pdfdomtree: " + nimgsrcstring);
      nimgsrcstring = nimgsrcstring.replace("<?xml version=\"1.0\" encoding=\"UTF-16\"?>", "");
      // System.out.println("img content @pdfdomtree: " + nimgsrcstring);
      // System.out.println("testHtmlOutput @pdfdomtree: " + testHtmlOutput);
      // Convert the testHtmlOutput string into a DOM Document
      Document htmlDocument = parseHtmlToDom(testHtmlOutput.replaceAll("<\\?xml[^>]*>", ""));

      // // String testHtmlOutput = "<!DOCTYPE
      // html><html><head><metacharset=\"UTF-8\"/><title>Sample
      // Page</title></head><body><h1>Welcome to theSample Page</h1><p>This is a dummy
      // paragraph for testing.</p><ul><li>Item1</li><li>Item 2</li><li>Item
      // 3</li></ul></body></html>";

      // // Convert the testHtmlOutput string into a DOM Document
      // Document htmlDocument = parseHtmlToDom(testHtmlOutput);

      DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
      DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
      LSSerializer writer = impl.createLSSerializer();
      LSOutput output = impl.createLSOutput();
      writer.getDomConfig().setParameter("format-pretty-print",
          Boolean.valueOf(true));
      output.setCharacterStream(outputStream);
      // createDOM(doc);
      createDOM(doc);
      FullhtmlContentOld = pdDocumentToHtmlString(doc);
      // try (FileWriter fileWriter = new FileWriter("FullhtmlContentOld.html")) {
      // fileWriter.write(FullhtmlContentOld);
      // System.out.println("Html content successfully saved to
      // FullhtmlContentOld.html");
      // } catch (IOException e) {
      // System.err.println("Error while writing to file: " + e.getMessage());
      // }
      writer.write(getDocument(), output);
      // writer.write(htmlDocument, output); // Write the DOM content to the
      // outputstream
      // Also write to an HTML file
      saveHtmlContentToFile(doc);

    } catch (Exception e) {
      throw new IOException("Error: cannot initialize the DOM serializer", e);
    }
  }

  public void saveHtmlContentToFile(PDDocument doc) throws IOException {
    try {
      createDOM(doc);

      String fullHtmlContent = pdDocumentToHtmlString(doc);
      fullHtmlContent = fullHtmlContent.replace("<?xml version=\"1.0\" encoding=\"UTF-16\"?>", "");

      String imgTagPattern = "<img[^>]*>";
      Pattern pattern = Pattern.compile(imgTagPattern);
      Matcher matcher = pattern.matcher(fullHtmlContent);

      StringBuilder imageContentBuilder = new StringBuilder();
      int imgCounter = 1;
      float dataTopValue = 0.0f;

      while (matcher.find()) {
        String originalImgTag = matcher.group();

        // Extract the `top` value
        Pattern topPattern = Pattern.compile("style=\"[^\"]*top:\\s*([\\d\\.]+)pt;[^\"]*\"");
        Matcher topMatcher = topPattern.matcher(originalImgTag);
        if (topMatcher.find()) {
          String topValueStr = topMatcher.group(1);
          try {
            dataTopValue = Float.parseFloat(topValueStr);
          } catch (NumberFormatException e) {
            System.err.println("Error parsing top value: " + topValueStr);
          }
        }

        // Extract the `left` value
        float leftValue = 0.0f;
        Pattern leftPattern = Pattern.compile("style=\"[^\"]*left:\\s*([\\d\\.]+)pt;[^\"]*\"");
        Matcher leftMatcher = leftPattern.matcher(originalImgTag);
        if (leftMatcher.find()) {
          String leftValueStr = leftMatcher.group(1);
          try {
            leftValue = Float.parseFloat(leftValueStr);
          } catch (NumberFormatException e) {
            System.err.println("Error parsing left value: " + leftValueStr);
          }
        }

        // Determine alignment based on `leftValue` and `containerWidth`
        String alignmentClass = ""; // Tailwind classes or inline styles
        if (leftValue > containerWidth / 2) {
          alignmentClass = "ml-0 md:ml-auto"; // Right-aligned (Tailwind class)
        } else if (Math.abs(leftValue - containerWidth / 2) <= 50) {
          alignmentClass = "mx-auto"; // Center-aligned (Tailwind class)
        }

        // Append `class` or `style` with alignment
        if (originalImgTag.contains("class=")) {
          String modifiedImgTag = originalImgTag.replaceFirst("class=\"([^\"]*)\"",
              "class=\"$1 img" + imgCounter + " " + alignmentClass + "\" data-top=\"" + dataTopValue + "\"");
          imageContentBuilder.append(modifiedImgTag).append("\n");
        } else {
          String modifiedImgTag = originalImgTag.replaceFirst("<img",
              "<img class=\"img" + imgCounter + " " + alignmentClass + "\" data-top=\"" + dataTopValue + "\"");
          imageContentBuilder.append(modifiedImgTag).append("\n");
        }

        imgCounter++;
      }

      String imageContent = imageContentBuilder.toString();
      imageContent = imageContent.replace("<?xml version=\"1.0\" encoding=\"UTF-16\"?>", "");
      String[] imageTags = imageContent.split("\n");

      // Extract <div> tags with class="r" and content "&nbsp;"
      String divPattern = "(?i)<div[^>]*class=\"r\"[^>]*>.*?</div>";

      // System.out.println(FullhtmlContentOld);

      Pattern divRegex = Pattern.compile(divPattern, Pattern.DOTALL);
      Matcher divMatcher = divRegex.matcher(FullhtmlContentOld);

      StringBuilder DivContentBuilder = new StringBuilder();

      while (divMatcher.find()) {
        String originalDivTag = divMatcher.group();

        Pattern topPattern = Pattern.compile("style=\"[^\"]*top:\\s*([\\d\\.]+)pt;[^\"]*\"");
        Matcher topMatcher = topPattern.matcher(originalDivTag);
        // float dataTopValue = 0.0f;
        if (topMatcher.find()) {
          String topValueStr = topMatcher.group(1);
          try {
            dataTopValue = Float.parseFloat(topValueStr);
          } catch (NumberFormatException e) {
            System.err.println("Error parsing top value: " + topValueStr);
          }
        }

        String modifiedDivTag = originalDivTag.replaceFirst("(<div[^>]*)>", "$1 data-top=\"" + dataTopValue + "\">");
        DivContentBuilder.append(modifiedDivTag).append("\n");
      }

      String DivContent = DivContentBuilder.toString();
      String[] DivTags = DivContent.split("\n");

      String bodyTagPattern = "(?i)<body.*?>(.*?)</body>";
      Pattern patternBody = Pattern.compile(bodyTagPattern, Pattern.DOTALL);
      Matcher matcherBody = patternBody.matcher(fullHtmlContent);

      if (matcherBody.find()) {
        String bodyContent = matcherBody.group(1);

        String styleTagPattern = "<style[^>]*>.*?</style>";
        Pattern patternStyle = Pattern.compile(styleTagPattern, Pattern.DOTALL);
        Matcher matcherStyle = patternStyle.matcher(fullHtmlContent);

        StringBuilder styleContentBuilder = new StringBuilder();
        while (matcherStyle.find()) {
          styleContentBuilder.append(matcherStyle.group()).append("\n");
        }

        String styleContent = styleContentBuilder.toString();
        StringBuilder finalHtmlBuilder = new StringBuilder();

        StringBuilder cssStyles = new StringBuilder("");

        cssStyles.append(
            "<style>.r{color:white;}[class^=\"custom-class-\"]{min-height:16pt}@media(min-width: 1280px){.custom-class-0{margin-top:4pt}.page{border:1px solid blue;width:"
                + containerWidth + "pt;}}");

        finalHtmlBuilder.append(
            "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\"/>\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n<title>PDF Extracted Text</title>\n<script src=\"https://cdn.tailwindcss.com\"></script>\n<style>")
            .append(createFontFaces())
            .append(
                "@media(min-width: 1280px){.r{display:block}.custom-class-0{margin-top:4pt}.page{border:1px solid blue;width:"
                    + containerWidth + "pt;}}")
            .append("</style>")
            .append(styleContent)
            .append(
                "</head>\n<body>\n<div class=\"mx-auto sm:max-w-screen-sm md:max-w-screen-md lg:max-w-screen-lg xl:max-w-screen-xl relative p-4 my-10 page\">");

        for (int i = 0; i < textLinesParser.size(); i++) {
          TextLine line = textLinesParser.get(i);
          float lineYCoordinate = line.getYCoordinate();
          float marginBottom = Math.max(
              line.getMarginBottom((i + 1 < textLinesParser.size()) ? textLinesParser.get(i + 1) : null) - 8.0f, 0.0f);
          float leftPaddingPercent = Math.max((line.getLeftPadding() / containerWidth) * 100, 0);

          // Define the minimum and maximum font sizes
final float MIN_FONT_SIZE = 8f;
final float MAX_FONT_SIZE = 32f;

// Clamp the font size to a range
float fontSize = line.getFontSize();
float clampedFontSize = Math.min(MAX_FONT_SIZE, Math.max(fontSize, MIN_FONT_SIZE));

// Generate the inline style
String inlineStyle = "font-family: " + line.getFontFamily().replace("+", " ") +
                     "; font-weight: normal; font-size: " + Math.round(clampedFontSize) + "pt; height: auto" +
                     "; padding-left: " + leftPaddingPercent + "%;";


          // String inlineStyle = "font-family: " + line.getFontFamily().replace("+", " ") +
          //     "; font-weight: normal; font-size: " + Math.max(line.getFontSize(), 8) + "pt;height: auto"+
          //     "; padding-left: " + leftPaddingPercent + "%;";

          float maxTopValue = Float.MIN_VALUE; // Initialize the highest top value
          int selectedIndex = -1; // Store the index of the selected div
          String selectedBackgroundColor = null; // To store the background-color of the selected div
          float diff = Float.MIN_VALUE;
          float diff2 = Float.MIN_VALUE;
          String selectedBorderBottom = null;
          int selectedIndex2 = -1;

          // First Pass: Find the div with the highest top value
          for (int k = 0; k < DivTags.length; k++) {
            String divTag = DivTags[k];
            String dataTopAttr = extractDataTopValue(divTag);

            if (dataTopAttr != null && isWithinRange(Float.parseFloat(dataTopAttr), lineYCoordinate, 8.9f)) {
              float currentTopValue = Float.parseFloat(dataTopAttr); // Parse the current top value
              // Check if maxTopValue contains 'E' (indicating scientific notation)
              String maxTopValueStr = String.valueOf(maxTopValue);
              // if (maxTopValue != (1.4E-45) && currentTopValue > maxTopValue) {
              diff = lineYCoordinate - currentTopValue;

              // System.out.println("data-top: " + dataTopAttr + "---" + "Current:" + currentTopValue + "---" + "Max:"
              //     + maxTopValue + "---" + "Y Coordinate:" + lineYCoordinate);
              // System.out.println("Index: " + k + " --- " + "diff:" + diff);
              if (lineYCoordinate - currentTopValue < 8.9f && currentTopValue > maxTopValue) {
                maxTopValue = currentTopValue; // Update maxTopValue
                selectedIndex = i + 1; // Store the index of the selected div
                selectedBackgroundColor = extractBackgroundColor(divTag); // Extract the background-color
                // selectedBorderBottom = extractBorderBottom(divTag); // Extract the
                // border-bottom
                // System.out.println("border bottom: "+selectedBorderBottom);
                // selectedIndex2 = i;
              }

            }
            if (dataTopAttr != null && isWithinRange(Float.parseFloat(dataTopAttr), lineYCoordinate, 30.9f)) {
             
              float currentTopValue = Float.parseFloat(dataTopAttr); // Parse the current top value 
              
              // Check if maxTopValue contains 'E' (indicating scientific notation)
              // if (maxTopValue != (1.4E-45) && currentTopValue > maxTopValue) {
              diff2 = lineYCoordinate - currentTopValue;
              
              if (currentTopValue > maxTopValue) {
                maxTopValue = currentTopValue;
                selectedBorderBottom = extractBorderBottom(divTag); // Extract the border-bottom
                //System.out.println("border bottom: " + selectedBorderBottom);
                selectedIndex2 = i;
              }
              // System.out.println("data-top: " + dataTopAttr + "---" + "Current:" + currentTopValue + "---" + "Max:"
              //     + maxTopValue + "---" + "Y Coordinate:" + lineYCoordinate + "---" + "diff:" + diff2+"---" + "selectedIndex2:" + selectedIndex2 );
            }
          }

          // // // Apply the background-color from the selected div to inlineStyle, if any
          if ((selectedBackgroundColor != null) && (selectedIndex != -1) && (diff > 0.0)) {
            inlineStyle += " background-color: " + selectedBackgroundColor + "; height:auto;";
            
          }

          if (selectedIndex2 == 2) {

           
              inlineStyle += " border-bottom: " + "1px solid" + ";height:14pt" + ";width:" + line.getLineWidth() + "pt;" + "padding-left:0;margin-left:auto;margin-right:auto;width:fit-content;";
            
          }

          // Add the inline style to cssStyles as a dynamic class
          cssStyles.append(".custom-class-")
              .append(i)
              .append(" { ")
              .append(inlineStyle)
              .append(" }");

          finalHtmlBuilder.append("<div class=\"custom-class-").append(i)
              // .append("\" style=\"")
              .append("\">").append(line.getText());

          for (String imgTag : imageTags) {
            String dataTopAttr = extractDataTopValue(imgTag);
            if (dataTopAttr != null && !dataTopAttr.isEmpty()) {
              try {
                  dataTopValue = Float.parseFloat(dataTopAttr);
                  if (isWithinRange(dataTopValue, lineYCoordinate, 35.0f)) {
                      finalHtmlBuilder.append(imgTag);
                      break;
                  }
              } catch (NumberFormatException e) {
                  System.err.println("Invalid float value for dataTopAttr: " + dataTopAttr);
                  // Handle the invalid value as needed, e.g., skip or use a default
              }
          } else {
              //System.err.println("dataTopAttr is null or empty");
              // Handle the null or empty case as needed, e.g., skip or use a default
          }
            
          }

          for (String divTag : DivTags) {
            String dataTopAttr = extractDataTopValue(divTag);
            if (isWithinRange(Float.parseFloat(dataTopAttr), lineYCoordinate, 10.9f)) {
              // finalHtmlBuilder.append(divTag);
              break;
            }
          }

          finalHtmlBuilder.append("</div>");
        }

        String padLeftpc = "@media(max-width:539px){[class^=\"custom-class-\"]{padding-left:0;height:auto}img{position:relative!important;left:0!important;top:0!important;}.page{width:100%;border:none!important}.r{left:0!important;width:94%!important;}}@media(max-width:1280px){.r{display:none;}}</style>";

        finalHtmlBuilder.append(cssStyles.toString() + padLeftpc + "</body></html>");

        String finalHtmlContent = finalHtmlBuilder.toString();

        // fileWriter.write(finalHtmlContent);

        // Add 16pt to the top value and z-index:-1 for specific div tags
        Pattern divPattern2 = Pattern.compile(
            "(?i)<div([^>]*?class=\"r\"[^>]*?)style\\s*=\\s*\"([^\"]*?top:\\s*)([\\d\\.]+)pt(.*?background-color:\\s*(?!#ffffff)[^;]+;[^\"]*?)\"");
        Matcher divMatcher2 = divPattern2.matcher(finalHtmlContent);
        StringBuffer modifiedContent = new StringBuffer();

        while (divMatcher2.find()) {
          String prefix = divMatcher2.group(1);
          String styleBeforeTop = divMatcher2.group(2);
          float topValue = Float.parseFloat(divMatcher2.group(3));
          String styleAfterTop = divMatcher2.group(4);
          float newTopValue = topValue + 16;

          String replacement = "<div" + prefix + "style=\"" + styleBeforeTop + newTopValue + "pt" + styleAfterTop
              + " z-index:-1\"";
          divMatcher2.appendReplacement(modifiedContent, replacement);
        }
        divMatcher2.appendTail(modifiedContent);
        finalHtmlContent = modifiedContent.toString();

        // Subtract 16pt from the top value for div tags where top value matches
        // data-top attribute
        Pattern divPattern3 = Pattern.compile(
            "(?i)<div([^>]*?class=\"r\"[^>]*?)style\\s*=\\s*\"([^\"]*?top:\\s*)([\\d\\.]+)pt([^>]*?)\"[^>]*?data-top=\"([\\d\\.]+)\"");

        Matcher divMatcher3 = divPattern3.matcher(finalHtmlContent);
        StringBuffer modifiedContent3 = new StringBuffer();

        while (divMatcher3.find()) {
          String prefix = divMatcher3.group(1); // Group 1: part before 'top'
          String styleBeforeTop = divMatcher3.group(2); // Group 2: style before the 'top' value
          float topValue = Float.parseFloat(divMatcher3.group(3)); // Group 3: top value (as a float)
          String styleAfterTop = divMatcher3.group(4); // Group 4: part after 'top'
          float dataTopValue3 = Float.parseFloat(divMatcher3.group(5)); // Group 5: data-top value (as a float)

          // Only modify if the top value (with 'pt') matches the data-top attribute
          if (topValue == dataTopValue3) {
            // Subtract 16pt from the top value
            float newTopValue = topValue + 8;

            // Build the replacement string with the modified top value
            String replacement = "<div" + prefix + "style=\"" + styleBeforeTop + newTopValue + "pt" + styleAfterTop
                + "\" data-top=\"" + dataTopValue + "\"";

            // Append the modified div to the output
            divMatcher3.appendReplacement(modifiedContent3, replacement);
          } else {
            // If the top value doesn't match, leave the div unchanged
            divMatcher3.appendReplacement(modifiedContent3, divMatcher3.group(0));
          }
        }
        divMatcher3.appendTail(modifiedContent3);

        // Update the final HTML content with the modifications
        finalHtmlContent = modifiedContent3.toString();

        // Check for missing <img> tags and add them at the top of <body> if any are
        // missing
        for (String imgTag : imageTags) {
          if (!finalHtmlContent.contains(imgTag)) {
            finalHtmlContent = finalHtmlContent.replaceFirst("(?i)<div.*?>",
                "$0" + imgTag + "\n");
          }
        }

        try (FileWriter fileWriter = new FileWriter("output/Output_Responsive.html")) {
          finalHtmlContent = finalHtmlContent.replaceAll(
              "(?i)<img([^>]*?)style\\s*=\\s*\"([^\"]*?)position\\s*:\\s*absolute([^>]*?)\"", "<img$1style=\"$2$3\"");
          finalHtmlContent = finalHtmlContent.replaceAll(
              "(?i)<img([^>]*?)style\\s*=\\s*\"([^\"]*?)left\\s*:\\s*[^;]+;([^>]*?)\"", "<img$1style=\"$2$3\"");
          finalHtmlContent = finalHtmlContent.replaceAll(
              "(?i)<img([^>]*?)style\\s*=\\s*\"([^\"]*?)top\\s*:\\s*[^;]+;([^>]*?)\"", "<img$1style=\"$2$3\"");
          finalHtmlContent = finalHtmlContent.replaceAll(
              "(?i)<div[^>]*class=\"r\"[^>]*style\\s*=\\s*\"[^\"]*background-color:\\s*#ffffff;[^\"]*\"[^>]*>.*?</div>",
              "");

          finalHtmlContent = finalHtmlContent.replaceAll(
              "(?i)(<div[^>]*class=\"r\"[^>]*>)(.*?)(</div>)", "$1&nbsp;$3");

          fileWriter.write(finalHtmlContent);
          System.out.println("HTML content successfully saved to Output_Responsive.html");
        } catch (IOException e) {
          System.err.println("Error while writing to file: " + e.getMessage());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (doc != null) {
        try {
          doc.close();
        } catch (IOException e) {
          System.err.println("Error closing PDF document: " + e.getMessage());
        }
      }
    }
  }

  // Classes for image data and text lines
  class ImageData {
    private float dataTop;
    private String imgTag;

    public ImageData(float dataTop, String imgTag) {
      this.dataTop = dataTop;
      this.imgTag = imgTag;
    }

    public float getDataTop() {
      return dataTop;
    }

    public String getImgTag() {
      return imgTag;
    }
  }

  private String extractBorderBottom(String divTag) {
    // Pattern to match the border-bottom property
    Pattern borderBottomPattern = Pattern.compile("border-bottom:\\s*([^;]+);");
    Matcher matcher = borderBottomPattern.matcher(divTag);
    if (matcher.find()) {
      return matcher.group(1).trim(); // Return the value of border-bottom
    }
    return null; // Return null if border-bottom is not found
  }

  private String extractBackgroundColor(String divTag) {
    Pattern backgroundColorPattern = Pattern.compile("background-color:\\s*([^;]+);");
    Matcher matcher = backgroundColorPattern.matcher(divTag);
    if (matcher.find()) {
      return matcher.group(1).trim(); // Return the value of background-color
    }
    return null; // Return null if background-color is not found
  }

  private String extractDataTopValue(String imgTag) {
    // Use a regex to extract the value of the data-top attribute
    Pattern pattern = Pattern.compile("data-top\\s*=\\s*\"(.*?)\"");
    Matcher matcher = pattern.matcher(imgTag);
    if (matcher.find()) {
      return matcher.group(1); // Return the value of the data-top attribute
    }
    return null; // Return null if the attribute is not found
  }

  private Map<Float, String> mapDivsToYCoordinates(String fullHtmlContent, List<TextLine> textLines) {
    Map<Float, String> yToDivMap = new HashMap<>();

    // Regex to identify <div> tags
    String divPattern = "<div[^>]*>(.*?)</div>";
    Pattern pattern = Pattern.compile(divPattern, Pattern.DOTALL);
    Matcher matcher = pattern.matcher(fullHtmlContent);

    // Iterate through all div tags
    while (matcher.find()) {
      String divContent = matcher.group(); // The full <div> tag with its content
      String divInnerText = matcher.group(1).trim(); // The inner text of the div

      // Find the Y-coordinate for this div's text by matching it with TextLine
      // objects
      for (int i = 0; i < textLinesParser.size(); i++) {
        if (divInnerText.contains(textLinesParser.get(i).getText())) {
          // Map the Y-coordinate of the text to the <div> content
          yToDivMap.put(textLinesParser.get(i).getYCoordinate(), divContent);
          break; // Break after the first match to avoid duplicates
        }
      }
    }

    return yToDivMap;
  }

  private boolean isWithinRange(float value, float target, float tolerance) {
    return Math.abs(value - target) <= tolerance;
  }

  private float extractYCoordinateFromImgTag(String imgTag) {
    String dataTopPattern = "data-top=\"([0-9.]+)\"";
    Pattern pattern = Pattern.compile(dataTopPattern);
    Matcher matcher = pattern.matcher(imgTag);

    if (matcher.find()) {
      return Float.parseFloat(matcher.group(1));
    }

    return -1; // Return -1 if no value is found
  }

  private String findDivForYCoordinate(float y, Map<Float, String> yToDivMap) {
    // Locate the closest matching div for the given Y-coordinate
    float closestY = -1;
    String targetDiv = null;

    for (Map.Entry<Float, String> entry : yToDivMap.entrySet()) {
      float currentY = entry.getKey();
      System.out.println("CurrentY: " + currentY);
      if (closestY == -1 || Math.abs(y - currentY) < Math.abs(y - closestY)) {
        closestY = currentY;
        System.out.println("ClosestY: " + closestY);
        targetDiv = entry.getValue();
      }
    }

    return targetDiv;
  }

  private String insertImgTagIntoDiv(String fullHtmlContent, String targetDiv, String imgTag, String testHtmlOutput) {
    if (targetDiv == null || imgTag == null)
      return fullHtmlContent;

    String divPattern = "(<div[^>]*" + Pattern.quote(targetDiv) + "[^>]*>)";
    Pattern pattern = Pattern.compile(divPattern);
    Matcher matcher = pattern.matcher(fullHtmlContent);

    if (matcher.find()) {
      String divStartTag = matcher.group(1);
      return testHtmlOutput.replace(divStartTag, divStartTag + imgTag);
    }

    return testHtmlOutput;
  }

  public String pdDocumentToHtmlString(PDDocument doc) throws IOException {
    try {
      // Step 1: Create a DOM structure from the PDDocument
      createDOM(doc); // Ensure this method generates the DOM structure you need

      // Step 2: Get the generated DOM Document
      Document htmlDocument = getDocument(); // This should return the populated DOM Document

      // Step 3: Convert the DOM Document to an HTML string
      DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
      DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");

      // Create a serializer
      LSSerializer writer = impl.createLSSerializer();

      // Enable pretty-printing for human-readable output (optional)
      writer.getDomConfig().setParameter("format-pretty-print", Boolean.FALSE);

      // Serialize the DOM Document to a string
      StringWriter stringWriter = new StringWriter();
      LSOutput output = impl.createLSOutput();
      output.setCharacterStream(stringWriter);
      writer.write(htmlDocument, output);

      // Return the resulting HTML string
      return stringWriter.toString();
    } catch (Exception e) {
      throw new IOException("Error converting PDDocument to HTML string", e);
    }
  }
  // public void writeText(PDDocument doc, Writer outputStream) throws IOException
  // {
  // try {
  // createDOM(doc);
  // // System.out.println("img content @pdfdomtree: " + nimgsrcstring);
  // nimgsrcstring = nimgsrcstring.replace("<?xml version=\"1.0\"
  // encoding=\"UTF-16\"?>", "");
  // System.out.println("img content @pdfdomtree: " + nimgsrcstring);

  // // testHtmlOutput+"</div>"+nimgsrcstring+"</body></html>"

  // // Convert the testHtmlOutput string into a DOM Document
  // Document htmlDocument =
  // parseHtmlToDom(testHtmlOutput.replaceAll("<\\?xml[^>]*>", ""));

  // // Set up the DOM serializer
  // DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
  // DOMImplementationLS impl = (DOMImplementationLS)
  // registry.getDOMImplementation("LS");
  // LSSerializer writer = impl.createLSSerializer();
  // LSOutput output = impl.createLSOutput();
  // writer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);

  // // Write htmlDocument to the provided outputStream
  // output.setCharacterStream(outputStream);
  // // createDOM(doc);

  // writer.write(htmlDocument, output);

  // // Also write htmlDocument to an HTML file
  // try (FileWriter fileWriter = new FileWriter("output2.html")) {
  // output.setCharacterStream(fileWriter);
  // writer.write(htmlDocument, output);
  // System.out.println("HTML content successfully written to output2.html");
  // }

  // } catch (Exception e) {
  // throw new IOException("Error: cannot initialize the DOM serializer", e);
  // }
  // }

  // Helper method to parse HTML string into a DOM Document
  private Document parseHtmlToDom(String htmlString) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true); // Enable namespaces if required
    DocumentBuilder builder = factory.newDocumentBuilder();

    try (StringReader reader = new StringReader(htmlString)) {
      return builder.parse(new org.xml.sax.InputSource(reader));
    }
  }

  public Document createDOM(PDDocument doc) throws IOException {
    // Using OutputStreamWriter to write directly to System.out
    OutputStreamWriter writer = new OutputStreamWriter(System.out);
    super.writeText(doc, writer);
    writer.flush(); // Ensure all data is written out to System.out
    // //System.out.println(super.getPageStart());
    return this.doc;
  }

  public Document getDocument() {
    return this.doc;
  }

  protected void renderPath(List<PathSegment> path, boolean stroke, boolean fill) throws IOException {
    float[] rect = toRectangle(path);
    if (rect != null) {
      this.curpage.appendChild(createRectangleElement(rect[0], rect[1], rect[2] -
          rect[0], rect[3] - rect[1], stroke, fill));
    } else if (stroke) {
      for (PathSegment segm : path) {
        this.curpage.appendChild(createLineElement(segm.getX1(), segm.getY1(),
            segm.getX2(), segm.getY2()));
      }
    } else {
      this.curpage.appendChild(createPathImage(path));
    }
  }

  public Element imageElements;

  public void renderImage(float x, float y, float width, float height, ImageResource resource) throws IOException {

    // Save the image to the output folder
    String outputFolder = "output\\images";
    String imageFileName = "test" + System.currentTimeMillis() + ".png";
    File outputFile = new File(outputFolder, imageFileName);

    // saveImageToOutputFolder(resource, outputFile);

    this.curpage.appendChild(createImageElement(x, y, width, height, resource));

    // System.out.println("Total HTML content @renderImage: " +testHtmlOutput);
  }

  public static String elementToHtmlString(Element element) {
    try {
      // Obtain a DOMImplementationLS instance
      DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
      DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");

      // Create a serializer
      LSSerializer serializer = impl.createLSSerializer();

      // Enable pretty-printing (optional, can be set to false for minified output)
      serializer.getDomConfig().setParameter("format-pretty-print", Boolean.FALSE);

      // Serialize the element to a string
      return serializer.writeToString(element);
    } catch (Exception e) {
      e.printStackTrace();
      return null; // Return null in case of an error
    }
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

  public void setTestImgSrc(String newImgSrc) {
    testImgSrc = newImgSrc;
  }

  public Element createImageElement(float x, float y, float width, float height, ImageResource resource)
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
    setTestImgSrc(imgSrc);

    config.setTestImgSrc(imgSrc);
    // MyConcretePDFBoxTree processor = new MyConcretePDFBoxTree("@pdfdomtree");
    // config.setTestHtmlOutput(processor.testHtmlOutput);

    if (!this.disableImageData && !imgSrc.isEmpty()) {
      el.setAttribute("src", imgSrc);
      MyConcretePDFBoxTree processor = new MyConcretePDFBoxTree("output");
      processor.imgSrcString2(elementToHtmlString(el));
      nimgsrcstring = elementToHtmlString(el);
      // processor.postProcessStyles("images/test.png");
      // System.out.println("Total styles content @pdfdomtree: " +
      // processor.globalStyle);
      // elementToHtmlString(el));
    } else {
      el.setAttribute("src", "");
    }
    return el;
  }

  protected Element createRectangleElement(float x, float y, float width, float height, boolean stroke, boolean fill) {
    float lineWidth = transformWidth(getGraphicsState().getLineWidth());
    float wcor = stroke ? lineWidth : 0.0F;
    float strokeOffset = (wcor == 0.0F) ? 0.0F : (wcor / 2.0F);
    width = (width - wcor < 0.0F) ? 1.0F : (width - wcor);
    height = (height - wcor < 0.0F) ? 1.0F : (height - wcor);

    // Build the style attribute
    StringBuilder pstyle = new StringBuilder(50);
    pstyle.append("left:").append(this.style.formatLength(x - strokeOffset)).append(';');
    pstyle.append("top:").append(this.style.formatLength(y - strokeOffset)).append(';');
    pstyle.append("width:").append(this.style.formatLength(width)).append(';');
    pstyle.append("height:").append(this.style.formatLength(height)).append(';');
    if (stroke) {
      String color = colorString(getGraphicsState().getStrokingColor());
      pstyle.append("border:").append(this.style.formatLength(lineWidth)).append(" solid ").append(color).append(';');
    }

    String fcolor = null; // Capture the background color if fill is true
    if (fill) {
      fcolor = colorString(getGraphicsState().getNonStrokingColor());
      pstyle.append("background-color:").append(fcolor).append(';');
    }

    // Save the `top` value and `fcolor` as attributes
    String topValueWithoutPt = this.style.formatLength(y - strokeOffset).replace("pt", "");

    Element el = this.doc.createElement("div");
    el.setAttribute("class", "r");
    // el.setAttribute("data-top", topValueWithoutPt); // Save the top value without
    // `pt`
    if (fcolor != null) {
      el.setAttribute("data-bgcolor", fcolor); // Save the background color
    }
    el.setAttribute("style", pstyle.toString());

    // Append a non-breaking space to the div
    el.appendChild(this.doc.createTextNode("\u00A0"));

    return el;
  }

  protected Element createLineElement(float x1, float y1, float x2, float y2) {
    HtmlDivLine line = new HtmlDivLine(x1, y1, x2, y2);
    String color = colorString(getGraphicsState().getStrokingColor());
    StringBuilder pstyle = new StringBuilder(50);
    pstyle.append("left:").append(this.style.formatLength(line.getLeft())).append(';');
    pstyle.append("top:").append(this.style.formatLength(line.getTop())).append(';');
    pstyle.append("width:").append(this.style.formatLength(line.getWidth())).append(';');
    pstyle.append("height:").append(this.style.formatLength(line.getHeight())).append(';');
    pstyle.append(line.getBorderSide()).append(':').append(this.style.formatLength(line.getLineStrokeWidth()))
        .append(" solid ").append(color).append(';');
    if (line.getAngleDegrees() != 0.0D)
      pstyle.append("transform:").append("rotate(").append(line.getAngleDegrees()).append("deg);");

    // Create the 'div' element
    Element el = this.doc.createElement("div");
    el.setAttribute("class", "r");
    el.setAttribute("style", pstyle.toString()); // Apply the styles to the element

    // Ensure only the non-breaking space is appended and no other nested elements.
    el.appendChild(this.doc.createTextNode("\u00A0")); // Unicode for non-breaking space

    return el;
  }

  protected class HtmlDivLine {
    private final float x1;

    private final float y1;

    private final float x2;

    private final float y2;

    private final float width;

    private final float height;

    private final boolean horizontal;

    private final boolean vertical;

    public HtmlDivLine(float x1, float y1, float x2, float y2) {
      this.x1 = x1;
      this.y1 = y1;
      this.x2 = x2;
      this.y2 = y2;
      this.width = Math.abs(x2 - x1);
      this.height = Math.abs(y2 - y1);
      this.horizontal = (this.height < 0.5F);
      this.vertical = (this.width < 0.5F);
    }

    public float getHeight() {
      return this.vertical ? this.height : 0.0F;
    }

    public float getWidth() {
      if (this.vertical)
        return 0.0F;
      if (this.horizontal)
        return this.width;
      return distanceFormula(this.x1, this.y1, this.x2, this.y2);
    }

    public float getLeft() {
      if (this.horizontal || this.vertical)
        return Math.min(this.x1, this.x2);
      return Math.abs((this.x2 + this.x1) / 2.0F) - getWidth() / 2.0F;
    }

    public float getTop() {
      if (this.horizontal || this.vertical)
        return Math.min(this.y1, this.y2);
      return Math.abs((this.y2 + this.y1) / 2.0F) - (getLineStrokeWidth() + getHeight()) / 2.0F;
    }

    public double getAngleDegrees() {
      if (this.horizontal || this.vertical)
        return 0.0D;
      return Math.toDegrees(Math.atan(((this.y2 - this.y1) / (this.x2 - this.x1))));
    }

    public float getLineStrokeWidth() {
      float lineWidth = PDFDomTreeOld.this.transformWidth(PDFDomTreeOld.this.getGraphicsState().getLineWidth());
      if (lineWidth < 0.5F)
        lineWidth = 0.5F;
      return lineWidth;
    }

    public String getBorderSide() {
      return this.vertical ? "border-right" : "border-bottom";
    }

    private float distanceFormula(float x1, float y1, float x2, float y2) {
      return (float) Math.sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
    }
  }

  protected String createGlobalStyle() {
    StringBuilder ret = new StringBuilder();
    ret.append(createFontFaces());
    ret.append("\n");
    ret.append(this.defaultStyle);
    return ret.toString();
  }

  protected void updateFontTable() {

    if (!(this.config.getFontHandler() instanceof org.fit.pdfdom.resource.IgnoreResourceHandler))
      super.updateFontTable();
  }

  protected String createFontFaces() {
    StringBuilder ret = new StringBuilder();
    for (FontTable.Entry font : this.fontTable.getEntries())
      createFontFace(ret, font);
    return ret.toString();
  }

  private void createFontFace(StringBuilder ret, FontTable.Entry font) {
    ret.append("@font-face {");
    ret.append("font-family:\"").append(font.usedName).append("\";");
    ret.append("src:url('");
    try {
      String src = this.config.getFontHandler().handleResource(font);
      ret.append(src);
    } catch (IOException e) {
      log.error("Error writing font face data for font: " + font.getName() + "Exception: {} {}", e
          .getMessage(), e.getClass());
    }
    ret.append("');");
    ret.append("}\n");
  }

  public void processDocument(PDDocument document, Writer output) throws IOException {

  }

  public void logTextList() {
    // Iterate through each TextPosition in the textList
    for (TextPosition textPosition : textList) {
      // Log the text and its position (X and Y coordinates)
      System.out.println("Text: " + textPosition.getUnicode() +
          " | X: " + textPosition.getX() +
          " | Y: " + textPosition.getY());
    }
  }

  private void generateHTML(PDDocument document, Writer output) throws IOException {
    String doctitle = document.getDocumentInformation().getTitle();
    if (doctitle != null && !doctitle.trim().isEmpty()) {
      output.write("<!DOCTYPE html>\n<html>\n<head>\n<title>" + escapeHTML(doctitle) + "</title>\n");
    } else {
      output.write("<!DOCTYPE html>\n<html>\n<head>\n<title>PDF Document</title>\n");
    }

    // Add global styles if needed
    output.write("<style>\n/* Add your CSS styles here */\n</style>\n</head>\n<body>\n");

    // Start generating HTML content
    for (TextPosition text : textList) {
      String unicode = text.getUnicode();
      if (unicode != null && !unicode.trim().isEmpty()) {
        // You can enhance this part to include positioning styles
        output.write("<span>" + escapeHTML(unicode) + "</span>");
      }
    }

    output.write("\n</body>\n</html>");
  }

  /**
   * Escapes HTML special characters in a string.
   *
   * @param text The text to escape.
   * @return The escaped text.
   */
  private String escapeHTML(String text) {
    if (text == null)
      return "";
    return text.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
