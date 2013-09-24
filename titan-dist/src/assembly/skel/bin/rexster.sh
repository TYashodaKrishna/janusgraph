#!/bin/bash

BIN="`dirname $0`"
CP="$( echo $BIN/../lib/*.jar . | sed 's/ /:/g')"
CP="$CP:$(find -L $BIN/../ext/ -name "*.jar" | tr '\n' ':')"
export CLASSPATH="$CP"

REXSTER_EXT=../ext

PUBLIC="$BIN"/../public/
FOREGROUND=1
ARGS="-configuration $BIN/../conf/rexster.xml"

while getopts 'ds' option; do
    case $option in
        'd') FOREGROUND=0;;
        's') ARGS="-wr $PUBLIC $ARGS"; ARGS="-$option $ARGS";;
         * ) ARGS="-$option $ARGS";;
    esac
done

# Find Java
if [ "$JAVA_HOME" = "" ] ; then
    JAVA="java -server"
else
    JAVA="$JAVA_HOME/bin/java -server"
fi

# Set Java options
if [ "$JAVA_OPTIONS" = "" ] ; then
    JAVA_OPTIONS="-Xms128m -Xmx512m"
fi

# Let Cassandra have 7199
JAVA_OPTIONS="$JAVA_OPTIONS \
              -Dcom.sun.management.jmxremote.port=7299 \
              -Dcom.sun.management.jmxremote.ssl=false \
              -Dcom.sun.management.jmxremote.authenticate=false"

# Launch the application
if [ 1 -eq $FOREGROUND ]; then
    $JAVA $JAVA_OPTIONS com.tinkerpop.rexster.Application $ARGS
else
    $JAVA $JAVA_OPTIONS com.tinkerpop.rexster.Application $ARGS >"$BIN"/../log/rexster.log 2>&1 &
    disown
fi

# Return the program's exit code
exit $?
