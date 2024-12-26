/**
 * MyPDFToHTML.java
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
import org.fit.pdfdom.resource.HtmlResourceHandler;
import org.fit.pdfdom.resource.IgnoreResourceHandler;
import org.fit.pdfdom.resource.SaveResourceToDirHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.w3c.dom.Document;

/**
 * @author burgetr
 *
 */
public class MyPDFToHTML {  // Updated class name
    private static final PDFDomTreeConfig config = null;
    
        public static PDFDomTreeConfig globalConfig;
    
        // Path to the PDF file
        String filePath = "test.pdf";
        String outputDir = "output";
        // globalConfig = PDFDomTreeConfig.createDefaultConfig();
        public String newImgSrcString = "newImage";
    
        public static String inFile = "";
        public static String outFile = "";
    
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
                System.out.println("Usage: MyPDFToHTML <infile> [<outfile>] [<options>]");  // Updated class name
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
            try {
                // Some operations
                System.out.println("Before PDF to HTML conversion\n");
    
                // Prepare the arguments for MyPDFToHTML (formerly PDFToHTML)
                String[] pdfToHtmlArgs = prepareArguments(args);
    
                // Load the MyPDFToHTML class dynamically (formerly PDFToHTML)
                Class<?> pdfToHtmlClass = Class.forName("org.fit.pdfdom.MyPDFToHTML");  // Updated class name
    
                // Call the main method of MyPDFToHTML class dynamically (formerly PDFToHTML)
                pdfToHtmlClass.getMethod("main", String[].class).invoke(null, (Object) pdfToHtmlArgs);
    
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
                System.out.println("Usage: MyPDFToHTML <infile> [<outfile>] [<options>]");  // Updated class name
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
                // System.out.println("PDF loaded successfully in MyPDFToHTML.");  // Updated class name
    
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
                    MyPDFToHTML.inFile, // Input file (updated class name)
                    "output.html", // Output file
                    "-fm=EMBED_BASE64", // Font handler mode
                    "-fdir=output/fonts", // Font directory
                    "-im=SAVE_TO_DIR", // Image handler mode
                    "-idir=output/images" // Image directory
            };
    
            callMyPDFToHTML(hardcodedArgs);
        }
    
        public void callMyPDFToHTML(String[] args) {
            // Path to the PDF file
            String filePath = "test.pdf";
            String outputDir = "output";
            globalConfig = PDFDomTreeConfig.createDefaultConfig();
    
            if (args.length < 1) {
                System.out.println("Usage: MyPDFToHTML <infile> [<outfile>] [<options>]");  // Updated class name
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
                // System.out.println("PDF loaded successfully in MyPDFToHTML.");  // Updated class name
    
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
        }
    
        public static PDFDomTreeConfig parseOptions(String[] args) {
            //PDFDomTreeConfig config = new PDFDomTreeConfig();
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("-fm=")) {
                    String mode = args[i].substring(4);
                    if (mode.equals("EMBED_BASE64")) {
                        //config.setFontHandlerMode(PDFDomTreeConfig.FontHandlerMode.EMBED_BASE64);
                    } else if (mode.equals("SAVE_TO_DIR")) {
                        //config.setFontHandlerMode(PDFDomTreeConfig.FontHandlerMode.SAVE_TO_DIR);
                    } else if (mode.equals("IGNORE")) {
                        //config.setFontHandlerMode(PDFDomTreeConfig.FontHandlerMode.IGNORE);
                    }
                } else if (args[i].startsWith("-fdir=")) {
                    String path = args[i].substring(6);
                    //config.setFontDirectory(path);
                } else if (args[i].startsWith("-im=")) {
                    String mode = args[i].substring(4);
                    if (mode.equals("EMBED_BASE64")) {
                        //config.setImageHandlerMode(PDFDomTreeConfig.ImageHandlerMode.EMBED_BASE64);
                    } else if (mode.equals("SAVE_TO_DIR")) {
                        //config.setImageHandlerMode(PDFDomTreeConfig.ImageHandlerMode.SAVE_TO_DIR);
                    } else if (mode.equals("IGNORE")) {
                        //config.setImageHandlerMode(PDFDomTreeConfig.ImageHandlerMode.IGNORE);
                    }
                } else if (args[i].startsWith("-idir=")) {
                    String path = args[i].substring(6);
                    //config.setImageDirectory(path);
                }
            }
            return config;
    }

    private static String[] prepareArguments(String[] args) {
        List<String> preparedArgs = new ArrayList<>();
        for (String arg : args) {
            preparedArgs.add(arg);
        }
        return preparedArgs.toArray(new String[0]);
    }
}
