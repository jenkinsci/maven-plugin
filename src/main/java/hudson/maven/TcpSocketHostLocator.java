package hudson.maven;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.List;

/**
 * Identify the Host name to use from maven-agent to connect to to jenkins slave agent TCP socket.
 * <p>
 * In simple scenarios both slave agent and maven process do live on same host without specific network
 * constraints, but for some virtualization usages maven process just can't bind a socket on wildcard
 * host network. This extension give infrastructure plugins a chance to configure the adequate hostname.
 * to handle such network constraints
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @since 2.12
 */
public abstract class TcpSocketHostLocator implements ExtensionPoint {

    /**
     * Try to identify the slave agent TCP socket host name or IP.
     * @return <code>null</code> if not found or does not apply to this specific implementation
     * @throws IOException
     */
    public abstract @CheckForNull String getTcpSocketHost() throws IOException;

    public static List<TcpSocketHostLocator> all() {
        return ExtensionList.lookup(TcpSocketHostLocator.class);
    }

}
