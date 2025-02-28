#!/bin/sh

# Use this script as forced command of an authorized SSH key:
# command="/home/helma/extras/deploy.sh" ssh-ed25519 AAAAC3NzaC…

# Define HELMA_HOST and ANTVILLE_HOST in this file
# shellcheck source=/dev/null
. "$HOME"/deploy.env

case "$SSH_ORIGINAL_COMMAND" in
  ping)
    echo pong
    ;;

  deploy-helma)
    rsync ./ "$HELMA_HOST":./ \
      --archive --compress --delete --verbose \
      --filter '+ /bin' \
      --filter '+ /extras' \
      --filter '+ /launcher.jar' \
      --filter '- /lib/ext' \
      --filter '+ /lib' \
      --filter '+ /modules' \
      --filter '- /*'
    printf 'Restarting Helma on HELMA_host… '
    ssh "$HELMA_HOST" sudo /bin/systemctl restart helma
    ;;

  deploy-antville)
    rsync ./apps/antville/ "$ANTVILLE_HOST":./apps/antville/ \
      --archive --compress --delete --verbose \
      --filter '+ /claustra' \
      --filter '+ /code' \
      --filter '+ /compat' \
      --filter '+ /i18n' \
      --filter '+ /lib' \
      --filter '- /*'
    rsync ./apps/antville/static/helma/ "$ANTVILLE_HOST":./apps/antville/static/helma/ \
      --archive --compress --verbose \
      --filter '+ /fonts' \
      --filter '+ /formica.html' \
      --filter '+ /img' \
      --filter '+ /scripts' \
      --filter '+ /styles' \
      --filter '- /*'
    printf 'Restarting Helma on ANTVILLE_host… '
    ssh "$ANTVILLE_HOST" sudo /bin/systemctl restart helma
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
