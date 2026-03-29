package fi.monopoly.support;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.slf4j.LoggerFactory;

public final class TestLogLevels {
    private static final String DEFAULT_CONFIG = "logback-test.xml";
    private static final String SIMULATION_CONFIG = "logback-simulation-test.xml";

    private TestLogLevels() {
    }

    public static LogConfigSnapshot useDefaultTestLogging() {
        configure(DEFAULT_CONFIG);
        return new LogConfigSnapshot(DEFAULT_CONFIG);
    }

    public static LogConfigSnapshot useSimulationLogging() {
        configure(SIMULATION_CONFIG);
        return new LogConfigSnapshot(DEFAULT_CONFIG);
    }

    private static synchronized void configure(String resourceName) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try {
            configurator.doConfigure(TestLogLevels.class.getClassLoader().getResource(resourceName));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure logging from " + resourceName, e);
        }
    }

    public record LogConfigSnapshot(String restoreConfigResource) {
        public void restore() {
            configure(restoreConfigResource);
        }
    }
}
