# Terraforming Catan!

## Game Flow

Players take turns placing developments.  The player with the highest score wins!

### Stuff to Add

 - more maps, and random initial settlement/cache placement
 - mess with random development selection ("drafting")
 - make sure developments can "pull" resources even after they are placed (like the port)
 - make monument less good, or more expensive
 - scroll developments separately from the board
 - show a flow chart for how developments chain into each other
 - Make it so orders ARE developments that have specific requirements to be
   placed (like other developments), but the requirements involve a specific
   player having built something.
 - resources don't accumulate?
   you just need buildings that build things to meet requirements?
 - roads to transport resources between tiles that can use them?
   maybe roads are the only way to use someone elses development in your
   production chain
 - Make it so orders are provided by tiles on the map, and all possible orders
   are fixed at the start of the game.
 - Make it so that players have no inventory, all resources on tiles they
   control form their bank.
 - Players can claim tiles (instead of taking resources off them)
 - A tile selection system to use for all selection, use this for the
   terraformer tile selection.
 - A pretty background
 - Make tax a percent of the resources instead of a flat value.
 - Other players can take resources off your tiles only after they reach a certain stack size (and still pay tax)
 - Add a deck of global effects (weather?) from which a new card is flipped each
   round.
   Each card has an effect that changes parameters across the whole game.

 - Add ability to redirect resources along production chains by investing
   resources in a building.
   the building with the most resources invested will take from buildings first
   when accumulating at the end of a rounds.
 - Add better logging: https://github.com/ptaoussanis/sente/issues/416
 - Possibly host on glitch.com

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
