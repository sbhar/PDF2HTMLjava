/**
 * PDFBoxTree.java
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
 * Created on 27.9.2011, 16:56:55 by burgetr
 */
package org.fit.pdfdom;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceRGBColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceRGBColor;
import org.apache.pdfbox.contentstream.operator.state.SetFlatness;
import org.apache.pdfbox.contentstream.operator.state.SetLineCapStyle;
import org.apache.pdfbox.contentstream.operator.state.SetLineDashPattern;
import org.apache.pdfbox.contentstream.operator.state.SetLineJoinStyle;
import org.apache.pdfbox.contentstream.operator.state.SetLineMiterLimit;
import org.apache.pdfbox.contentstream.operator.state.SetLineWidth;
import org.apache.pdfbox.contentstream.operator.state.SetRenderingIntent;
import org.apache.pdfbox.contentstream.operator.text.SetFontAndSize;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.text.TextPositionComparator;
import org.apache.pdfbox.util.IterativeMergeSort;
import org.apache.pdfbox.util.Matrix;
import org.fit.pdfdom.resource.ImageResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.Element;

import static org.apache.pdfbox.pdmodel.graphics.state.RenderingMode.*;

/**
 * A generic tree of boxes created from a PDF file. It processes the PDF
 * document and calls
 * the appropriate abstract methods in order to render a page, text box, etc.
 * The particular
 * implementations are expected to implement these actions in order to build the
 * resulting
 * document tree.
 *
 * @author burgetr
 */
public abstract class PDFBoxTree extends PDFTextStripper {

  public List<TextPosition> textList; // Declare textList

  public double highestTopValue = 0.0;

  private static Logger log = LoggerFactory.getLogger(PDFBoxTree.class);

  /** Length units used in the generated CSS */
  public static final String UNIT = "pt";

  /** Known font names that are recognized in the PDF files */
  protected static String[] cssFontFamily = { "Times New Roman", "Times", "Garamond", "Helvetica", "Arial Narrow",
      "Arial", "Verdana", "Courier New", "MS Sans Serif" };

  /** Known font subtypes recognized in PDF files */
  protected static String[] pdFontType = { "normal", "roman", "bold", "italic", "bolditalic" };
  /**
   * Font weights corresponding to the font subtypes in
   * {@link PDFDomTree#pdFontType}
   */
  protected static String[] cssFontWeight = { "normal", "normal", "bold", "normal", "bold" };
  /**
   * Font styles corresponding to the font subtypes in
   * {@link PDFDomTree#pdFontType}
   */
  protected static String[] cssFontStyle = { "normal", "normal", "normal", "italic", "italic" };

  /**
   * When set to <code>true</code>, the graphics in the PDF file will be ignored.
   */
  protected boolean disableGraphics = false;
  /** When set to <code>true</code>, the embedded images will be ignored. */
  protected boolean disableImages = false;
  /**
   * When set to <code>true</code>, the image data will not be transferred to the
   * HTML data: url.
   */
  protected boolean disableImageData = false;
  /** First page to be processed */
  protected int startPage;
  /** Last page to be processed */
  protected int endPage;

  /** Table of embedded fonts */
  protected FontTable fontTable;

  /** The PDF page currently being processed */
  protected PDPage pdpage;

  /**
   * Current text coordinates (the coordinates of the last encountered text box).
   */
  protected float cur_x;
  /**
   * Current text coordinates (the coordinates of the last encountered text box).
   */
  protected float cur_y;

  /** Current path construction position */
  protected float path_x;
  /** Current path construction position */
  protected float path_y;
  /** Starting path construction position */
  protected float path_start_x;
  /** Starting path construction position */
  protected float path_start_y;

  /** Previous positioned text. */
  protected TextPosition lastText = null;

  /** Last diacritic if any */
  protected TextPosition lastDia = null;

  /** The text box currently being created. */
  protected StringBuilder textLine;

  /** Current text line metrics */
  protected TextMetrics textMetrics;

  /** Current graphics path */
  protected Vector<PathSegment> graphicsPath;

  /** The style of the future box being modified by the operators */
  protected BoxStyle style;

  /** The style of the text line being created */
  protected BoxStyle curstyle;

  public PDFBoxTree() throws IOException {
    super();
    textList = new ArrayList<>(); // Initialize textList
    super.setSortByPosition(true);
    super.setSuppressDuplicateOverlappingText(true);

    // add operators for tracking the graphic state
    addOperator(new SetStrokingColorSpace());
    addOperator(new SetNonStrokingColorSpace());
    addOperator(new SetLineDashPattern());
    addOperator(new SetStrokingDeviceGrayColor());
    addOperator(new SetNonStrokingDeviceGrayColor());
    addOperator(new SetFlatness());
    addOperator(new SetLineJoinStyle());
    addOperator(new SetLineCapStyle());
    addOperator(new SetStrokingDeviceCMYKColor());
    addOperator(new SetNonStrokingDeviceCMYKColor());
    addOperator(new SetLineMiterLimit());
    addOperator(new SetStrokingDeviceRGBColor());
    addOperator(new SetNonStrokingDeviceRGBColor());
    addOperator(new SetRenderingIntent());
    addOperator(new SetStrokingColor());
    addOperator(new SetNonStrokingColor());
    addOperator(new SetStrokingColorN());
    addOperator(new SetNonStrokingColorN());
    addOperator(new SetFontAndSize());
    addOperator(new SetLineWidth());

    init();
  }

