package fi.monopoly.client.desktop;

import fi.monopoly.client.session.desktop.DesktopClientSessionRuntime;
import fi.monopoly.client.session.desktop.DesktopClientViewModels;
import fi.monopoly.host.session.local.DesktopHostedGameTestAccess;

/**
 * Bundles one client-side desktop host binding.
 *
 * <p>The app shell should only see the client runtime/session/view models it needs, not the
 * concrete embedded-host bootstrap graph used to produce them.</p>
 */
public record DesktopClientHostBinding(
        DesktopRuntimeAccess runtimeAccess,
        DesktopClientViewModels viewModels,
        DesktopClientSessionRuntime sessionRuntime,
        DesktopHostedGameTestAccess testAccess
) {
}
