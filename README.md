# Shifting World

A single player civ building roguelike game.

## Inspirations

 - Terra Mystica
 - Ozymandias
 - dotAge

## Game Flow

### Start

At the beginning of the game, the board will be set up with a randomly generated
hex grid of land types, and several possible starting locations for the player
to choose between.

Season tiles will also be laid out in a random order.
Each season tile contains the following info:

 - A waxing land tile
   - Maybe also a waning land tile

Objective tiles will also be randomly selected.  These could include:

 - Discover a secret of the world by building a temple on a specific tile and
   protecting it for two seasons.
 - Having N (10?) developments at the end of a season.

It is always true that if all player developments are destroyed, the game will
end in a loss.

At this point, the player will be able to choose which faction they want to use
for that game.
Factions have different abilities and different development options.

### Definitions

#### In Reach

A tile is "in reach" if it is directly adjacent to any existing development.

#### In Cluster

A development is "in cluster" with another if they are both connected to each
other by a continuous sequence of directly adjacent developments.

### Developments

All developments are specific to certain land types.
If the land type of a tile a development is on changes to one that it is not
compatible with, it will be destroyed.

All resources in the game accumulate on specific development tiles, and can only
be accessed by all developments "in cluster".

All developments have an "upkeep" cost that must be paid at the end of each
season.
Resources from the current development cluster will automatically be removed to
pay this cost.
This means that if development clusters get cut off from one another, they may
not be able to sustain themselves and one cluster may die off.

### During Each Season...

The player can take these actions any number of times provided they have enough
resources:

 - Build new developments on the proper land tiles "in reach".
 - Transform land tiles "in reach" by spending workers from the current cluster
   based on the transformation wheel.

The player can also use any "once per season" abilities they might have.

### After Each Season...

1. For each land tile of the "waxing" land type, a single adjacent land tile of
   a different type will be transformed into the waxing type.
   This should be done deterministically - perhaps this always happens in a
   certain direction also denoted on the season tile?
1. Upkeep for all developments must be paid.
1. All developments that could not be upkept or are no longer on valid land are
   destroyed.
1. All remaining developments produce resources.

## Feelings to Inspire in Players

 - Each new game should be exciting to start, as you look at the board and try
   to evaluate the best strategy for that specific board state.
 - Games should not be too long - an hour at most.

## Stuff to Add

In order of priority.

## Setup

First, install dependencies:

    # Linuxbrew and clojure
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    brew install clojure/tools/clojure

    yarn

### Individual Server Startup

You can start the frontend service with:

    clj -M:frontend

You find the application at http://localhost:8700.
