package hudson.maven;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.Issue;

/**
 * Unit test for {@link ExecutedMojo}.
 * 
 * @author kutzi
 */
class ExecutedMojoTest {
    
    private MojoDescriptor mojoDescriptor;
    private Level oldLevel;

    @BeforeEach
    void beforeEach() {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("com.test");
        pluginDescriptor.setArtifactId("testPlugin");
        pluginDescriptor.setVersion("1.0");
        
        ClassRealm classRealm = new ClassRealm(null, "test", getClass().getClassLoader());
        pluginDescriptor.setClassRealm(classRealm);
        
        MojoDescriptor descriptor = new MojoDescriptor();
        descriptor.setPluginDescriptor(pluginDescriptor);
        this.mojoDescriptor = descriptor;
        
        // suppress the WARNING logs we expect
        this.oldLevel = Logger.getLogger(ExecutedMojo.class.getName()).getLevel();
        Logger.getLogger(ExecutedMojo.class.getName()).setLevel(Level.SEVERE);
    }

    @AfterEach
    void afterEach() {
        Logger.getLogger(ExecutedMojo.class.getName()).setLevel(oldLevel);
    }

    @Test
    void testMojoFromJarFile() {
        // Faking JUnit's Assertions to be the plugin class
        this.mojoDescriptor.setImplementation(Assertions.class.getName());
        MojoExecution execution = new MojoExecution(this.mojoDescriptor);
        MojoInfo info = new MojoInfo(execution, null, null, null, -1);
        
        ExecutedMojo executedMojo = new ExecutedMojo(info, 1L);
        
        assertNotNull(executedMojo.digest);
    }

    @Test
    @Issue("JENKINS-5044")
    void testMojoFromClassesDirectory() {
        // Faking this class as the mojo impl:
        this.mojoDescriptor.setImplementation(getClass().getName());
        MojoExecution execution = new MojoExecution(this.mojoDescriptor);
        MojoInfo info = new MojoInfo(execution, null, null, null, -1);
        
        ExecutedMojo executedMojo = new ExecutedMojo(info, 1L);
        
        assertEquals("com.test", executedMojo.groupId);
    }
}
