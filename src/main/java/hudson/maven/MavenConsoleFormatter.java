package hudson.maven;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class MavenConsoleFormatter extends Formatter {

    // Line separator string.  This is the value of the line.separator
    // property at the moment that the SimpleFormatter was created.
    private final String lineSeparator = System.getProperty("line.separator");
    
    @Override
    public String format(LogRecord record) {
        String message = formatMessage(record);
        String level = record.getLevel() == Level.SEVERE ? "ERROR" : record.getLevel().getName();
        String logMsg = "[" + level
                +   "] "
                + message
                + lineSeparator;
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
            logMsg += sw.toString();
            } catch (Exception ex) {
            }
        }
        return logMsg;
    }

}
