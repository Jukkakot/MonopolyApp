package fi.monopoly.components.computer;

import fi.monopoly.types.StreetType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tunable weights and switches for the {@code STRONG} computer player.
 *
 * @param buyThreshold baseline purchase threshold once the board is no longer in the
 *                     generous early-game phase.
 *                     Higher value means the bot declines more medium-value property offers;
 *                     lower value means it buys more aggressively.
 *                     Example: raising this from {@code 6.5} to {@code 8.0} makes the bot skip
 *                     more single streets unless they complete or strongly improve a set.
 * @param minCashReserve minimum cash the bot tries to keep in normal conditions.
 *                       Higher value makes the bot safer but slower to buy/build;
 *                       lower value makes it spend more freely.
 *                       Example: raising this from {@code 250} to {@code 350} leaves more buffer
 *                       after purchases, but some otherwise acceptable early buys will be declined.
 * @param dangerCashReserve larger reserve used when the board is considered dangerous or late-game.
 *                          Higher value makes the bot much more conservative around houses, hotels,
 *                          and unmortgaging; lower value makes it push advantage harder.
 *                          Example: raising this from {@code 400} to {@code 700} means a bot with
 *                          M900 cash may stop building instead of spending another M300 round.
 * @param completionWeight score bonus for buying a property that completes a full set.
 *                         Higher value makes the bot chase monopolies more aggressively;
 *                         lower value makes it treat completion as just one factor among others.
 *                         Example: lowering this from {@code 9.0} to {@code 5.0} makes a set-closing
 *                         purchase compete more evenly with liquidity concerns.
 * @param progressWeight score bonus for moving from 0/3 to 1/3 or 1/3 to 2/3 in a set.
 *                       Higher value makes partial set progress matter more;
 *                       lower value pushes the bot toward only fully decisive buys.
 *                       Example: raising this from {@code 3.0} to {@code 5.0} makes a second orange
 *                       look more attractive even if it does not complete the monopoly yet.
 * @param opponentBlockWeight score bonus for denying an opponent a near-complete set.
 *                            Higher value makes denial plays more common;
 *                            lower value makes the bot focus more on its own portfolio.
 *                            Example: raising this from {@code 6.0} to {@code 9.0} makes the bot
 *                            buy a mediocre deed more often if an opponent already owns the rest.
 * @param railroadWeight flat purchase score bonus for railroads.
 *                       Higher value makes railroads compare better against ordinary streets;
 *                       lower value makes the bot pass more of them unless cash is abundant.
 *                       Example: lowering this from {@code 2.5} to {@code 1.0} would reduce
 *                       railroad buying outside clear synergy situations.
 * @param utilityWeight flat purchase score bonus for utilities.
 *                      Higher value makes utilities acceptable more often;
 *                      lower value makes them close to filler assets.
 *                      Example: raising this from {@code 0.5} to {@code 2.0} would noticeably
 *                      increase utility acceptance in the mid-game.
 * @param liquidityPenaltyWeight penalty multiplier for ending up below the desired reserve.
 *                               Higher value makes the bot strongly avoid cash-thin buys;
 *                               lower value lets it take more balance-sheet risk.
 *                               Example: raising this from {@code 3.0} to {@code 5.0} makes a
 *                               purchase that leaves only M50 over reserve look much worse.
 * @param buyToBlockOpponent whether opponent-blocking score is enabled at all.
 *                           {@code true} allows denial buys; {@code false} makes the bot evaluate
 *                           purchases only for its own direct upside and liquidity.
 * @param prioritizeThreeHouses whether building logic prefers reaching the strong three-house phase
 *                              before pushing beyond it.
 *                              {@code true} keeps the classic Monopoly bias toward broad 3-house
 *                              pressure; {@code false} makes build choices more rent/cost driven.
 * @param preferJailLateGame whether jail logic should favor staying in jail during dangerous late-game
 *                           states instead of always paying to get out.
 *                           {@code true} makes the bot avoid high-rent boards more often;
 *                           {@code false} makes it play more actively even when the board is lethal.
 * @param houseBuildAggression multiplier for development score when considering house building.
 *                             Higher value makes the bot convert monopolies into houses sooner;
 *                             lower value makes it preserve cash and delay development.
 *                             Example: raising this from {@code 1.0} to {@code 1.4} makes a decent
 *                             orange-set build look compelling earlier in the mid-game.
 * @param hotelAversion penalty applied when a build round would push a set into hotel territory.
 *                      Higher value slows down hotel upgrades and keeps the bot at strong house levels longer;
 *                      lower value makes hotel conversion more acceptable.
 *                      Example: raising this from {@code 4.0} to {@code 8.0} makes a 4-house set less likely
 *                      to upgrade immediately even if cash is available.
 * @param developmentBias bonus for actions that improve already-owned monopolies or near-monopolies.
 *                        Higher value pushes cash toward deepening strong sets; lower value keeps the bot more
 *                        open to broad expansion and safer balance-sheet play.
 *                        Example: raising this from {@code 2.0} to {@code 5.0} makes orange/red build and
 *                        unmortgage actions outrank side-grade property buys more often.
 * @param mortgageTolerance factor that reduces effective reserve pressure for risk-tolerant play.
 *                          Higher value allows thinner cash positions and more mortgage-prone states;
 *                          lower value keeps the bot conservative.
 *                          Example: raising this from {@code 0.15} to {@code 0.35} means a dangerous board still
 *                          demands reserve, but not the full raw amount from the reserve policy.
 * @param unmortgageAggression multiplier for unmortgage score.
 *                             Higher value reactivates mortgaged assets sooner; lower value hoards cash longer.
 *                             Example: raising this from {@code 1.0} to {@code 1.5} makes completed-set unmortgages
 *                             happen earlier after a cash windfall.
 * @param buildReservePerOpponentMonopoly extra reserve added for each threatening opponent monopoly on the board.
 *                                        Higher value makes the bot spend more defensively when opponents have sets;
 *                                        lower value makes it keep building through danger.
 *                                        Example: with value {@code 80}, two opponent monopolies add M160 extra reserve.
 * @param auctionAggression multiplier for strong-bot auction valuation.
 *                          Higher value bids closer to strategic ceiling; lower value drops out earlier.
 *                          Example: raising this from {@code 1.0} to {@code 1.2} lets a strong bot stretch further
 *                          for a set-completing railroad or color-group piece.
 * @param tradeFairnessTolerance points of unfairness the bot is willing to tolerate for strategic upside.
 *                               Higher value accepts more uneven trades; lower value demands cleaner value parity.
 *                               Example: raising this from {@code 15} to {@code 40} makes the bot accept a modestly
 *                               losing trade if it meaningfully improves position.
 * @param tradeSetCompletionWeight bonus applied when a trade completes or breaks a set.
 *                                 Higher value makes monopoly-shaping trades much more important; lower value keeps
 *                                 raw cash/liquidation value dominant.
 *                                 Example: raising this from {@code 220} to {@code 320} makes set-completing trades
 *                                 far more likely to be accepted despite small cash imbalance.
 * @param colorGroupWeights per-group value multipliers for build, buy, trade, unmortgage, and auction evaluation.
 *                          Higher value prioritizes that group more, lower value de-emphasizes it.
 *                          Example: setting {@code ORANGE -> 1.2} and {@code UTILITY -> 0.8} pushes the bot toward
 *                          stronger classic monopoly groups and away from marginal utility plays.
 * @param jailExitThreshold danger threshold above which the bot prefers staying in jail in late-game states.
 *                          Higher value means the bot leaves jail more often; lower value means it stays jailed more easily.
 *                          Example: lowering this from {@code 500} to {@code 350} makes the bot camp in jail sooner
 *                          once the board becomes hostile.
 * @param bankruptcyAversion multiplier for how hard the bot tries to preserve itself before sacrificing strong assets.
 *                           Higher value means it is more willing to liquidate premium assets to survive;
 *                           lower value means it preserves premium sets longer even at bankruptcy risk.
 *                           Example: raising this from {@code 1.0} to {@code 1.5} makes the bot mortgage or strip
 *                           stronger groups earlier when the alternative is likely elimination.
 * @param railroadCompletionWeight extra bonus for gaining additional railroad synergy.
 *                                 Higher value makes the 2nd/3rd/4th railroad much more attractive;
 *                                 lower value makes railroads behave more like plain cash assets.
 *                                 Example: raising this from {@code 30} to {@code 70} makes a bot fight much harder
 *                                 for railroad #3 or #4.
 * @param utilityCompletionWeight extra bonus for gaining additional utility synergy.
 *                                Higher value makes dual-utility ownership matter more;
 *                                lower value keeps utilities as weak fillers.
 *                                Example: raising this from {@code 20} to {@code 50} makes acquiring the second utility
 *                                noticeably more attractive.
 * @param buildRoundCap highest building level the bot is willing to push toward before stopping voluntary development.
 *                      Lower value keeps the bot around houses, higher value allows freer hotel progression.
 *                      Example: lowering this from {@code 5} to {@code 3} makes the bot strongly prefer stopping around
 *                      the classic three-house phase.
 * @param postMonopolyCashBuffer extra reserve kept once the bot already owns at least one monopoly.
 *                               Higher value slows down snowball spending after a monopoly is formed;
 *                               lower value lets the bot press advantage harder.
 *                               Example: raising this from {@code 125} to {@code 250} keeps much more cash back after
 *                               a monopoly is online.
 * @param auctionSetCompletionBonus extra valuation bonus in auctions when the deed would complete a set.
 *                                  Higher value makes set-completing auction bids far more aggressive;
 *                                  lower value keeps auction play price-disciplined.
 *                                  Example: raising this from {@code 90} to {@code 180} means a set-closing deed
 *                                  can justify bidding well above face liquidation value.
 * @param tradeLiquidityWeight multiplier for the value of cash inside trade evaluation.
 *                             Higher value makes cash-in-hand matter more; lower value emphasizes structural assets.
 *                             Example: raising this from {@code 1.0} to {@code 1.3} makes the bot demand more money
 *                             in exchange for giving up useful deeds.
 * @param opponentLeaderPressure multiplier for denial/pressure logic against the leading opponent.
 *                               Higher value makes the bot more willing to block, overbid, or accept slight inefficiency
 *                               to slow the leader; lower value keeps play more self-focused.
 *                               Example: raising this from {@code 1.0} to {@code 1.4} makes leader-denial purchases and
 *                               auctions happen more often.
 * @param jailCardHoldBias bonus toward staying in jail and keeping the card when the board is dangerous.
 *                         Higher value makes the bot hoard the card more often; lower value spends it more freely.
 *                         Example: raising this from {@code 1.0} to {@code 3.0} makes the bot often save the card for a
 *                         nastier future spot instead of consuming it immediately.
 * @param mortgageRecoveryPriority multiplier for how strongly the bot prefers reactivating mortgaged high-quality assets.
 *                                 Higher value makes key mortgaged sets come back online sooner;
 *                                 lower value makes the bot sit on cash longer before recovery.
 *                                 Example: raising this from {@code 1.0} to {@code 1.4} makes completed-set unmortgaging
 *                                 outrank marginal late-turn passivity more often.
 */
