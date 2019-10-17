#!/bin/bash

if [ -f "/mud/config/startup.lock" ]; then
  exit 0
fi

driver $CONFIG
