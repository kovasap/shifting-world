# Microscope: A Wormbot Data Explorer

## Setup

First, install dependencies:

    # Linuxbrew and clojure
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    brew install clojure/tools/clojure

    yarn

    # Image server dependency
    sudo apt install libvips-dev
    npm i

Then, start up all the servers with:

    ./run.zsh

### Individual Server Startup

You can start the frontend service with:

    clj -M:frontend

You find the application at http://localhost:8700.

Start the backend API with this alias:

    clj -M:api

Find the backend server's documentation at: http://localhost:3000

Start the image server with:

    npx simple-image-server ./data

TODO find a way to run this with `yarn` so we aren't using `npm` and `yarn`.

## Notes

### Wormlist.txt

Each row is a single worm.

x,y,age of death in frames,id of animal,day of death (rounded),minutes alive
