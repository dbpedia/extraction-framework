#!/bin/bash

# quit if an error occurs
set -e

COMMAND=$1
MYHOME=$2
MYDIR=$3
MYDB=$4

# FIXME: check $COMMAND

# TODO: check $MYDB? It may be empty, but should be a well-formed database name.

if [[ -z $MYHOME || -z $MYDIR || ( $COMMAND != "install" && $COMMAND != "start" && $COMMAND != "stop" && $COMMAND != "run" ) ]]
then
    echo "usage: $0 (install|start|stop|run) <mysql home> <mysql dir> <database>"
    echo
    echo "install:"
    echo "Install MySQL databases in <mysql dir>/data."
    echo "Start server listening at default port and socket <mysql dir>/mysql.sock, logging to <mysql dir>/mysql.log."
    echo "Grant all privileges to anonymous user."
    echo
    echo "start:"
    echo "Start MySQL server using databases in <mysql dir>/data, listening on localhost at default port and socket <mysql dir>/mysql.sock, logging to <mysql dir>/mysql.log."
    echo
    echo "stop:"
    echo "Stop MySQL server listening at socket <mysql dir>/mysql.sock."
    echo
    echo "run:"
    echo "Connect to MySQL server listening at socket <mysql dir>/mysql.sock, execute SQL from standard input."
    echo
    echo "<mysql home> must be the absolute path of your MySQL installation directory."
    echo
    echo "<mysql dir> must be an absolute path of an existing directory where MySQL will store its data."
    echo
    echo "<database> only used by run, optional."
    echo
    echo "Example:"
    echo "$0 ~/bin/mysql ~/data/mysql"
    exit 1
fi

# FIXME: check that MYDIR is an absolute path, or better: make it absolute

# We need to be in MySQL install dir because of http://bugs.mysql.com/bug.php?id=34981
cd $MYHOME

case "$COMMAND" in
install)
    ./scripts/mysql_install_db --no-defaults --character-set-server=utf8 --datadir="$MYDIR/data"
    
    ./bin/mysqld_safe --no-defaults --character-set-server=utf8 --socket="$MYDIR/mysql.sock" --bind-address=localhost --datadir="$MYDIR/data" --max_allowed_packet=1G --key_buffer_size=1G --query_cache_size=1G >>"$MYDIR/mysql.log" 2>&1 &
    
    # wait for server to start
    sleep 5
    
    ./bin/mysql --no-defaults --default-character-set=utf8 --socket="$MYDIR/mysql.sock" -u root -e "GRANT ALL ON *.* TO ''@'localhost'" mysql
;;
start)
    ./bin/mysqld_safe --no-defaults --character-set-server=utf8 --socket="$MYDIR/mysql.sock" --bind-address=localhost --datadir="$MYDIR/data" --max_allowed_packet=1G --key_buffer_size=1G --query_cache_size=1G >>"$MYDIR/mysql.log" 2>&1 &
;;
stop)
    ./bin/mysqladmin --no-defaults --default-character-set=utf8 --socket="$MYDIR/mysql.sock" shutdown
;;
run)
    ./bin/mysql --no-defaults --default-character-set=utf8 --socket="$MYDIR/mysql.sock" "$MYDB"
;;
esac

# FIXME: make sure that this is called even if the script fails
cd - >/dev/null
