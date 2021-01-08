package lib.hudson;

import static org.junit.Assert.assertTrue;

import com.gargoylesoftware.htmlunit.html.DomNodeUtil;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

import hudson.maven.MavenModuleSet;
import hudson.model.Item;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public class ListScmBrowsersTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    public void selectBoxesUnique_MavenProject() throws Exception {
        check(j.jenkins.createProject(MavenModuleSet.class, "p"));
    }

    private void check(Item p) throws IOException, SAXException {
        HtmlPage page = j.createWebClient().getPage(p, "configure");
        List<HtmlSelect> selects = DomNodeUtil.selectNodes(page, "//select");
        assertTrue(selects.size() > 0);
        for (HtmlSelect select : selects) {
            Set<String> title = new HashSet<>();
            for (HtmlOption o : select.getOptions()) {
                assertTrue("Duplicate entry: " + o.getText(), title.add(o.getText()));
            }
        }
    }
}
