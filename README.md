# Terraforming Catan!

## Design

A worker placement game where workers are all placed on a common game board.
Workers placed on empty tiles can develop them.
In future turns workers placed on developments can take the actions that
development allows.

Whenever an opponent uses a development that you built, they need to pay you a
tax (in resources).

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
