/*
 * The MIT License
 *
 * Copyright (c) 20014, Diabol AB, Patrik Boström
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

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;
import java.util.Map;

@Extension(optional = true)
@SuppressWarnings("UnusedDeclaration")
public class PomVersionTokenMacro extends DataBoundTokenMacro {

    private static final String NAME = "POM_VERSION";

    // Force a classloading error TokenMacro plugin isn't available
    @SuppressWarnings("UnusedDeclaration")
    public static final Class clazz = DataBoundTokenMacro.class;


    @DataBoundTokenMacro.Parameter(required = false)
    public boolean stripSnapshot = false;

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        Map<String, String> env = context.getEnvironment(listener);
        if (env.containsKey(NAME)) {
            if (stripSnapshot) {
                String version = env.get(NAME);
                return version.replace("-SNAPSHOT", "");
            } else {
                return env.get(NAME);
            }
        }
        return "";
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return "POM_VERSION".equals(macroName);
    }
}