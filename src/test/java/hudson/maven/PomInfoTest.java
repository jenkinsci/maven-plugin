package hudson.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class PomInfoTest {

    private MavenProject project;

    @BeforeEach
    void beforeEach() {
        project = new MavenProject();
        project.setGroupId("test");
        project.setArtifactId("testmodule");
        project.setVersion("2.0-SNAPSHOT");
        project.setPackaging("jar");
        project.setOriginalModel(new Model());
        project.getOriginalModel().setDependencyManagement(new DependencyManagement());
    }

    @Test
    void testImportedDependencyManagementIncludedInDependencies() {
        Dependency depImport = new Dependency();
        depImport.setGroupId("importedGroup");
        depImport.setArtifactId("importedArtifact");
        depImport.setVersion("1.0-SNAPSHOT");
        depImport.setScope("import");
        project.getOriginalModel().getDependencyManagement().addDependency(depImport);
        ModuleDependency modDepImport = new ModuleDependency(depImport);

        final PomInfo pomInfo = new PomInfo(project, null, "relPath");
        assertTrue(pomInfo.dependencies.contains(modDepImport));
    }

    @Test
    void testRegularDependencyManagementNotIncludedInDependencies() {
        Dependency depImport = new Dependency();
        depImport.setGroupId("group");
        depImport.setArtifactId("artifact");
        depImport.setVersion("1.0-SNAPSHOT");
        project.getOriginalModel().getDependencyManagement().addDependency(depImport);
        ModuleDependency modDepImport = new ModuleDependency(depImport);

        final PomInfo pomInfo = new PomInfo(project, null, "relPath");
        assertFalse(pomInfo.dependencies.contains(modDepImport));
    }
}