  /**
   * Internal initialization.
   */
  private void init() {
    style = new BoxStyle(UNIT);
    textLine = new StringBuilder();
    textMetrics = null;
    graphicsPath = new Vector<PathSegment>();
    startPage = 0;
    endPage = Integer.MAX_VALUE;
    fontTable = new FontTable();
  }

  public void processPage(PDPage page) throws IOException {
    if (getCurrentPageNo() >= startPage && getCurrentPageNo() <= endPage) {
      pdpage = page;
      updateFontTable();
      startNewPage();
      super.processPage(page);
    }
  }

  /**
   * Checks whether the graphics processing is disabled.
   * 
   * @return <code>true</code> when the graphics processing is disabled in the
   *         parser configuration.
   */
  public boolean getDisableGraphics() {
    return disableGraphics;
  }

  /**
   * Disables the processing of the graphic operators in the PDF files.
   * 
   * @param disableGraphics when set to <code>true</code> the graphics is ignored
   *                        in the source file.
   */
  public void setDisableGraphics(boolean disableGraphics) {
    this.disableGraphics = disableGraphics;
  }

  /**
   * Checks whether processing of embedded images is disabled.
   * 
   * @return <code>true</code> when the processing of embedded images is disabled
   *         in the parser configuration.
   */
  public boolean getDisableImages() {
    return disableImages;
  }

  /**
   * Disables the processing of images contained in the PDF files.
   * 
   * @param disableImages when set to <code>true</code> the images are ignored in
   *                      the source file.
   */
  public void setDisableImages(boolean disableImages) {
    this.disableImages = disableImages;
  }

  /**
   * Checks whether the copying of image data is disabled.
   * 
   * @return <code>true</code> when the copying of image data is disabled in the
   *         parser configuration.
   */
  public boolean getDisableImageData() {
    return disableImageData;
  }

  /**
   * Disables the copying the image data to the resulting DOM tree.
   * 
   * @param disableImageData when set to <code>true</code> the image data is not
   *                         copied to the document tree.
   *                         The eventual <code>img</code> elements will have an
   *                         empty <code>src</code> attribute.
   */
  public void setDisableImageData(boolean disableImageData) {
    this.disableImageData = disableImageData;
  }

  @Override
  public int getStartPage() {
    return startPage;
  }

  @Override
  public void setStartPage(int startPage) {
    this.startPage = startPage;
  }

  @Override
  public int getEndPage() {
    return endPage;
  }

  @Override
  public void setEndPage(int endPage) {
    this.endPage = endPage;
  }

  // private Element imageElements;

  // public void setImageElements(Element imageElement) {
  // this.imageElements = imageElement;
  // }

  // public Element getImageElements() {
  // return this.imageElements;
  // }

  // // In PDFBoxTree class
  // private String imgSrc;

  // public void setImgSrc(String imgSrc) {
  //   //System.out.println("in pdfboxtree setImgSrc: " + imgSrc);
  //   this.imgSrc = imgSrc;
  // }

  // public String getImgSrc() {
  //   return this.imgSrc;
  // }

  // // Abstract method(s) to be implemented by subclasses
  // public abstract void processTree();

  // ===========================================================================================

  /**
   * Adds a new page to the resulting document and makes it a current (active)
   * page.
   */
  protected abstract void startNewPage();

  /**
   * Creates a new text box in the current page. The style and position of the
   * text are contained
   * in the {@link PDFBoxTree#curstyle} property.
   * 
   * @param data The text contents.
   */
  protected abstract void renderText(String data, TextMetrics metrics);

  /**
   * Adds a rectangle to the current page on the specified position.
   * 
   * @param rect   the rectangle to be rendered
   * @param stroke should there be a stroke around?
   * @param fill   should the rectangle be filled?
   */
  protected abstract void renderPath(List<PathSegment> path, boolean stroke, boolean fill) throws IOException;

  /**
   * Adds an image to the current page.
   * 
   * @param type   the image type: <code>"png"</code> or <code>"jpeg"</code>
   * @param x      the X coordinate of the image
   * @param y      the Y coordinate of the image
   * @param width  the width coordinate of the image
   * @param height the height coordinate of the image
   * @param data   the image data depending on the specified type
   * @return
   */
  protected abstract void renderImage(float x, float y, float width, float height, ImageResource data)
      throws IOException;

  protected float[] toRectangle(List<PathSegment> path) {
    if (path.size() == 4) {
      Set<Float> xc = new HashSet<Float>();
      Set<Float> yc = new HashSet<Float>();
      // find x/y 1/2
      for (PathSegment line : path) {
        xc.add(line.getX1());
        xc.add(line.getX2());
        yc.add(line.getY1());
        yc.add(line.getY2());
      }
      if (xc.size() == 2 && yc.size() == 2) {
        return new float[] { Collections.min(xc), Collections.min(yc), Collections.max(xc),
            Collections.max(yc) };
      } else
        return null; // two different X and Y coordinates required
    } else
      return null; // four segments required
  }

