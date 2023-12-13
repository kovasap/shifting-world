# Terraforming Catan!

## Game Flow

Players take turns placing developments.
The player with the highest score at the end wins!

## Feelings to Inspire in Players

 - Each new game should be exciting to start, as you look at the board and try
   to evaluate the best strategy for that specific board state.
 - Games should not be too long - players should not feel like they have to keep
   playing a game for a while that they have already lost.

## Stuff to Add

In order of priority.

1. Random map generation that works well given the developments that exist.
   Every game should feel like a new puzzle to solve.
1. show a flow chart for how developments chain into each other somewhere.  Start with https://blog.klipse.tech/visualization/2021/02/16/graph-playground-cytoscape.html.
1. Add a deck of global effects (weather?) from which a set of cards is flipped
   each game.
   Each card has an effect that changes parameters across the whole game.
   This could include the old idea of "orders", which make some
   developments/resources more/less valuable in this game.
1. Possibly add a "protection" effect when a development is placed that prevents
   other players from using it's resources for a turn or two so that it's harder
   for people to steal your resources.
1. A tile selection system to use for all selection, use this for the
   terraformer tile selection.
1. Add better logging: https://github.com/ptaoussanis/sente/issues/416
1. Possibly host on glitch.com

## Setup

First, install dependencies:

    # Linuxbrew and clojure
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    brew install clojure/tools/clojure

    yarn

Then, start up all the servers with:

    ./run.zsh

You might need to open ports 3000, 5000, and 9630 to connect from another
machine.
On linux with UFW you can do this with:

```
sudo ufw allow 3000
sudo ufw allow 5000
sudo ufw allow 9630
```

### On Raspberry Pi

```
curl -sL https://deb.nodesource.com/setup_18.x | sudo bash -
sudo apt-get install nodejs
curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -
sudo apt install yarn
curl -O https://download.clojure.org/install/linux-install-1.11.1.1273.sh
chmod +x linux-install-1.11.1.1273.sh
sudo ./linux-install-1.11.1.1273.sh
sudo cp cljs-board-game.service /etc/systemd/system/
systemctl enable cljs-board-game.service
systemctl start cljs-board-game.service
# To read logs:
journalctl -u cljs-board-game.service
```

Set up port forwarding on router to forward ports 3000, 5000, 9630 to the
raspberry pi's IP address.

Set up duckdns to point to the IP at https://www.whatismyip.com/.

Now anyone can access the game at kovas.duckdns.org:3000!

See info about setting up a static domain name at
https://gist.github.com/taichikuji/6f4183c0af1f4a29e345b60910666468.

### Individual Server Startup

You can start the frontend service with:

    clj -M:frontend

You find the application at http://localhost:8700.

Start the backend API with this alias:

    clj -M:api

Find the backend server's documentation at: http://localhost:3000
