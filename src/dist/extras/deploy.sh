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

  save\ *)
    set -- $SSH_ORIGINAL_COMMAND
    shift
    branch_name=local-changes
    git switch "$branch_name" 2>/dev/null || git switch --create "$branch_name"
    git add --all
    git diff --cached --quiet || git commit --message "$*"
    ;;

  commit\ *)
    set -- $SSH_ORIGINAL_COMMAND
    shift
    git switch main
    git add --all
    git diff --cached --quiet || git commit --message "$*"
    ;;

  *)
    # Allow any rsync command but restrict it to the installation directory
    rrsync -wo /home/helma
    ;;
esac
