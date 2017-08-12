package hudson.maven.util;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import java.util.logging.Logger;

public final class VariableExpander {

    private static final Logger LOGGER = Logger.getLogger(VariableExpander.class.getName());

    private final AbstractBuild<?, ?> build;
    private final BuildListener listener;

    public VariableExpander(AbstractBuild<?, ?> build, BuildListener listener) {
        this.build = build;
        this.listener = listener;
    }

    public String expand(final String rawString) {
        try {
            return build.getEnvironment(listener).expand(rawString);
        } catch (IOException e) {
            LOGGER.fine("Cannot expand the variables in email recipients.");
        } catch (InterruptedException e) {
            LOGGER.fine("Cannot expand the variables in email recipients.");
        }
        return rawString;
    }
}