  /**
   * Updates the font table by adding new fonts used at the current page.
   */
  protected void updateFontTable() {
    PDResources resources = pdpage.getResources();
    if (resources != null) {
      try {
        processFontResources(resources, fontTable);
      } catch (IOException e) {
        log.error("Error processing font resources: "
            + "Exception: {} {}", e.getMessage(), e.getClass());
      }
    }
  }

  private void processFontResources(PDResources resources, FontTable table) throws IOException {
    String fontNotSupportedMessage = "Font: {} skipped because type '{}' is not supported.";

    for (COSName key : resources.getFontNames()) {
      PDFont font = resources.getFont(key);
      if (font instanceof PDTrueTypeFont) {
        table.addEntry(font);
        log.debug("Font: " + font.getName() + " TTF");
      } else if (font instanceof PDType0Font) {
        PDCIDFont descendantFont = ((PDType0Font) font).getDescendantFont();
        if (descendantFont instanceof PDCIDFontType2)
          table.addEntry(font);
        else
          log.warn(fontNotSupportedMessage, font.getName(), font.getClass().getSimpleName());
      } else if (font instanceof PDType1CFont)
        table.addEntry(font);
      else
        log.warn(fontNotSupportedMessage, font.getName(), font.getClass().getSimpleName());
    }

    for (COSName name : resources.getXObjectNames()) {
      PDXObject xobject = resources.getXObject(name);
      if (xobject instanceof PDFormXObject) {
        PDFormXObject xObjectForm = (PDFormXObject) xobject;
        PDResources formResources = xObjectForm.getResources();
        if (formResources != null && formResources != resources
            && formResources.getCOSObject() != resources.getCOSObject())
          processFontResources(formResources, table);
      }
    }

  }

  // ===========================================================================================

  @Override
  protected void processOperator(Operator operator, List<COSBase> arguments)
      throws IOException {
    String operation = operator.getName();
    /*
     * System.out.println("Operator: " + operation + ":" + arguments.size());
     * if (operation.equals("sc") || operation.equals("cs"))
     * {
     * System.out.print("  ");
     * for (int i = 0; i < arguments.size(); i++)
     * System.out.print(arguments.get(i) + " ");
     * System.out.println();
     * }
     */

    // word spacing
    if (operation.equals("Tw")) {
      style.setWordSpacing(getLength(arguments.get(0)));
    }

    // letter spacing
    else if (operation.equals("Tc")) {
      style.setLetterSpacing(getLength(arguments.get(0)));
    }

    // graphics
    else if (operation.equals("m")) // move
    {
      if (!disableGraphics) {
        if (arguments.size() == 2) {
          float[] pos = transformPosition(getLength(arguments.get(0)), getLength(arguments.get(1)));
          path_x = pos[0];
          path_y = pos[1];
          path_start_x = pos[0];
          path_start_y = pos[1];
        }
      }
    } else if (operation.equals("l")) // line
    {
      if (!disableGraphics) {
        if (arguments.size() == 2) {
          float[] pos = transformPosition(getLength(arguments.get(0)), getLength(arguments.get(1)));
          graphicsPath.add(new PathSegment(path_x, path_y, pos[0], pos[1]));
          path_x = pos[0];
          path_y = pos[1];
        }
      }
    } else if (operation.equals("h")) // end subpath
    {
      if (!disableGraphics) {
        graphicsPath.add(new PathSegment(path_x, path_y, path_start_x, path_start_y));
      }
    }

    // rectangle
    else if (operation.equals("re")) {
      if (!disableGraphics) {
        if (arguments.size() == 4) {
          float x = getLength(arguments.get(0));
          float y = getLength(arguments.get(1));
          float width = getLength(arguments.get(2));
          float height = getLength(arguments.get(3));

          float[] p1 = transformPosition(x, y);
          float[] p2 = transformPosition(x + width, y + height);

          graphicsPath.add(new PathSegment(p1[0], p1[1], p2[0], p1[1]));
          graphicsPath.add(new PathSegment(p2[0], p1[1], p2[0], p2[1]));
          graphicsPath.add(new PathSegment(p2[0], p2[1], p1[0], p2[1]));
          graphicsPath.add(new PathSegment(p1[0], p2[1], p1[0], p1[1]));
        }
      }
    }

    // fill
    else if (operation.equals("f") || operation.equals("F") || operation.equals("f*")) {
      renderPath(graphicsPath, false, true);
      graphicsPath.removeAllElements();
    }

    // stroke
    else if (operation.equals("S")) {
      renderPath(graphicsPath, true, false);
      graphicsPath.removeAllElements();
    } else if (operation.equals("s")) {
      graphicsPath.add(new PathSegment(path_x, path_y, path_start_x, path_start_y));
      renderPath(graphicsPath, true, false);
      graphicsPath.removeAllElements();
    }

    // stroke and fill
    else if (operation.equals("B") || operation.equals("B*")) {
      renderPath(graphicsPath, true, true);
      graphicsPath.removeAllElements();
    } else if (operation.equals("b") || operation.equals("b*")) {
      graphicsPath.add(new PathSegment(path_x, path_y, path_start_x, path_start_y));
      renderPath(graphicsPath, true, true);
      graphicsPath.removeAllElements();
    }

    // cancel path
    else if (operation.equals("n")) {
      graphicsPath.removeAllElements();
    }

    // invoke named object - images
    else if (operation.equals("Do")) {
      if (!disableImages)
        processImageOperation(arguments);
    }

    super.processOperator(operator, arguments);
  }

