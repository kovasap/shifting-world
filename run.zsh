#!/bin/zsh

print "Go to http://localhost:3000 to view the application!"

# See https://unix.stackexchange.com/a/137503
jobs=()
trap '((#jobs == 0)) || kill $jobs' EXIT HUP TERM INT

# raspberrypi doesn't have enough memory to run both servers at once.
if [[ $(hostname) != *raspberrypi* ]]; then
  clj -M:frontend & jobs+=($!)
fi
clj -M:api & jobs+=($!)

wait
