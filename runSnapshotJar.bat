@echo off
REM Batch file to run PDFToHTML with the specified classpath and arguments

REM Define the classpath
set CLASSPATH="target/pdf2dom-2.0.4-SNAPSHOT.jar;D:\Pdf2Dom\target\test-classes;D:\Pdf2Dom\target\classes;C:\Users\Administrator\.m2\repository\org\apache\pdfbox\pdfbox\2.0.27\pdfbox-2.0.27.jar;C:\Users\Administrator\.m2\repository\org\apache\pdfbox\fontbox\2.0.27\fontbox-2.0.27.jar;C:\Users\Administrator\.m2\repository\commons-logging\commons-logging\1.2\commons-logging-1.2.jar;C:\Users\Administrator\.m2\repository\net\mabboud\fontverter\FontVerter\1.2.22\FontVerter-1.2.22.jar;C:\Users\Administrator\.m2\repository\org\reflections\reflections\0.9.9\reflections-0.9.9.jar;C:\Users\Administrator\.m2\repository\com\google\guava\guava\15.0\guava-15.0.jar;C:\Users\Administrator\.m2\repository\org\javassist\javassist\3.18.2-GA\javassist-3.18.2-GA.jar;C:\Users\Administrator\.m2\repository\com\google\code\findbugs\annotations\2.0.1\annotations-2.0.1.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.4\commons-lang3-3.4.jar;C:\Users\Administrator\.m2\repository\commons-io\commons-io\2.11.0\commons-io-2.11.0.jar;C:\Users\Administrator\.m2\repository\org\apache\fontbox\fontbox\2.0.29\fontbox-2.0.29.jar;C:\Users\Administrator\.m2\repository\org\slf4j\slf4j-api\1.7.32\slf4j-api-1.7.32.jar;C:\Users\Administrator\.m2\repository\junit\junit\4.13.2\junit-4.13.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest-core\1.3\hamcrest-core-1.3.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.15.3\jsoup-1.15.3.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest-all\1.3\hamcrest-all-1.3.jar;C:\Users\Administrator\.m2\repository\commons-codec\commons-codec\1.15\commons-codec-1.15.jar;C:\Users\Administrator\.m2\repository\org\slf4j\slf4j-simple\1.7.32\slf4j-simple-1.7.32.jar;C:\Users\Administrator\.m2\repository\net\mabboud\gfxassert\GfxAssert\1.0.4\GfxAssert-1.0.4.jar"

REM Run the Java command
java -cp %CLASSPATH% org.fit.pdfdom.PDFToHTML test.pdf output2.html -fm=SAVE_TO_DIR -idir=output/images -im=SAVE_TO_DIR -fdir=output/fonts

REM Pause to see the output
pause
