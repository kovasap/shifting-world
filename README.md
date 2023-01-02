# Terraforming Catan!

## Design

A worker placement game where workers are all placed on a common game board.
Workers placed on empty tiles can develop them.
In future turns workers placed on developments can take the actions that
development allows.

Whenever an opponent uses a development that you built, they need to pay you a
tax (in resources).

### Stuff to Add

 - Production Chains.
   Buildings build next to each other can "chain" resources, converting raw
   resources into refined ones.
 - Make it so that when you build a library a new deck of cards is created for
   that library specifically.
   You as the builder get to look at (and reorder?) the top 3 cards (or the
   whole deck?).
 - Add a deck of global effects (weather?) from which a new card is flipped each
   round.
   Each card has an effect that changes parameters across the whole game.

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
