#!/bin/sh
set -e
# user https://software.clapper.org/daemonize/ to start vault in background

var=$HOME
pidfile=$var/vault.pid
logfile=$var/vault.log
if [ ! -f $pidfile ] ; then
  echo "vault is not running"
  exit 1
fi
kill `cat $pidfile`
rm $pidfile
echo "vault stopped"