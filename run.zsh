#!/bin/zsh

print "Go to http://localhost:3000 to view the application!"
print $(pwd)

# See https://unix.stackexchange.com/a/137503
jobs=()
trap '((#jobs == 0)) || kill $jobs' EXIT HUP TERM INT

# raspberrypi doesn't have enough memory to run both servers at once.
# This is for development only anyway
if [[ $(hostname) != *raspberrypi* ]]; then
  clj -M:frontend & jobs+=($!)
fi
# This is for actually running the server in non-dev mode.
clj -M:frontend-release
clj -M:api & jobs+=($!)

wait
