#!/bin/bash

LOG_FILE="/tmp/bootstrap.log".$(date +%Y%m%d)
touch $LOG_FILE

#######################################################################
# Function to write to the Log file
#######################################################################
write_log() {
  while read text; do
    LOGTIME=$(date "+%Y-%m-%d %H:%M:%S")
    if [ ! -f $LOG_FILE ]; then
      echo "ERROR!! $LOG_FILE not found. Exiting."
      exit 1
    fi
    echo $LOGTIME": $text" | tee -a $LOG_FILE
  done
}

{
# Start time
start_time=$(date +%s)
echo "Userdata Start Time: $start_time"

sudo setenforce 0;
ulimit -n 65000
echo "ntp and chrony operations"
service ntpd stop
systemctl disable chronyd
sudo ntpd -gq
timedatectl set-ntp true
systemctl restart systemd-timedated
systemctl start ntpd
sudo ntpdate -u 169.254.169.123
systemctl restart nscd

# Execute pre start userdata patch
echo "Executing pre start userdata patch"
${pre_start_userdata}
echo "Done executing pre start userdata patch"

# Start application

# Create env variable file
mkdir -p /etc/odin/${component_name}
ENV_FILE=/etc/odin/${component_name}/.component.env
cat << EOM > $ENV_FILE
<#list environment_variables as key, value>
${key}=${value}
</#list>
PID_PATH=/run/${component_name}.pid
EOM

# Create a systemd service
SYSTEMD_SERVICE_FILE=/etc/systemd/system/${component_name}.service
cat << EOM > $SYSTEMD_SERVICE_FILE
[Unit]
Description=${component_name}
After=network.target

[Service]
Type=forking
Restart=no
TimeoutStartSec=0
User=root
Group=root
EnvironmentFile=$ENV_FILE
ExecStart=/usr/bin/bash -c "bash ${application_directory}/${start_script_path}"
TimeoutStopSec=5

[Install]
WantedBy=multi-user.target
EOM

# Start application
echo "Starting service now..."
systemctl start ${component_name} --no-block
echo "Check the logs using 'journalctl -u ${component_name} --no-pager'"

# Execute post start userdata patch
echo "Executing post start userdata patch"
${post_start_userdata}
echo "Done executing post start userdata patch"

# End time
end_time=$(date +%s)

# Calculate the difference in seconds
time_difference=$((end_time - start_time))

# Convert seconds to hours, minutes, and seconds
hours=$((time_difference / 3600))
minutes=$(((time_difference % 3600) / 60))
seconds=$((time_difference % 60))

echo "Userdata end time: $end_time"
# Print the difference
echo "Time taken: $hours hours, $minutes minutes, $seconds seconds"
} | write_log
