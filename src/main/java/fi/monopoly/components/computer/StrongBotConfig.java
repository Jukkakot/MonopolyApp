package fi.monopoly.components.computer;

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
        boolean preferJailLateGame
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
                true
        );
    }
}
