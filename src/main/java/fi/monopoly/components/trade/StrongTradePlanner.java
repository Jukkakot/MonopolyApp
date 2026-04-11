package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerAction;
import fi.monopoly.components.computer.ComputerDecision;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.StrongBotConfig;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;

import java.util.EnumSet;
import java.util.List;

final class StrongTradePlanner {
    private static final int MIN_TRADE_CASH_STEP = 10;
    private static final int DEFAULT_TRADE_CASH_STEP = 20;

    private final TradeOfferEvaluator tradeOfferEvaluator = new TradeOfferEvaluator();
    private final StrongBotConfig config;

    StrongTradePlanner(StrongBotConfig config) {
        this.config = config;
    }

    TradePlan plan(Player proposer, List<Player> players) {
        if (proposer == null || players == null || players.size() < 2) {
            return null;
        }
        Player leader = findLeader(proposer, players);
        TradePlan bestPlan = null;
        for (Player recipient : players) {
            if (recipient == proposer) {
                continue;
            }
            for (Property property : recipient.getOwnedProperties()) {
                TradePlan candidate = buildPlanForProperty(proposer, recipient, property, leader);
                if (candidate == null) {
                    continue;
                }
                if (bestPlan == null || candidate.decision().score() > bestPlan.decision().score()) {
                    bestPlan = candidate;
                }
            }
        }
        return bestPlan;
    }

    private TradePlan buildPlanForProperty(Player proposer, Player recipient, Property property, Player leader) {
        if (!isTradeable(property)) {
            return null;
        }
        StreetType streetType = property.getSpotType().streetType;
        boolean completesSet = completesSetFor(proposer, streetType);
        boolean advancesSet = advancesSetFor(proposer, streetType);
        boolean deniesLeader = blocksLeaderCompletion(leader, proposer, recipient, streetType);
        if (!completesSet && !advancesSet && !deniesLeader) {
            return null;
        }
        if (recipient.ownsAllStreetProperties(streetType) && streetType.placeType == PlaceType.STREET) {
            return null;
        }

        int availableCash = availableTradeCash(proposer);
        if (availableCash < MIN_TRADE_CASH_STEP) {
            return null;
        }

        int startingOffer = roundUpToNearestTen(Math.max(property.getLiquidationValue(), property.getPrice() / 2));
        int maxOffer = roundDownToNearestTen(Math.min(availableCash, maxBidForProperty(property, completesSet, deniesLeader)));
        if (startingOffer > maxOffer) {
            return null;
        }

        BotTradeProfile recipientProfile = profileFor(recipient);
        StrongBotConfig recipientStrongConfig = recipient.getComputerProfile() == ComputerPlayerProfile.STRONG
                ? config
                : null;

        TradePlan bestPlan = null;
        for (int moneyOffer = startingOffer; moneyOffer <= maxOffer; moneyOffer += cashStep(property)) {
            TradeOffer offer = new TradeOffer(
                    proposer,
                    recipient,
                    new TradeSelection(moneyOffer, List.of(), false),
                    new TradeSelection(0, List.of(property), false)
            );
            if (!offer.isValid()) {
                continue;
            }
            if (recipient.isComputerControlled()
                    && !tradeOfferEvaluator.evaluateForRecipient(offer, recipientProfile, recipientStrongConfig).accept()) {
                continue;
            }

            double proposerGain = tradeOfferEvaluator.estimateNetDeltaForRecipient(offer.reversePerspective(), config);
            double totalScore = proposerGain + strategicBonus(property, completesSet, advancesSet, deniesLeader);
            if (proposerGain < -config.tradeFairnessTolerance() && !completesSet) {
                continue;
            }

            if (bestPlan == null || totalScore > bestPlan.decision().score()) {
                bestPlan = new TradePlan(
                        offer,
                        new ComputerDecision(
                                ComputerAction.PROPOSE_TRADE,
                                totalScore,
                                buildReason(property, recipient, completesSet, deniesLeader, moneyOffer, proposerGain)
                        )
                );
            }
        }
        return bestPlan;
    }

    private int availableTradeCash(Player proposer) {
        int reserve = config.minCashReserve();
        boolean hasCompletedSet = false;
        EnumSet<StreetType> seenStreetTypes = EnumSet.noneOf(StreetType.class);
        for (Property property : proposer.getOwnedProperties()) {
            StreetType streetType = property.getSpotType().streetType;
            if (!seenStreetTypes.add(streetType)) {
                continue;
            }
            if (proposer.ownsAllStreetProperties(streetType)) {
                hasCompletedSet = true;
                break;
            }
        }
        if (hasCompletedSet) {
            reserve += config.postMonopolyCashBuffer();
        }
        return Math.max(0, proposer.getMoneyAmount() - reserve);
    }

