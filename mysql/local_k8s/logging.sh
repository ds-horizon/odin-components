#!/bin/bash

# Function to set up error handling and signal trapping
setup_error_handling() {
    set -euo pipefail
    trap 'wait; exit 1' SIGTERM SIGINT
}

# Function to log info messages with timestamp
log_with_timestamp() {
    awk '{
          cmd = "date +\"%Y-%m-%d %H:%M:%S,%3N\""
          cmd | getline timestamp
          close(cmd)
          if (tolower($0) ~ /::error::/) {
              print "::error::" timestamp " " $0
          } else {
              print "::info::" timestamp " " $0
          }
          fflush()
      }'
}

# Function to log info messages with timestamp skipping already processed error logs
log_with_timestamp_skip_error() {
    awk '{
          cmd = "date +\"%Y-%m-%d %H:%M:%S,%3N\""
          cmd | getline timestamp
          close(cmd)
          if (tolower($0) ~ /::error::/) {
              print $0
          } else {
              print "::info::" timestamp " " $0
          }
          fflush()
      }'
}

# Function to log error messages with timestamp
log_errors_with_timestamp() {
    awk '{
         cmd = "date +\"%Y-%m-%d %H:%M:%S,%3N\""
         cmd | getline timestamp
         close(cmd)
         print "::error::" timestamp " " $0
         fflush()
     }'
}

# Export functions so they can be used in other scripts
export -f setup_error_handling
export -f log_with_timestamp
export -f log_with_timestamp_skip_error
export -f log_errors_with_timestamp