public record StrongBotConfig(
        double buyThreshold,
        int minCashReserve,
        int dangerCashReserve,
        double completionWeight,
        double progressWeight,
        double opponentBlockWeight,
        double railroadWeight,
        double utilityWeight,
        double liquidityPenaltyWeight,
        boolean buyToBlockOpponent,
        boolean prioritizeThreeHouses,
        boolean preferJailLateGame,
        double houseBuildAggression,
        double hotelAversion,
        double developmentBias,
        double mortgageTolerance,
        double unmortgageAggression,
        int buildReservePerOpponentMonopoly,
        double auctionAggression,
        int tradeFairnessTolerance,
        int tradeSetCompletionWeight,
        Map<StreetType, Double> colorGroupWeights,
        int jailExitThreshold,
        double bankruptcyAversion,
        int railroadCompletionWeight,
        int utilityCompletionWeight,
        int buildRoundCap,
        int postMonopolyCashBuffer,
        int auctionSetCompletionBonus,
        double tradeLiquidityWeight,
        double opponentLeaderPressure,
        double jailCardHoldBias,
        double mortgageRecoveryPriority
) {
    public static StrongBotConfig defaults() {
        return new StrongBotConfig(
                6.5,
                250,
                400,
                9.0,
                3.0,
                6.0,
                2.5,
                0.5,
                3.0,
                true,
                true,
                true,
                1.0,
                4.0,
                2.0,
                0.15,
                1.0,
                80,
                1.0,
                15,
                220,
                defaultColorGroupWeights(),
                500,
                1.0,
                30,
                20,
                5,
                125,
                90,
                1.0,
                1.0,
                1.0,
                1.0
        );
    }

    public StrongBotConfig {
        colorGroupWeights = Map.copyOf(colorGroupWeights);
    }

    public double colorGroupWeight(StreetType streetType) {
        return colorGroupWeights.getOrDefault(streetType, 1.0);
    }

    private static Map<StreetType, Double> defaultColorGroupWeights() {
        EnumMap<StreetType, Double> weights = new EnumMap<>(StreetType.class);
        weights.put(StreetType.BROWN, 0.95);
        weights.put(StreetType.LIGHT_BLUE, 1.0);
        weights.put(StreetType.PURPLE, 1.0);
        weights.put(StreetType.ORANGE, 1.2);
        weights.put(StreetType.RED, 1.15);
        weights.put(StreetType.YELLOW, 1.05);
        weights.put(StreetType.GREEN, 0.95);
        weights.put(StreetType.DARK_BLUE, 1.05);
        weights.put(StreetType.RAILROAD, 1.1);
        weights.put(StreetType.UTILITY, 0.8);
        return Map.copyOf(weights);
    }
}
