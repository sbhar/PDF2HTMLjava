// File: MyConcretePDFBoxTree.java
package org.fit.pdfdom;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.geom.AffineTransform;

import org.fit.pdfdom.*;

import org.fit.pdfdom.resource.ImageResource;
import java.util.List;

public class MyConcretePDFBoxTree extends ConcretePDFBoxTree {

    private PDPage pdpage;
    private String htmlOutput;

    // Constructor that calls the parent class constructor
    public MyConcretePDFBoxTree(String outputDir) throws IOException {
        super(outputDir); // Call the parent constructor with the required argument
        // super.endDocument(document);
        //System.out.println(toRender(testHtmlOutput));
        htmlOutput = testHtmlOutput;
        PDFDomTreeConfig config = PDFDomTreeConfig.createDefaultConfig();
       // config.setTestHtmlOutput(toRender(testHtmlOutput));
    }

    public MyConcretePDFBoxTree(PDFDomTreeConfig config) throws IOException {
        super(config);
        //config.setTestHtmlOutput(htmlOutput);
        System.out.println("Value of testHtmlOutput at MyConcrete2: " + config.getHtmlOutput());
    }

    @Override
    protected void renderPath(List<PathSegment> path, boolean stroke, boolean fill) throws IOException {
        // Implement the renderImage method here.
        // Example implementation:
        //System.out.println("Rendering image at (" + x + ", " + y + ") with size (" + width + "x" + height + ")");
        // You would actually do the image rendering logic here,
        // such as appending to a page or whatever your project needs.
        // System.out.println("Title: " + getOutputDir());
    }
    @Override
    protected void startNewPage() {
        // Implement the renderImage method here.
        // Example implementation:
        //System.out.println("Rendering image at (" + x + ", " + y + ") with size (" + width + "x" + height + ")");
        // You would actually do the image rendering logic here,
        // such as appending to a page or whatever your project needs.
        // System.out.println("Title: " + getOutputDir());
    }
    @Override
    protected void renderText(String data, TextMetrics metrics) {

    }

    protected void renderImage(float x, float y, float width, float height, ImageResource data) {
        
    }

}
