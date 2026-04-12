package fi.monopoly.application.session;

import fi.monopoly.application.command.DeclareBankruptcyCommand;
import fi.monopoly.application.command.MortgagePropertyForDebtCommand;
import fi.monopoly.application.command.PayDebtCommand;
import fi.monopoly.application.command.SellBuildingForDebtCommand;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DebtRemediationCommandHandlerTest {

    @BeforeEach
    void resetPropertyFactory() {
        PropertyFactory.resetState();
    }

    @Test
    void mortgageCommandRaisesCashAndKeepsDebtOpen() {
        Player debtor = new Player("Debtor", Color.MEDIUMPURPLE, 0, 1, ComputerPlayerProfile.HUMAN);
        Property railroad = PropertyFactory.getProperty(SpotType.RR1);
        debtor.addOwnedProperty(railroad);
        ActiveDebtSupplier debtSupplier = new ActiveDebtSupplier(debtor, false);
        FakeGateway gateway = new FakeGateway();
        gateway.debtSupplier = debtSupplier;
        DebtRemediationCommandHandler handler = newHandler(debtor, debtSupplier, gateway);

        var result = handler.handle(new MortgagePropertyForDebtCommand("local-session", playerId(debtor), debtSupplier.debtId(), SpotType.RR1.name()));

        assertTrue(result.accepted());
        assertTrue(railroad.isMortgaged());
        assertEquals(100, debtor.getMoneyAmount());
        assertNotNull(result.sessionState().activeDebt());
        assertEquals("PropertyMortgaged", result.events().get(0).eventType());
    }

    @Test
    void sellBuildingCommandRaisesCashAndKeepsDebtOpen() {
        Player debtor = new Player("Debtor", Color.MEDIUMPURPLE, 0, 1, ComputerPlayerProfile.HUMAN);
        StreetProperty b1 = (StreetProperty) PropertyFactory.getProperty(SpotType.B1);
        StreetProperty b2 = (StreetProperty) PropertyFactory.getProperty(SpotType.B2);
        debtor.addOwnedProperty(b1);
        debtor.addOwnedProperty(b2);
        debtor.addMoney(500);
        assertTrue(b1.buyHouses(1));
        debtor.addMoney(-debtor.getMoneyAmount());
        ActiveDebtSupplier debtSupplier = new ActiveDebtSupplier(debtor, false);
        FakeGateway gateway = new FakeGateway();
        gateway.debtSupplier = debtSupplier;
        DebtRemediationCommandHandler handler = newHandler(debtor, debtSupplier, gateway);

        var result = handler.handle(new SellBuildingForDebtCommand("local-session", playerId(debtor), debtSupplier.debtId(), SpotType.B1.name(), 1));

        assertTrue(result.accepted());
        assertEquals(0, b1.getHouseCount());
        assertEquals(25, debtor.getMoneyAmount());
        assertNotNull(result.sessionState().activeDebt());
        assertEquals("BuildingSold", result.events().get(0).eventType());
    }

    @Test
    void payDebtCommandRejectsWhenCashIsStillInsufficient() {
        Player debtor = new Player("Debtor", Color.MEDIUMPURPLE, 50, 1, ComputerPlayerProfile.HUMAN);
        ActiveDebtSupplier debtSupplier = new ActiveDebtSupplier(debtor, false);
        FakeGateway gateway = new FakeGateway();
        gateway.debtSupplier = debtSupplier;
        DebtRemediationCommandHandler handler = newHandler(debtor, debtSupplier, gateway);

        var result = handler.handle(new PayDebtCommand("local-session", playerId(debtor), debtSupplier.debtId()));

        assertFalse(result.accepted());
        assertEquals("DEBT_NOT_PAYABLE", result.rejections().get(0).code());
        assertFalse(gateway.payDebtCalled.get());
    }

    @Test
    void payDebtCommandResolvesDebtWhenCashIsEnough() {
        Player debtor = new Player("Debtor", Color.MEDIUMPURPLE, 100, 1, ComputerPlayerProfile.HUMAN);
        ActiveDebtSupplier debtSupplier = new ActiveDebtSupplier(debtor, false);
        FakeGateway gateway = new FakeGateway();
        gateway.debtSupplier = debtSupplier;
        gateway.onPayDebt = debtSupplier::clear;
        DebtRemediationCommandHandler handler = newHandler(debtor, debtSupplier, gateway);

        var result = handler.handle(new PayDebtCommand("local-session", playerId(debtor), debtSupplier.debtId()));

        assertTrue(result.accepted());
        assertTrue(gateway.payDebtCalled.get());
        assertNull(result.sessionState().activeDebt());
        assertEquals(TurnPhase.WAITING_FOR_END_TURN, result.sessionState().turn().phase());
    }

    @Test
    void declareBankruptcyCommandRequiresBankruptcyRisk() {
        Player debtor = new Player("Debtor", Color.MEDIUMPURPLE, 0, 1, ComputerPlayerProfile.HUMAN);
        ActiveDebtSupplier debtSupplier = new ActiveDebtSupplier(debtor, false);
        FakeGateway gateway = new FakeGateway();
        gateway.debtSupplier = debtSupplier;
        DebtRemediationCommandHandler handler = newHandler(debtor, debtSupplier, gateway);

        var result = handler.handle(new DeclareBankruptcyCommand("local-session", playerId(debtor), debtSupplier.debtId()));

        assertFalse(result.accepted());
        assertEquals("BANKRUPTCY_NOT_ALLOWED", result.rejections().get(0).code());
    }

    private DebtRemediationCommandHandler newHandler(Player debtor, ActiveDebtSupplier debtSupplier, FakeGateway gateway) {
        return new DebtRemediationCommandHandler(
                "local-session",
                () -> new SessionState(
                        "local-session",
                        0L,
                        SessionStatus.IN_PROGRESS,
                        List.of(new SeatState("seat-0", 0, playerId(debtor), SeatKind.HUMAN, ControlMode.MANUAL, debtor.getName())),
                        List.of(new PlayerSnapshot(playerId(debtor), "seat-0", debtor.getName(), debtor.getMoneyAmount(), -1, false, false, false, 0, List.of())),
                        new TurnState(playerId(debtor), debtSupplier.get() == null ? TurnPhase.WAITING_FOR_END_TURN : TurnPhase.RESOLVING_DEBT, false, false),
                        null,
                        null,
                        debtSupplier.get(),
                        null
                ),
                debtSupplier::set,
                gateway
        );
    }

    private static String playerId(Player player) {
        return "player-" + player.getId();
    }

    private static final class ActiveDebtSupplier {
        private final Player debtor;
        private final boolean bankruptcyRisk;
        private DebtStateModel activeDebt;

        private ActiveDebtSupplier(Player debtor, boolean bankruptcyRisk) {
            this.debtor = debtor;
            this.bankruptcyRisk = bankruptcyRisk;
            this.activeDebt = build();
        }

        private DebtStateModel get() {
            if (activeDebt == null) {
                return null;
            }
            activeDebt = build();
            return activeDebt;
        }

        private void clear() {
            activeDebt = null;
        }

        private void set(DebtStateModel debtStateModel) {
            activeDebt = debtStateModel;
        }

        private String debtId() {
            return build().debtId();
        }

        private DebtStateModel build() {
            return new DebtStateModel(
                    "debt:" + playerId(debtor),
                    playerId(debtor),
                    DebtCreditorType.BANK,
                    null,
                    100,
                    "Debt",
                    bankruptcyRisk,
                    debtor.getMoneyAmount(),
                    debtor.getTotalLiquidationValue(),
                    bankruptcyRisk
                            ? List.of(DebtAction.PAY_DEBT_NOW, DebtAction.MORTGAGE_PROPERTY, DebtAction.SELL_BUILDING, DebtAction.SELL_BUILDING_ROUNDS_ACROSS_SET, DebtAction.DECLARE_BANKRUPTCY)
                            : List.of(DebtAction.PAY_DEBT_NOW, DebtAction.MORTGAGE_PROPERTY, DebtAction.SELL_BUILDING, DebtAction.SELL_BUILDING_ROUNDS_ACROSS_SET)
            );
        }
    }

    private static final class FakeGateway extends LegacyDebtRemediationGateway {
        private AtomicBoolean payDebtCalled = new AtomicBoolean(false);
        private Runnable onPayDebt = () -> {
        };
        private ActiveDebtSupplier debtSupplier;

        private FakeGateway() {
            super(null);
        }

        @Override
        public Property propertyById(String propertyId) {
            return PropertyFactory.getProperty(SpotType.valueOf(propertyId));
        }

        @Override
        public DebtState activeDebtState() {
            DebtStateModel debt = debtSupplier.get();
            if (debt == null) {
                return null;
            }
            Player debtor = debtSupplier.debtor;
            return new DebtState(
                    new PaymentRequest(debtor, BankTarget.INSTANCE, debt.amountRemaining(), debt.reason()),
                    () -> {
                    },
                    debt.bankruptcyRisk()
            );
        }

        @Override
        public boolean mortgageProperty(String propertyId) {
            return propertyById(propertyId).handleMortgaging();
        }

        @Override
        public boolean sellBuildings(String propertyId, int count) {
            return propertyById(propertyId) instanceof StreetProperty streetProperty && streetProperty.sellHouses(count);
        }

        @Override
        public void payDebtNow() {
            payDebtCalled.set(true);
            onPayDebt.run();
        }
    }
}
