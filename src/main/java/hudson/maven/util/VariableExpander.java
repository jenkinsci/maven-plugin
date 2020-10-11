package hudson.maven.util;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.logging.Logger;

public final class VariableExpander {

    private static final Logger LOGGER = Logger.getLogger(VariableExpander.class.getName());

    private final AbstractBuild<?, ?> build;
    private final TaskListener listener;

    public VariableExpander(AbstractBuild<?, ?> build, TaskListener listener) {
        if (build == null) {
            throw new IllegalArgumentException("'build' cannot be null.");
        }
        if (listener == null) {
            throw new IllegalArgumentException("'listener' cannot be null.");
        }
        this.build = build;
        this.listener = listener;
    }

    public String expand(final String rawString) {
        try {
            return build.getEnvironment(listener).expand(rawString);
        } catch (IOException | InterruptedException e) {
            LOGGER.fine("Cannot expand the variables in email recipients.");
        }
        return rawString;
    }
}
