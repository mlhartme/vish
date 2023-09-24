#!/bin/sh
set -e
# user https://software.clapper.org/daemonize/ to start vault in background

var=$HOME
pidfile=$var/vault.pid
logfile=$var/vault.log
if [ -f $pidfile ] ; then
  echo "vault is already running"
  exit 1
fi
daemonize -o $logfile -e $logfile -p $pidfile `which vault` server -dev -dev-root-token-id=root
echo "root">$HOME/.vault-token
sleep 2
vault kv put secret/folder a=1 b=2
echo "vault started"