  protected void processImageOperation(List<COSBase> arguments) throws IOException {
    COSName objectName = (COSName) arguments.get(0);
    PDXObject xobject = getResources().getXObject(objectName);
    if (xobject instanceof PDImageXObject) {
      PDImageXObject pdfImage = (PDImageXObject) xobject;
      BufferedImage outputImage = pdfImage.getImage();
      outputImage = rotateImage(outputImage);

      ImageResource imageData = new ImageResource(getTitle(), outputImage);

      Rectangle2D bounds = calculateImagePosition(pdfImage);
      float x = (float) bounds.getX();
      float y = (float) bounds.getY();

      renderImage(x, y, (float) bounds.getWidth(), (float) bounds.getHeight(), imageData);
    }
  }

  private BufferedImage rotateImage(BufferedImage outputImage) {
    // x, y and size are handled by css attributes but still need to rotate the
    // image so pulling
    // only rotation out of the matrix so no giant whitespace offset from
    // translations
    Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();

    AffineTransform tr = ctm.createAffineTransform();
    double rotate = Math.atan2(tr.getShearY(), tr.getScaleY()) - Math.toRadians(pdpage.getRotation());
    outputImage = ImageUtils.rotateImage(outputImage, rotate);

    return outputImage;
  }

  private Rectangle2D calculateImagePosition(PDImageXObject pdfImage) throws IOException {
    Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
    Rectangle2D imageBounds = pdfImage.getImage().getRaster().getBounds();

    AffineTransform imageTransform = new AffineTransform(ctm.createAffineTransform());
    imageTransform.scale(1.0 / pdfImage.getWidth(), -1.0 / pdfImage.getHeight());
    imageTransform.translate(0, -pdfImage.getHeight());

    AffineTransform pageTransform = createCurrentPageTransformation();
    pageTransform.concatenate(imageTransform);

    return pageTransform.createTransformedShape(imageBounds).getBounds2D();
  }

  // @Override
  // protected void processTextPosition(TextPosition text) {
  // // Print each individual character (text) for debugging or logging purposes
  // // System.out.println("text" + text.getUnicode()); // This will print each
  // // individual character

  // textList.add(text);

  // TextPositionComparator comparator = new TextPositionComparator();
  // try {
  // Collections.sort(textList, comparator);
  // } catch (IllegalArgumentException e) {
  // // Handle non-transitive comparator by implementing a custom sort if
  // necessary
  // // For simplicity, we'll skip it here
  // System.err.println("Error during sorting: " + e.getMessage());
  // }
  // String myText = "";
  // for (TextPosition textPosition : textList) {
  // // Log the text and its position (X and Y coordinates)
  // // System.out.println("Text: " + textPosition.getUnicode() +
  // // " | X: " + textPosition.getX() +
  // // " | Y: " + textPosition.getY());
  // text = textPosition;
  // }

  // if (text.isDiacritic()) {
  // lastDia = text;
  // } else if (!text.getUnicode().trim().isEmpty()) {
  // // Merge diacritic if necessary
  // if (lastDia != null) {
  // if (text.contains(lastDia)) {
  // text.mergeDiacritic(lastDia);
  // }
  // lastDia = null;
  // }

  // cur_x = text.getX();
  // cur_y = text.getY();

  // // Calculate distance from last text position
  // float distx = 0;
  // float disty = 0;
  // if (lastText != null) {
  // distx = text.getX() - (lastText.getX() + lastText.getWidth());
  // disty = text.getY() - lastText.getY();
  // }

  // // Check if we should split the boxes (new line/box logic)
  // boolean split = lastText == null || distx > 1.0f || distx < -6.0f ||
  // Math.abs(disty) > 1.0f
  // || isReversed(getTextDirectionality(text)) !=
  // isReversed(getTextDirectionality(lastText));

  // // Check if the style has changed, and if so, split the boxes
  // updateStyle(style, text);
  // if (!style.equals(curstyle)) {
  // split = true;
  // }

  // if (split) { // Start of a new box
  // // Finish current box (if any)
  // if (lastText != null) {
  // finishBox();
  // }
  // // Start a new box
  // curstyle = new BoxStyle(style);
  // }

  // // Append the current text to the textLine (for the complete sequence)
  // textLine.append(text);
  // // System.out.println(textLine.toString() + "\n");

