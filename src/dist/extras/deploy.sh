#!/bin/sh

# Use this script as forced command of an authorized SSH key:
# command="/home/helma/extras/deploy.sh" ssh-ed25519 AAAAC3NzaC…

case "$SSH_ORIGINAL_COMMAND" in
  ping)
    echo pong
    ;;

  restart)
    printf 'Restarting Helma… '
    sudo /bin/systemctl restart helma
    printf '%s\n' 'done.'
    ;;

  *)
    # Allow any rsync command but restrict it to the installation directory
    rrsync -wo /home/helma
    ;;
esac
