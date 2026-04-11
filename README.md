# MonopolyApp

A Java + Processing based Monopoly application.

This project is a desktop board game with its own board, dice, popup flow, deeds, property purchase/rent logic, card
spots, a right-side game info panel, trading, property auctions, computer players, and a gradually refactored turn
system. The codebase has also been made more testable over time, and now includes unit tests plus headless simulation
tests for the main game flow.

## Current Features

- playable Monopoly-style desktop board game
- default three-player setup with 1 human and 2 `STRONG` bots
- multiple players on the same board
- dice rolling, movement, and animations
- pause / resume during live play
- buying properties
- property auctions, including human bid / pass interaction
- paying rent
- debt resolution flow with retry and bankruptcy handling
- bankruptcy asset transfer and bank-side auctions
- tax spots
- jail / go to jail flow
- chance / community chest card spots with localized shuffled decks
- deed view for owned properties
- house buying and selling, including even full-set rounds
- mortgage / unmortgage support
- player-to-player trading with counteroffers and bot trade evaluation
- popup-based decision flow
- right-side sidebar for turn state, selected player info, debt state, and recent message history
- winner / game-over popup flow
- language toggle button for supported locales
- debug controls via God mode popup
- unit tests, smoke tests, and repeated headless bot simulation tests

## Tech Stack

- Java 19
- Processing 4
- ControlP5
- Maven
- JUnit 5
- Lombok

## Running the App

### Requirements

- Java 19
- Maven

### Start the Game

In IntelliJ, the easiest way is to run [
`StartMonopolyApp.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/main/java/StartMonopolyApp.java).

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
- `1` / `2`  
  choose the primary / secondary popup action in many popup flows
- `E`  
  end the turn in debug mode
- `A`  
  toggle animation skipping
- `D`  
  toggle debug mode
- `P`  
  pause / resume
- `T`  
  open trading on your turn
- `H`  
  print help to the console

## Testing

The project includes both regular unit tests and headless simulation tests that drive the live game flow.

Examples:

- [
  `GameSmokeTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/GameSmokeTest.java)
- [
  `GameBotSimulationTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/GameBotSimulationTest.java)
- [
  `GameComputerPlayerTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/GameComputerPlayerTest.java)
- [
  `GameBankruptcyTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/GameBankruptcyTest.java)
- [
  `TurnEngineTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/turn/TurnEngineTest.java)
- [
  `BoardTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/board/BoardTest.java)
- [
  `PathTest.java`](/E:/Documents/ProcessingProjects/MonopolyApp/src/test/java/fi/monopoly/components/board/PathTest.java)

The simulation tests use a headless bootstrap and automate gameplay. Their purpose is to catch regressions where the
game flow breaks, stalls, or gets stuck in popup / animation / debt / auction states.