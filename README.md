# Terraforming Catan!

## Game Flow

### Phase 1: Drafting blueprints

Each player draws 5 blueprint cards, each of which suggests a building type they
can build.
Then these are drafted around the table, with the last card in the draft removed
from the game (so everyone ends up with 4 blueprints).

This still needs to be implemented.

### Phase 2: Gameplay

Players take turns placing a worker on the board, either spending resources to
build a blueprint or taking the action a currently built building allows them to
take.
If any resources are accumulated on the tile with the worker, the player takes
them into their supply.
Doing this on an opponents tile means you will pay a tax of X% of the resources
on the tile to them.

You can also place your worker on an order to claim if, if you meet the
requirements and can pay for it.

Once everyone has placed all their workers, this rounds ends, all tiles
accumulate resources, and a new round starts.

### Phase 3: Game End

Once a player has completed 3 orders, the game ends.

### Stuff to Add

 - A tile selection system to use for all selection, use this for the
   terraformer tile selection.
 - A pretty background
 - Make tax a percent of the resources instead of a flat value.
 - Add a deck of global effects (weather?) from which a new card is flipped each
   round.
   Each card has an effect that changes parameters across the whole game.

 - Add ability to redirect resources along production chains by investing
   resources in a building.
   the building with the most resources invested will take from buildings first
   when accumulating at the end of a rounds.

## Setup

First, install dependencies:

    # Linuxbrew and clojure
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    brew install clojure/tools/clojure

    yarn

Then, start up all the servers with:

    ./run.zsh

### Individual Server Startup

You can start the frontend service with:

    clj -M:frontend

You find the application at http://localhost:8700.

Start the backend API with this alias:

    clj -M:api

Find the backend server's documentation at: http://localhost:3000
