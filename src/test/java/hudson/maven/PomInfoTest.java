package hudson.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PomInfoTest {
    private MavenProject project;

    @Before
    public void setUp() {
        project = new MavenProject();
        project.setGroupId("test");
        project.setArtifactId("testmodule");
        project.setVersion("2.0-SNAPSHOT");
        project.setPackaging("jar");
        project.setOriginalModel(new Model());
        project.getOriginalModel().setDependencyManagement(new DependencyManagement());
    }

    @Test
    public void testImportedDependencyManagementIncludedInDependencies() {
        Dependency depImport = new Dependency();
        depImport.setGroupId("importedGroup");
        depImport.setArtifactId("importedArtifact");
        depImport.setVersion("1.0-SNAPSHOT");
        depImport.setScope("import");
        project.getOriginalModel().getDependencyManagement().addDependency(depImport);
        ModuleDependency modDepImport = new ModuleDependency(depImport);

        final PomInfo pomInfo = new PomInfo(project, null, "relPath");
        Assert.assertTrue(pomInfo.dependencies.contains(modDepImport));
    }

    @Test
    public void testRegularDependencyManagementNotIncludedInDependencies() {
        Dependency depImport = new Dependency();
        depImport.setGroupId("group");
        depImport.setArtifactId("artifact");
        depImport.setVersion("1.0-SNAPSHOT");
        project.getOriginalModel().getDependencyManagement().addDependency(depImport);
        ModuleDependency modDepImport = new ModuleDependency(depImport);

        final PomInfo pomInfo = new PomInfo(project, null, "relPath");
        Assert.assertFalse(pomInfo.dependencies.contains(modDepImport));
    }
}