  // // Accumulate metrics for the current text
  // if (textMetrics == null) {
  // textMetrics = new TextMetrics(text);
  // } else {
  // // System.out.println(text);
  // textMetrics.append(text);
  // // System.out.println(textMetrics.getTop() + "\n");
  // }

  // // Set the current text as the last processed text
  // lastText = text;
  // }
  // }
  @Override
  protected void processTextPosition(TextPosition text) {
    // System.out.println(text);
    textList.add(text);
    if (text.isDiacritic()) {
      this.lastDia = text;
    } else if (!text.getUnicode().trim().isEmpty()) {
      if (this.lastDia != null) {
        if (text.contains(this.lastDia))
          text.mergeDiacritic(this.lastDia);
        this.lastDia = null;
      }
      this.cur_x = text.getX();
      this.cur_y = text.getY();
      float distx = 0.0F;
      float disty = 0.0F;
      if (this.lastText != null) {
        distx = text.getX() - this.lastText.getX() + this.lastText.getWidth();
        disty = text.getY() - this.lastText.getY();
      }
      boolean split = (this.lastText == null || distx > 1.0F || distx < -6.0F || Math.abs(disty) > 1.0F
          || isReversed(getTextDirectionality(text)) != isReversed(getTextDirectionality(this.lastText)));
      updateStyle(this.style, text);
      if (!this.style.equals(this.curstyle))
        split = true;
      if (split) {

        // System.out.println(text);
        // this.textLine.append(text.getUnicode());
        finishBox();
      }
      this.curstyle = new BoxStyle(this.style);
    }
    this.textLine.append(text.getUnicode());
    if (this.textMetrics == null) {
      this.textMetrics = new TextMetrics(text);
    } else {
      this.textMetrics.append(text);
    }
    this.lastText = text;
  }

  // Custom method to sort the textList
  public void sortTextList2() throws IOException {
    if (textList != null && !textList.isEmpty()) {
      TextPositionComparator comparator = new TextPositionComparator();
      try {
        // Collections.sort(textList, comparator);
      } catch (IllegalArgumentException e) {
        // Handle non-transitive comparator by implementing a custom sort if necessary
        // For simplicity, we'll skip it here
        System.err.println("Error during sorting: " + e.getMessage());
      }
    }
  }

  public List<TextPosition> getTextList() {
    return textList;
  }

  // public String endDocument2() throws IOException {
  // // Sort textList here, only once after all TextPosition objects are added
  // String textx = "sugata";
  // if (textList != null && !textList.isEmpty()) {
  // TextPositionComparator comparator = new TextPositionComparator();
  // try {
  // Collections.sort(textList, comparator);
  // } catch (IllegalArgumentException e) {
  // IterativeMergeSort.sort(textList, comparator);
  // }

  // // Generate HTML output based on the sorted textList
  // StringBuilder htmlOutput = new StringBuilder();
  // TextPosition lastText = null;

  // for (TextPosition text : textList) {
  // if (text.isDiacritic()) {
  // lastDia = text;
  // } else if (!text.getUnicode().trim().isEmpty()) {
  // if (lastDia != null && text.contains(lastDia)) {
  // text.mergeDiacritic(lastDia);
  // lastDia = null;
  // }

  // cur_x = text.getX();
  // cur_y = text.getY();

  // float distx = lastText != null ? text.getX() - (lastText.getX() +
  // lastText.getWidth()) : 0;
  // float disty = lastText != null ? text.getY() - lastText.getY() : 0;

  // boolean split = lastText == null || distx > 1.0f || distx < -6.0f ||
  // Math.abs(disty) > 1.0f
  // || isReversed(getTextDirectionality(text)) !=
  // isReversed(getTextDirectionality(lastText));

  // updateStyle(style, text);
  // if (!style.equals(curstyle)) {
  // split = true;
  // }

  // if (split) {
  // if (lastText != null) {
  // finishBox();
  // }
  // curstyle = new BoxStyle(style);
  // }

  // textLine.append(text.getUnicode());
  // if (textMetrics == null) {
  // textMetrics = new TextMetrics(text);
  // } else {
  // textMetrics.append(text);
  // }

  // lastText = text;
  // }
  // }

  // finishBox(); // Finalize the last box

  // // Output final HTML
  // //System.out.println(htmlOutput.toString());
  // //String textx = "sugata";
  // textx = htmlOutput.toString();
  // return textx;
  // } else {
  // System.out.println("textList is empty or null; no output generated.");
  // }
  // return textx;
  // }

  // // Comparator to sort TextPosition objects
  // public class TextPositionComparator implements Comparator<TextPosition> {
  // @Override
  // public int compare(TextPosition pos1, TextPosition pos2) {
  // // Compare based on Y position first (top to bottom)
  // int yCompare = Float.compare(pos1.getYDirAdj(), pos2.getYDirAdj());
  // if (yCompare != 0) {
  // return yCompare;
  // }
  // // If Y positions are similar, compare based on X position (left to right)
  // return Float.compare(pos1.getXDirAdj(), pos2.getXDirAdj());
  // }
  // }

