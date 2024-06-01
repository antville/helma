#!/bin/sh

# Use this script as forced command of an authorized SSH key:
# command="/home/helma/extras/deploy.sh" ssh-ed25519 AAAAC3NzaC…

case "$SSH_ORIGINAL_COMMAND" in
  ping)
    echo pong
    ;;

  deploy-helma)
    rsync ./ p3k.org:./ \
      --archive --compress --delete --verbose \
      --filter '+ /bin' \
      --filter '+ /extras' \
      --filter '+ /launcher.jar' \
      --filter '- /lib/ext' \
      --filter '+ /lib' \
      --filter '+ /modules' \
      --filter '- /*'
    ;;

  deploy-antville)
    rsync ./apps/antville/ p3k.org:./apps/antville/ \
      --archive --compress --delete --verbose \
      --filter '+ /claustra' \
      --filter '+ /code' \
      --filter '+ /compat' \
      --filter '+ /i18n' \
      --filter '+ /lib' \
      --filter '- /*'
    rsync ./apps/antville/static/helma/ p3k.org:/var/www/weblogs.at/ \
      --archive --compress --verbose \
      --filter '+ /fonts' \
      --filter '+ /formica.html' \
      --filter '+ /img' \
      --filter '+ /scripts' \
      --filter '+ /styles' \
      --filter '- /*'
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
