# MonopolyApp

A Java + Processing based Monopoly application.

This project is a desktop board game with its own board, dice, popup flow, deeds, property purchase/rent logic, card
spots, a right-side game info panel, and a gradually refactored turn system. The codebase has also been made more
testable over time, and now includes unit tests plus a headless smoke test for the main game flow.

## Current Features

- playable Monopoly-style desktop board game
- default three-player game setup
- multiple players on the same board
- dice rolling, movement, and animations
- buying properties
- paying rent
- debt resolution flow with retry and bankruptcy handling
- tax spots
- jail / go to jail flow
- chance / community chest card spots with localized shuffled decks
- deed view for owned properties
- partial house buying / selling support
- partial mortgage / unmortgage support
- popup-based decision flow
- right-side sidebar for turn state, selected player info, debt state, and popup history
- debug controls for cash, movement, debt, jail, and language switching
- automated smoke test that runs the game headlessly and verifies it does not get stuck

## Tech Stack

- Java 19
- Processing 4
- ControlP5
- Maven
- JUnit 5
- Lombok

## Project Structure

- [`src/main/java/fi/monopoly`](/E:/Documents/ProcessingProjects/MonopolyApp/src/main/java/fi/monopoly)  
  application runtime and bootstrap
- [
  `src/main/java/fi/monopoly/components`](/E:/Documents/ProcessingProjects/MonopolyApp/src/main/java/fi/monopoly/components)  
  core game logic, players, board flow, dice, popups
- [
  `src/main/java/fi/monopoly/components/turn`](/E:/Documents/ProcessingProjects/MonopolyApp/src/main/java/fi/monopoly/components/turn)  
  newer turn engine structure and effect-based direction
- [
  `src/main/java/fi/monopoly/components/spots`](/E:/Documents/ProcessingProjects/MonopolyApp/src/main/java/fi/monopoly/components/spots)  
  board spot behavior
- [
  `src/main/java/fi/monopoly/components/properties`](/E:/Documents/ProcessingProjects/MonopolyApp/src/main/java/fi/monopoly/components/properties)  
  properties, rent, buildings, mortgage logic
- [`src/main/java/fi/monopoly/images`](/E:/Documents/ProcessingProjects/MonopolyApp/src/main/java/fi/monopoly/images)  
  images and rendering objects
- [`src/test/java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java)  
  unit tests and smoke tests

## Running the App

### Requirements

- Java 19
- Maven

### Start the Game

In IntelliJ, the easiest way is to run [
`StartMonopolyApp.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/main/java/StartMonopolyApp.java).

From the command line, you can first compile and run tests with:

```bash
mvn test
```

Then run `StartMonopolyApp` from the IDE or with your own Java run configuration.

Note: this project is currently easiest to run from an IDE because it is a Processing-based desktop app rather than a
packaged release build.

Window resizing is enabled. The app currently keeps a minimum window size so the fixed board and essential sidebar
controls still fit while the layout migration is in progress, but the board itself is not yet fully responsive.

## Controls

These keys are available during the game:

- `Space`  
  roll dice / accept the next flow step depending on the current state
- `Enter`  
  alternative confirmation key in some flows
- `E`  
  end the turn in debug mode
- `A`  
  toggle animation skipping
- `D`  
  toggle debug mode
- `H`  
  print help to the console

## Testing

The project includes both regular unit tests and a smoke test that drives the full game flow.

Examples:

- [
  `GameSmokeTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/GameSmokeTest.java)
- [
  `TurnEngineTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/turn/TurnEngineTest.java)
- [
  `BoardTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/board/BoardTest.java)
- [
  `PathTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/board/PathTest.java)

Run tests with:

```bash
mvn test
```

The smoke test uses a headless bootstrap and simulates gameplay automatically. Its purpose is to catch regressions where
the game flow breaks or gets stuck in popup / animation states.

## Missing / In Progress

See the up-to-date task list in [`TODO.txt`](/E:/Documents/ProcessingProjects/MonopolyApp/TODO.txt).

Some of the biggest unfinished areas are:

- bankruptcy and debt-resolution rule polish
- auctions
- player-to-player trading
- final jail rule behavior
- fuller building rule support
- player setup screen
- full responsive layout for board and sidebar resizing

## Architecture Notes

The project has gone through an incremental refactor where:

- popup and event flow have been moved into services
- turn logic has started moving into a dedicated `turn` layer
- debt handling has been separated into payment and debt-resolution components
- sidebar and popup layout logic has started moving into centralized layout helpers
- part of the older global `MonopolyApp.self` usage has been replaced with a `MonopolyRuntime` structure
- testability has been improved with headless testing and cleaner domain-side logic

The refactor is still in progress, so the codebase currently contains both older and newer patterns side by side.