  /**
   * Finishes the current box - empties the text line buffer and creates a DOM
   * element from it.
   */

  // Declare the array as an instance variable if you need it to persist across
  // calls

  public List<Object[]> array = new ArrayList<>();

  // public List<Object[]> getSortedText() {
  // // Determine directionality and set `s` appropriately
  // String s;
  // if (isReversed(Character.getDirectionality(textLine.charAt(0)))) {
  // s = textLine.reverse().toString();
  // } else {
  // s = textLine.toString();
  // }

  // // Add entry to array only if it doesn't already exist
  // Object[] newItem = new Object[] { s, textMetrics.getTop() };
  // boolean exists = array.stream().anyMatch(item -> item[0].equals(newItem[0])
  // && item[1].equals(newItem[1]));

  // if (!exists) {
  // array.add(newItem);
  // }

  // // Sort the array based on the top value of textMetrics
  // array.sort(Comparator.comparingDouble(o -> ((Float) o[1]).doubleValue()));

  // // Remove duplicates
  // List<Object[]> uniqueArray = new ArrayList<>();
  // Object[] previousItem = null;
  // for (Object[] item : array) {
  // if (previousItem == null || !item[0].equals(previousItem[0]) ||
  // !item[1].equals(previousItem[1])) {
  // uniqueArray.add(item); // Add item if it’s not a duplicate
  // }
  // previousItem = item; // Update previousItem for next comparison
  // }

  // array = uniqueArray; // Replace original array with the deduplicated list

  // // Build the concatenated text with only the last chunk
  // StringBuilder concatenatedText = new StringBuilder();
  // if (!array.isEmpty()) {
  // String lastText = (String) array.get(array.size() - 1)[0]; // Get only the
  // last item
  // concatenatedText.append(lastText);
  // }

  // // Render the concatenated text containing only the last chunk
  // // renderText(concatenatedText.toString().trim(), textMetrics); // Trim to
  // // remove any trailing spaces
  // return array;
  // }

  // Helper function to find the index of the element with the lowest `top` value
  public int findLowestTopIndex() {
    if (array.isEmpty()) {
      // System.out.println("Array is empty");
      return 0; // Handle empty array case
    }

    float minTop = Float.MAX_VALUE;
    int minIndex = 0;

    // Debugging: Print out all `top` values
    // System.out.println("All top values in array:");
    for (int i = 0; i < array.size(); i++) {
      float topValue = (Float) array.get(i)[1];
      // System.out.println("Index " + i + ", Top: " + topValue);

      // Find the lowest `top` value
      if (topValue < minTop) {
        minTop = topValue;
        minIndex = i;
      }
    }
    // System.out.println(findHighestTopIndex());
    // Debugging: Print the lowest `top` and its index
    // System.out.println("Lowest top value: " + minTop + " at index " + minIndex);
    return minIndex;
  }

  // Helper function to find the index of the element with the highest `top` value
  public int findHighestTopIndex() {
    if (array.isEmpty()) {
      System.out.println("Array is empty");
      return 0; // Handle empty array case
    }

    float maxTop = Float.MIN_VALUE;
    highestTopValue = maxTop;
    int maxIndex = 0;

    // Debugging: Print out all `top` values
    // System.out.println("All top values in array:");
    for (int i = 0; i < array.size(); i++) {
      float topValue = (Float) array.get(i)[1];
      // System.out.println("Index " + i + ", Top: " + topValue);

      // Find the highest `top` value
      if (topValue > maxTop) {
        maxTop = topValue;
        maxIndex = i;
      }
    }

    // Debugging: Print the highest `top` and its index
    // System.out.println("Highest top value: " + maxTop + " at index " + maxIndex);
    return maxIndex;
  }

  protected void finishBox() {
    // Matrix translateMatrix=new Matrix();
    // TextPosition text = new TextPosition(0, 0, 0, translateMatrix, 0, 0, 0, 0, 0,
    // null, null, null, cur_x, endPage);
    // TextPosition t2 = null;
    TextPositionComparator comparator = new TextPositionComparator();
    Collections.sort(textList, comparator);
    for (int i = 0; i < textList.size(); i++) {
      TextPosition t2 = textList.get(i);
      // System.out.println(t2);
    }
    // this.textLine.append(t2.getUnicode());
    if (textLine.length() > 0) {
      String s;
      if (isReversed(Character.getDirectionality(textLine.charAt(0)))) {
        s = textLine.reverse().toString();
      } else {
        // System.out.println(textLine.toString() + "\n");
        s = textLine.toString();
      }

      // Update curstyle properties
      curstyle.setLeft(textMetrics.getX());
      curstyle.setTop(textMetrics.getTop());
      curstyle.setLineHeight(textMetrics.getHeight());

      // List<Object[]> sortedText = getSortedText();
      TextPosition myText;
      // double myTop = 0.0;
      // for (Object[] row : sortedText) {
      // String text = (String) row[0]; // Get the text from the array
      // Float top = (Float) row[1]; // Get the textMetrics top value

      // myText = text;
      // myTop = top;
      // }
      // System.out.println(myText);

      // renderText(((((textMetrics.getTop()==myTop)&&(myText==s)))?"":myText),
      // textMetrics);

      // for (TextPosition textPosition : textList) {
      // // Log the text and its position (X and Y coordinates)
      // // System.out.println("Text: " + textPosition.getUnicode() +
      // // " | X: " + textPosition.getX() +
      // // " | Y: " + textPosition.getY());
      // myText = textPosition;
      // }

      // String s2 = "S";

      // s2 = myText.getUnicode();
      renderText(s, textMetrics);

      textLine = new StringBuilder();
      textMetrics = null;
    }
  }

