#!/bin/zsh

print "Go to http://localhost:8700 to view the application!"

# See https://unix.stackexchange.com/a/137503
jobs=()
trap '((#jobs == 0)) || kill $jobs' EXIT HUP TERM INT

clj -M:frontend & jobs+=($!)
clj -M:api & jobs+=($!)
npx simple-image-server ./data & jobs+=($!)

wait
