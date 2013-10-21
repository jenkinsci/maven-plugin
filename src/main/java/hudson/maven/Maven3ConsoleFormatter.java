/*
 * The MIT License
 *
 * Copyright (c) 2013, Christoph Kutzinski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.maven;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
/**
 * Adapts the standard system-err formatting of Maven3, so that it's less verbose and thus
 * suitable for the Jenkins console.
 * 
 * @author kutzi
 */
class Maven3ConsoleFormatter extends Formatter {

    // Line separator string.  This is the value of the line.separator
    // property at the moment that the SimpleFormatter was created.
    private final String lineSeparator = System.getProperty("line.separator");
    
    @Override
    public String format(LogRecord record) {
        String message = formatMessage(record);
        // translate SEVERE to ERROR, so error highlighting in Jenkins console can kick in
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
