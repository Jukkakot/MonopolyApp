package fi.monopoly.application.session;

import fi.monopoly.application.command.RefreshSessionViewCommand;
import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.application.result.CommandRejection;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.domain.session.SessionState;

import java.util.List;
import java.util.function.Supplier;

public final class SessionApplicationService {
    private final String sessionId;
    private final Supplier<SessionState> sessionStateSupplier;

    public SessionApplicationService(String sessionId, Supplier<SessionState> sessionStateSupplier) {
        this.sessionId = sessionId;
        this.sessionStateSupplier = sessionStateSupplier;
    }

    public SessionState currentState() {
        return sessionStateSupplier.get();
    }

    public CommandResult handle(SessionCommand command) {
        if (command instanceof RefreshSessionViewCommand refreshSessionViewCommand) {
            if (!sessionId.equals(refreshSessionViewCommand.sessionId())) {
                return rejected("WRONG_SESSION", "Command session does not match active session");
            }
            return accepted();
        }
        return rejected("UNSUPPORTED_COMMAND", "Command is not supported by the PR1 seam");
    }

    private CommandResult accepted() {
        return new CommandResult(true, currentState(), List.of(), List.of(), List.of());
    }

    private CommandResult rejected(String code, String message) {
        return new CommandResult(false, currentState(), List.of(), List.of(new CommandRejection(code, message)), List.of());
    }
}
