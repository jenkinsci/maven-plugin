package hudson.maven;

import hudson.Util;
import org.jvnet.hudson.test.ExtractChangeLogParser;
import org.jvnet.hudson.test.ExtractResourceWithChangesSCM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

/**
 * Fixed the changelog in ExtractResourceWithChangesSCM.
 */
public class ExtractResourceWithAllChangesSCM extends ExtractResourceWithChangesSCM {

    public ExtractResourceWithAllChangesSCM(URL firstZip, URL secondZip) {
        super(firstZip, secondZip);
    }

    public ExtractResourceWithAllChangesSCM(URL firstZip, URL secondZip, String moduleRoot) {
        super(firstZip, secondZip, moduleRoot);
    }

    private static String escapeForXml(String string) {
        return Util.xmlEscape(Util.fixNull(string));
    }

    public void saveToChangeLog(File changeLogFile, ExtractChangeLogParser.ExtractChangeLogEntry changeLog) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(changeLogFile);

        PrintStream stream = new PrintStream(outputStream, false, "UTF-8");

        stream.println("<?xml version='1.0' encoding='UTF-8'?>");
        stream.println("<extractChanges>");
        stream.println("<entry>");
        stream.println("<zipFile>" + escapeForXml(changeLog.getZipFile()) + "</zipFile>");

        for (String fileName : changeLog.getAffectedPaths()) {
            stream.println("<file>");
            stream.println("<fileName>" + escapeForXml(fileName) + "</fileName>");
            stream.println("</file>");
        }

        stream.println("</entry>");
        stream.println("</extractChanges>");

        stream.close();
    }


}
