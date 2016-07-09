/*
 * The MIT License
 * 
 * Copyright (c) 2016 CloudBess, Inc.
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
import hudson.model.Item;
import hudson.tasks.MavenSelector;
import hudson.tasks.Maven.MavenInstallation;

/**
 * {@link MavenSelector} extension for {@link MavenModule} and {@link MavenModuleSet} {@link Item}s
 */
@Extension
public class MavenPluginMavenSelectorImpl extends MavenSelector {

    @Override
    public boolean isApplicable(Class<? extends Item> itemClass) {
        return itemClass.isAssignableFrom(MavenModule.class) || itemClass.isAssignableFrom(MavenModuleSet.class);
    }

    @Override
    public MavenInstallation selectMavenInstallation(Item item) {
        if (item instanceof MavenModule) {
            return ((MavenModule) item).getParent().getMaven();
        } else if (item instanceof MavenModuleSet) {
            return ((MavenModuleSet) item).getMaven();
        }
        return null;
    }

}
