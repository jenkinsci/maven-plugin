package lib.hudson;

import org.htmlunit.html.DomNodeUtil;
import org.htmlunit.html.HtmlOption;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;

import hudson.maven.MavenModuleSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import hudson.model.Item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class ListScmBrowsersTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void selectBoxesUnique_MavenProject() throws Exception {
        check(j.jenkins.createProject(MavenModuleSet.class, "p"));
    }

    private void check(Item p) throws Exception {
        HtmlPage page = j.createWebClient().getPage(p, "configure");
        List<HtmlSelect> selects = DomNodeUtil.selectNodes(page, "//select");
        assertFalse(selects.isEmpty());
        for (HtmlSelect select : selects) {
            Set<String> title = new HashSet<>();
            for (HtmlOption o : select.getOptions()) {
                assertTrue(title.add(o.getText()), "Duplicate entry: " + o.getText());
            }
        }
    }
}
