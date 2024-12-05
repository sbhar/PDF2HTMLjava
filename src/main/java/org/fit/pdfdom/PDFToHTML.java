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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
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
public class PDFToHTML {
    public static PDFDomTreeConfig globalConfig;

    // Path to the PDF file
    String filePath = "test.pdf";
    String outputDir = "output";
    // globalConfig = PDFDomTreeConfig.createDefaultConfig();
    public String newImgSrcString = "newImage";

    public static String inFile ="";

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
            System.out.println(infile);

            PDFDomTreeOld parser = new PDFDomTreeOld(config);
            // Load the PDF document
            document = PDDocument.load(new File(infile));
            // // Create an instance of your concrete PDFBoxTree class
            MyConcretePDFBoxTree processor = new MyConcretePDFBoxTree(outputDir);

            processor.processDocument(document);

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

        org.fit.pdfdom.PDFDomTree parser = null;

        try {

            Class<?> parserClass = Class.forName("org.fit.pdfdom.PDFDomTree");
            //System.out.println("Class loaded: " + parserClass.getName());
            Object parserObj = parserClass.getConstructor(PDFDomTreeConfig.class).newInstance(config);
            parser = (org.fit.pdfdom.PDFDomTree) parserObj;
            document = PDDocument.load(new File(PDFToHTML.inFile));
            parser.createDOM(document);

            // Create a Writer for the output HTML file
            try (Writer output = new FileWriter("Output_Default.html")) {
                // Call the writeText method to convert PDF to HTML
                parser.writeText(document, output);
            }

            System.out.println("Conversion successful! Output written;Output_Default.html ");

            // Close the document
            document.close();

        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
            e.printStackTrace();
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
}
