package fi.monopoly.support;

import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.properties.Properties;
import fi.monopoly.components.properties.Property;
import javafx.scene.paint.Color;

import java.lang.reflect.Field;

public final class TestObjectFactory {
    private TestObjectFactory() {
    }

    public static Player player(String name, int money, int turnNumber) {
        return new Player(name, Color.BLACK, money, turnNumber);
    }

    public static void giveProperty(Player player, Property property) {
        property.setOwnerPlayer(player);
        getProperties(player).addProperty(property);
    }

    public static Players playersWithTurn(Player turnPlayer, Player... others) {
        Players players = new Players(null);
        for (Player player : playersList(turnPlayer, others)) {
            getPlayerList(players).add(player);
        }
        setField(Players.class, players, "turnNum", turnPlayer.getTurnNumber());
        setField(Players.class, players, "selectedPlayer", turnPlayer);
        return players;
    }

    private static Properties getProperties(Player player) {
        return (Properties) getField(Player.class, player, "ownedProperties");
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<Player> getPlayerList(Players players) {
        return (java.util.List<Player>) getField(Players.class, players, "playerList");
    }

    private static java.util.List<Player> playersList(Player turnPlayer, Player... others) {
        java.util.List<Player> playerList = new java.util.ArrayList<>();
        playerList.add(turnPlayer);
        java.util.Collections.addAll(playerList, others);
        return playerList;
    }

    private static Object getField(Class<?> type, Object target, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Class<?> type, Object target, String name, Object value) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