    private int maxBidForProperty(Property property, boolean completesSet, boolean deniesLeader) {
        int base = property.getPrice();
        int strategicHeadroom = completesSet
                ? config.tradeSetCompletionWeight()
                : deniesLeader
                ? Math.max(60, (int) Math.round(config.tradeSetCompletionWeight() * 0.4))
                : 40;
        return base + strategicHeadroom;
    }

    private double strategicBonus(Property property, boolean completesSet, boolean advancesSet, boolean deniesLeader) {
        double bonus = 0;
        if (completesSet) {
            bonus += config.tradeSetCompletionWeight();
        } else if (advancesSet) {
            bonus += 60;
        }
        if (deniesLeader) {
            bonus += 100 * config.opponentLeaderPressure();
        }
        StreetType streetType = property.getSpotType().streetType;
        if (streetType.placeType == PlaceType.RAILROAD) {
            bonus += config.railroadCompletionWeight();
        } else if (streetType.placeType == PlaceType.UTILITY) {
            bonus += config.utilityCompletionWeight();
        }
        bonus *= config.colorGroupWeight(streetType);
        return bonus;
    }

    private String buildReason(
            Property property,
            Player recipient,
            boolean completesSet,
            boolean deniesLeader,
            int moneyOffer,
            double proposerGain
    ) {
        if (completesSet) {
            return "Propose trade for " + property.getDisplayName()
                    + ": completes " + property.getSpotType().streetType + " for M" + moneyOffer
                    + " (estimated gain " + round(proposerGain) + ")";
        }
        if (deniesLeader) {
            return "Propose denial trade for " + property.getDisplayName()
                    + " from " + recipient.getName() + " for M" + moneyOffer
                    + " (estimated gain " + round(proposerGain) + ")";
        }
        return "Propose progression trade for " + property.getDisplayName()
                + " for M" + moneyOffer + " (estimated gain " + round(proposerGain) + ")";
    }

    private boolean completesSetFor(Player proposer, StreetType streetType) {
        return proposer.countOwnedProperties(streetType) == SpotType.getNumberOfSpots(streetType) - 1;
    }

    private boolean advancesSetFor(Player proposer, StreetType streetType) {
        return proposer.countOwnedProperties(streetType) > 0;
    }

    private boolean blocksLeaderCompletion(Player leader, Player proposer, Player recipient, StreetType streetType) {
        if (leader == null || leader == proposer || leader == recipient) {
            return false;
        }
        return leader.countOwnedProperties(streetType) == SpotType.getNumberOfSpots(streetType) - 1;
    }

    private boolean isTradeable(Property property) {
        if (property == null) {
            return false;
        }
        if (property instanceof StreetProperty streetProperty) {
            return streetProperty.getBuildingLevel() <= 0;
        }
        return true;
    }

    private Player findLeader(Player proposer, List<Player> players) {
        Player leader = null;
        int highestNetWorth = Integer.MIN_VALUE;
        for (Player player : players) {
            if (player == proposer) {
                continue;
            }
            int netWorth = player.getMoneyAmount() + player.getTotalLiquidationValue();
            if (leader == null || netWorth > highestNetWorth) {
                leader = player;
                highestNetWorth = netWorth;
            }
        }
        return leader;
    }

    private BotTradeProfile profileFor(Player player) {
        return switch (player.getComputerProfile()) {
            case SMOKE_TEST -> BotTradeProfile.CAUTIOUS;
            case STRONG -> BotTradeProfile.BALANCED;
            case HUMAN -> BotTradeProfile.AGGRESSIVE;
        };
    }

    private int cashStep(Property property) {
        return property.getPrice() <= 120 ? MIN_TRADE_CASH_STEP : DEFAULT_TRADE_CASH_STEP;
    }

    private int roundDownToNearestTen(int value) {
        return Math.max(0, value - (value % 10));
    }

    private int roundUpToNearestTen(int value) {
        return (int) (Math.ceil(Math.max(0, value) / 10.0) * 10);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    record TradePlan(TradeOffer offer, ComputerDecision decision) {
    }
}
