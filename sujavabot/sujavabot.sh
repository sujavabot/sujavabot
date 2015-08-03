#!/bin/bash
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

cd $DIR

java -cp sujavabot.jar:lib/* org.sujavabot.Main "$@"
