package fi.monopoly.presentation.game.desktop;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.SpotType;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public final class DebugController {
    private final MonopolyRuntime runtime;
    private final Board board;
    private final Supplier<Player> currentPlayerSupplier;
    private final Runnable resetTurnState;
    private final Runnable restoreNormalTurnControls;
    private final Runnable retryPendingDebtPayment;
    private final BiConsumer<PaymentRequest, CallbackAction> paymentRequestHandler;

    public DebugController(
            MonopolyRuntime runtime,
            Board board,
            Supplier<Player> currentPlayerSupplier,
            Runnable resetTurnState,
            Runnable restoreNormalTurnControls,
            Runnable retryPendingDebtPayment,
            BiConsumer<PaymentRequest, CallbackAction> paymentRequestHandler
    ) {
        this.runtime = runtime;
        this.board = board;
        this.currentPlayerSupplier = currentPlayerSupplier;
        this.resetTurnState = resetTurnState;
        this.restoreNormalTurnControls = restoreNormalTurnControls;
        this.retryPendingDebtPayment = retryPendingDebtPayment;
        this.paymentRequestHandler = paymentRequestHandler;
    }

    public void initializeDebtDebugScenario() {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        turnPlayer.buyProperty(PropertyFactory.getProperty(SpotType.RR1));
        turnPlayer.addMoney(-(turnPlayer.getMoneyAmount() - 40));
        paymentRequestHandler.accept(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 200, text("game.debug.reason.tax", 200)),
                () -> restoreNormalTurnControls.run()
        );
    }

    public void openGodModeMenu() {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        showDebugMenu(
                text("game.debug.godMode.title", turnPlayer.getName()),
                new ButtonProps(text("game.debug.button.controller"), this::openControllerModeMenu),
                new ButtonProps(text("game.debug.button.money"), this::openMoneyMenu),
                new ButtonProps(text("game.debug.button.move"), this::openMoveMenu),
                new ButtonProps(text("game.debug.button.debt"), this::openDebtMenu),
                new ButtonProps(text("game.debug.button.jail"), this::openJailMenu),
                new ButtonProps(text("game.debug.button.scenarios"), this::openScenarioMenu)
        );
    }

    private void openControllerModeMenu() {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        showDebugMenu(
                text("game.debug.controller.title", turnPlayer.getName(), text(turnPlayer.getComputerProfile().textKey())),
                new ButtonProps(text("game.debug.controller.human"), () -> setCurrentPlayerController(ComputerPlayerProfile.HUMAN)),
                new ButtonProps(text("game.debug.controller.smokeTest"), () -> setCurrentPlayerController(ComputerPlayerProfile.SMOKE_TEST)),
                new ButtonProps(text("game.debug.controller.strong"), () -> setCurrentPlayerController(ComputerPlayerProfile.STRONG))
        );
    }

    private void openMoneyMenu() {
        showDebugMenu(
                text("game.debug.money.title"),
                new ButtonProps(text("game.debug.money.add500"), () -> adjustCurrentPlayerMoney(500)),
                new ButtonProps(text("game.debug.money.minus100"), () -> adjustCurrentPlayerMoney(-100)),
                new ButtonProps(text("game.debug.money.set0"), () -> setCurrentPlayerMoney(0)),
                new ButtonProps(text("game.debug.money.set50"), () -> setCurrentPlayerMoney(50)),
                new ButtonProps(text("game.debug.money.set1500"), () -> setCurrentPlayerMoney(1500))
        );
    }

    private void openMoveMenu() {
        ButtonProps[] spotButtons = SpotType.SPOT_TYPES.stream()
                .map(spotType -> new ButtonProps(spotType.name(), () -> moveCurrentPlayerTo(spotType)))
                .toArray(ButtonProps[]::new);
        showDebugMenu(text("game.debug.move.title"), spotButtons);
    }

    private void openDebtMenu() {
        showDebugMenu(
                text("game.debug.debt.title"),
                new ButtonProps(text("game.debug.debt.50"), () -> startDebtScenario(50)),
                new ButtonProps(text("game.debug.debt.100"), () -> startDebtScenario(100)),
                new ButtonProps(text("game.debug.debt.200"), () -> startDebtScenario(200)),
                new ButtonProps(text("game.debug.debt.500"), () -> startDebtScenario(500)),
                new ButtonProps(text("game.button.retryDebt"), () -> retryPendingDebtPayment.run())
        );
    }

    private void openJailMenu() {
        showDebugMenu(
                text("game.debug.jail.title"),
                new ButtonProps(text("game.debug.jail.send"), this::sendCurrentPlayerToJail),
                new ButtonProps(text("game.debug.jail.oneRound"), () -> setCurrentPlayerJailRounds(1)),
                new ButtonProps(text("game.debug.jail.threeRounds"), () -> setCurrentPlayerJailRounds(JailSpot.JAIL_ROUND_NUMBER)),
                new ButtonProps(text("game.debug.jail.release"), this::releaseCurrentPlayerFromJail)
        );
    }

    private void openScenarioMenu() {
        showDebugMenu(
                text("game.debug.scenario.title"),
                new ButtonProps(text("game.debug.scenario.brownMonopoly"), this::giveBrownMonopoly),
                new ButtonProps(text("game.debug.scenario.brownDebt"), this::setupBrownDebtScenario),
                new ButtonProps(text("game.debug.scenario.railDebt"), this::setupRailroadDebtScenario),
                new ButtonProps(text("game.debug.scenario.resetUi"), () -> resetTurnState.run())
        );
    }

    private void adjustCurrentPlayerMoney(int delta) {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        int targetMoney = Math.max(0, turnPlayer.getMoneyAmount() + delta);
        setCurrentPlayerMoney(targetMoney);
    }

    private void setCurrentPlayerMoney(int targetMoney) {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        turnPlayer.addMoney(targetMoney - turnPlayer.getMoneyAmount());
        showDebugInfo(text("game.debug.money.nowHas", turnPlayer.getName(), turnPlayer.getMoneyAmount()));
    }

    private void setCurrentPlayerController(ComputerPlayerProfile profile) {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        turnPlayer.setComputerProfile(profile);
        showDebugInfo(
                text("game.debug.controller.changed", turnPlayer.getName(), text(profile.textKey()))
        );
    }

    private void moveCurrentPlayerTo(SpotType spotType) {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        Spot targetSpot = board.getPathWithCriteria(spotType);
        if (turnPlayer.isInJail()) {
            JailSpot.jailTimeLeftMap.remove(turnPlayer);
        }
        turnPlayer.setSpot(targetSpot);
        turnPlayer.setCoords(targetSpot.getTokenCoords(turnPlayer));
        showDebugInfo(text("game.debug.move.moved", turnPlayer.getName(), spotType.name()));
    }

    private void startDebtScenario(int amount) {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        log.debug("Debug action: start custom debt scenario for {} amount={}", turnPlayer.getName(), amount);
        paymentRequestHandler.accept(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, amount, text("game.debug.reason.tax", amount)),
                () -> restoreNormalTurnControls.run()
        );
    }

    private void setCurrentPlayerJailRounds(int roundsLeft) {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        Spot jailSpot = board.getPathWithCriteria(SpotType.JAIL);
        JailSpot.jailTimeLeftMap.put(turnPlayer, roundsLeft);
        turnPlayer.setSpot(jailSpot);
        turnPlayer.setCoords(jailSpot.getTokenCoords(turnPlayer));
        showDebugInfo(text("game.debug.jail.rounds", turnPlayer.getName(), roundsLeft));
    }

    private void releaseCurrentPlayerFromJail() {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        JailSpot.jailTimeLeftMap.remove(turnPlayer);
        showDebugInfo(text("game.debug.jail.released", turnPlayer.getName()));
    }

    private void giveBrownMonopoly() {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        assignProperty(turnPlayer, SpotType.B1);
        assignProperty(turnPlayer, SpotType.B2);
        showDebugInfo(text("game.debug.scenario.brownOwned", turnPlayer.getName()));
    }

    private void setupBrownDebtScenario() {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        giveBrownMonopoly();
        StreetProperty b1 = (StreetProperty) PropertyFactory.getProperty(SpotType.B1);
        StreetProperty b2 = (StreetProperty) PropertyFactory.getProperty(SpotType.B2);
        setCurrentPlayerMoney(1500);
        b1.buyHouses(2);
        b2.buyHouses(2);
        setCurrentPlayerMoney(50);
        paymentRequestHandler.accept(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 300, text("game.debug.reason.brownDebt")),
                () -> restoreNormalTurnControls.run()
        );
    }

    private void setupRailroadDebtScenario() {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        assignProperty(turnPlayer, SpotType.RR1);
        setCurrentPlayerMoney(40);
        paymentRequestHandler.accept(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 200, text("game.debug.reason.railDebt")),
                () -> restoreNormalTurnControls.run()
        );
    }

    private void assignProperty(Player player, SpotType spotType) {
        Property property = PropertyFactory.getProperty(spotType);
        Player previousOwner = property.getOwnerPlayer();
        if (previousOwner != null && previousOwner != player) {
            previousOwner.removeOwnedProperty(property);
        }
        property.setMortgaged(false);
        player.addOwnedProperty(property);
    }

    private void sendCurrentPlayerToJail() {
        Player turnPlayer = currentPlayerSupplier.get();
        if (turnPlayer == null) {
            return;
        }
        log.debug("Debug action: send player {} to jail", turnPlayer.getName());
        Spot jailSpot = board.getPathWithCriteria(SpotType.JAIL);
        JailSpot.jailTimeLeftMap.put(turnPlayer, JailSpot.JAIL_ROUND_NUMBER);
        turnPlayer.setSpot(jailSpot);
        turnPlayer.setCoords(jailSpot.getTokenCoords(turnPlayer));
        showDebugInfo(text("game.debug.sentToJail", turnPlayer.getName()));
    }

    private void showDebugMenu(String title, ButtonProps... buttons) {
        runtime.popupService().showManualDecision(title, buttons);
    }

    private void showDebugInfo(String message) {
        runtime.popupService().showManualDecision(message);
    }
}