  /**
   * Checks whether the text directionality corresponds to reversed text (very
   * rough)
   * 
   * @param directionality the Character.directionality
   * @return
   */
  protected boolean isReversed(byte directionality) {
    switch (directionality) {
      case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
      case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
      case Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING:
      case Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE:
        return true;

      default:
        return false;
    }
  }

  /**
   * Updates the text style according to a new text position
   * 
   * @param bstyle the style to be updated
   * @param text   the text position
   */
  protected void updateStyle(BoxStyle bstyle, TextPosition text) {
    // System.out.println("======= Checkink =======");
    String font = text.getFont().getName();
    String family = null;
    String weight = null;
    String fstyle = null;

    bstyle.setFontSize(text.getXScale()); // this seems to give better results than getFontSizeInPt()
    bstyle.setLineHeight(text.getHeight());

    if (font != null) {
      // font style and weight
      for (int i = 0; i < pdFontType.length; i++) {
        if (font.toLowerCase().lastIndexOf(pdFontType[i]) >= 0) {
          weight = cssFontWeight[i];
          fstyle = cssFontStyle[i];
          break;
        }
      }
      if (weight != null)
        bstyle.setFontWeight(weight);
      else
        bstyle.setFontWeight(cssFontWeight[0]);
      if (fstyle != null)
        bstyle.setFontStyle(fstyle);
      else
        bstyle.setFontStyle(cssFontStyle[0]);

      // font family
      // If it's a known common font don't embed in html output to save space
      String knownFontFamily = findKnownFontFamily(font);
      if (!knownFontFamily.equals(""))
        family = knownFontFamily;
      else {
        family = fontTable.getUsedName(text.getFont());
        if (family == null)
          family = font;
      }

      if (family != null)
        bstyle.setFontFamily(family);
    }

    updateStyleForRenderingMode();
  }

  private String findKnownFontFamily(String font) {
    for (String fontFamilyOn : cssFontFamily) {
      if (font.toLowerCase().lastIndexOf(fontFamilyOn.toLowerCase().replaceAll("\\s+", "")) >= 0)
        return fontFamilyOn;
    }

    return "";
  }

  private void updateStyleForRenderingMode() {
    String fillColor = colorString(getGraphicsState().getNonStrokingColor());
    String strokeColor = colorString(getGraphicsState().getStrokingColor());

    if (isTextFillEnabled())
      style.setColor(fillColor);
    else
      style.setColor(BoxStyle.transparentColor);
    if (isTextStrokeEnabled())
      style.setStrokeColor(strokeColor);
    else
      style.setStrokeColor(BoxStyle.transparentColor);
  }

  private boolean isTextStrokeEnabled() {
    RenderingMode mode = getGraphicsState().getTextState().getRenderingMode();
    return mode == STROKE || mode == STROKE_CLIP || mode == FILL_STROKE || mode == FILL_STROKE_CLIP;
  }

  private boolean isTextFillEnabled() {
    RenderingMode mode = getGraphicsState().getTextState().getRenderingMode();
    return mode == FILL || mode == FILL_CLIP || mode == FILL_STROKE || mode == FILL_STROKE_CLIP;
  }

  /**
   * Obtains the media box valid for the current page.
   * 
   * @return the media box rectangle
   */
  protected PDRectangle getCurrentMediaBox() {
    PDRectangle layout = pdpage.getCropBox();
    return layout;
  }

  // ===========================================================================================

  /**
   * Transforms a length according to the current transformation matrix.
   */
  protected float transformLength(float w) {
    Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
    Matrix m = new Matrix();
    m.setValue(2, 0, w);
    return m.multiply(ctm).getTranslateX();
  }

  /**
   * Transforms a position according to the current transformation matrix and
   * current page transformation.
   * 
   * @param x
   * @param y
   * @return
   */
  protected float[] transformPosition(float x, float y) {
    Point2D.Float point = super.transformedPoint(x, y);
    AffineTransform pageTransform = createCurrentPageTransformation();
    Point2D.Float transformedPoint = (Point2D.Float) pageTransform.transform(point, null);

    return new float[] { (float) transformedPoint.getX(), (float) transformedPoint.getY() };
  }

