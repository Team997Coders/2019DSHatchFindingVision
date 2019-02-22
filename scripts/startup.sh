#!/bin/bash
#
# Start up two cameras.
# Front camera streams mjpeg over http to port 1337.
# Back camera streams mjpeg over http to port 1336.
#
~/.local/bin/ntserver &
sleep 3
~/.local/bin/ipcamera -i 9999 -o 1337 &
sleep 3
~/.local/bin/ipcamera -i 9998 -o 1336 &
sleep 3
./stream-camera.sh front 9999 &
sleep 3
./stream-camera.sh back 9998 &
sleep 3
./runCameraVision.sh &
