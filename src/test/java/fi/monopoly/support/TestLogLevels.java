package fi.monopoly.support;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

public final class TestLogLevels {

    private TestLogLevels() {
    }

    public static LogLevelSnapshot forceWarnOnly() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        Logger monopolyLogger = (Logger) LoggerFactory.getLogger("fi.monopoly");
        Level previousRootLevel = rootLogger.getLevel();
        Level previousMonopolyLevel = monopolyLogger.getLevel();
        rootLogger.setLevel(Level.WARN);
        monopolyLogger.setLevel(Level.WARN);
        return new LogLevelSnapshot(previousRootLevel, previousMonopolyLevel);
    }

    public record LogLevelSnapshot(Level rootLevel, Level monopolyLevel) {
        public void restore() {
            Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            Logger monopolyLogger = (Logger) LoggerFactory.getLogger("fi.monopoly");
            rootLogger.setLevel(rootLevel);
            monopolyLogger.setLevel(monopolyLevel);
        }
    }
}