  protected AffineTransform createCurrentPageTransformation() {
    PDRectangle cb = pdpage.getCropBox();
    AffineTransform pageTransform = new AffineTransform();

    switch (pdpage.getRotation()) {
      case 90:
        pageTransform.translate(cb.getHeight(), 0);
        break;
      case 180:
        pageTransform.translate(cb.getWidth(), cb.getHeight());
        break;
      case 270:
        pageTransform.translate(0, cb.getWidth());
        break;
    }

    pageTransform.rotate(Math.toRadians(pdpage.getRotation()));
    pageTransform.translate(0, cb.getHeight());
    pageTransform.scale(1, -1);
    pageTransform.translate(-cb.getLowerLeftX(), -cb.getLowerLeftY());

    return pageTransform;
  }

  /**
   * Obtains a number from a PDF number value
   * 
   * @param value the PDF value of the Integer or Fload type
   * @return the corresponging numeric value
   */
  protected int intValue(COSBase value) {
    if (value instanceof COSNumber)
      return ((COSNumber) value).intValue();
    else
      return 0;
  }

  /**
   * Obtains a number from a PDF number value
   * 
   * @param value the PDF value of the Integer or Float type
   * @return the corresponging numeric value
   */
  protected float floatValue(COSBase value) {
    if (value instanceof COSNumber)
      return ((COSNumber) value).floatValue();
    else
      return 0;
  }

  /**
   * Obtains a length in points from a PDF number value
   * 
   * @param value the PDF value of the Integer or Fload type
   * @return the resulting length in points
   */
  protected float getLength(COSBase value) {
    return floatValue(value); // no conversion is done right now, we count in PDF units
  }

  /**
   * Obtains a string from a PDF value
   * 
   * @param value the PDF value of the String, Integer or Float type
   * @return the corresponging string value
   */
  protected String stringValue(COSBase value) {
    if (value instanceof COSString)
      return ((COSString) value).getString();
    else if (value instanceof COSNumber)
      return String.valueOf(((COSNumber) value).floatValue());
    else
      return "";
  }

  /**
   * Creates a CSS rgb() specification from the color component values.
   * 
   * @param ir red value (0..255)
   * @param ig green value (0..255)
   * @param ib blue value (0..255)
   * @return the rgb() string
   */
  protected String colorString(int ir, int ig, int ib) {
    return String.format("#%02x%02x%02x", ir, ig, ib);
  }

  /**
   * Creates a CSS rgb() specification from the color component values.
   * 
   * @param r red value (0..1)
   * @param g green value (0..1)
   * @param b blue value (0..1)
   * @return the rgb() string
   */
  protected String colorString(float r, float g, float b) {
    return colorString((int) (r * 255), (int) (g * 255), (int) (b * 255));
  }

  /**
   * Creates a CSS rgb specification from a PDF color
   * 
   * @param pdcolor
   * @return the rgb() string
   */
  protected String colorString(PDColor pdcolor) {
    String color = null;
    try {
      float[] rgb = pdcolor.getColorSpace().toRGB(pdcolor.getComponents());
      color = colorString(rgb[0], rgb[1], rgb[2]);
    } catch (IOException e) {
      log.error("colorString: IOException: {}", e.getMessage());
    } catch (UnsupportedOperationException e) {
      log.error("colorString: UnsupportedOperationException: {}", e.getMessage());
    }
    return color;
  }

  protected String getTitle() {
    String title = document.getDocumentInformation().getTitle();
    if (title == null || title.isEmpty())
      title = "PDF Document";

    // finishBox();
    return title;
  }

  protected byte getTextDirectionality(TextPosition text) {
    return getTextDirectionality(text.getUnicode());
  }

  protected byte getTextDirectionality(String s) {
    if (s.length() > 0)
      return Character.getDirectionality(s.charAt(0));
    else
      return Character.DIRECTIONALITY_UNDEFINED;
  }

  // Helper function to get the trimmed array with the range based on lowest and
  // highest `top`
  public List<Object[]> getTrimmedArray() {
    if (array.isEmpty()) {
      // System.out.println("Array is empty");
      return new ArrayList<>();
    }

    // Variables to track the lowest and highest `top` values and their respective
    // indices
    float minTop = Float.MAX_VALUE;
    float maxTop = Float.MIN_VALUE;
    int minIndex = 0;
    int maxIndex = 0;

    // Debugging: Print all `top` values
    // System.out.println("All top values in array:");
    for (int i = 0; i < array.size(); i++) {
      float topValue = (Float) array.get(i)[1];
      // System.out.println("Index " + i + ", Top: " + topValue);

      // Find the lowest `top` value
      if (topValue < minTop) {
        minTop = topValue;
        minIndex = i;
      }

      // Find the highest `top` value
      if (topValue > maxTop) {
        maxTop = topValue;
        maxIndex = i;
      }
    }

    // Ensure endIndex is after startIndex
    if (maxIndex < minIndex) {
      maxIndex = minIndex;
    }

    // Debugging: Print the lowest and highest `top` and their indices
    // System.out.println("Lowest top value: " + minTop + " at index " + minIndex);
    // System.out.println("Highest top value: " + maxTop + " at index " + maxIndex);

    // Return the trimmed array from `minIndex` to `maxIndex`
    return array.subList(minIndex, maxIndex + 1);
  }

}
