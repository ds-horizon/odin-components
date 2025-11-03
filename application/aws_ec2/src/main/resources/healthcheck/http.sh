# shellcheck disable=SC2148
MAX_ATTEMPTS=24
DELAY_INTERVAL=5

attempts=1
while [[ $attempts -le $MAX_ATTEMPTS ]]; do
  status=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:${port?c}${path})
  if [[ $status -ne 200 ]]; then
    echo "Status code:$status. Sleeping for $DELAY_INTERVAL seconds before making next attempts"
    attempts=$attempts+1
    sleep $DELAY_INTERVAL
  else
    echo "Healthcheck successful"
    exit 0
  fi
done

echo "Max attempts:[$MAX_ATTEMPTS] exceeded while waiting for healthcheck to be successful" >&2
exit 1
