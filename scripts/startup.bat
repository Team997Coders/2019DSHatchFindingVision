@echo off
REM
REM Requires Python 3 and gstream to be installed
REM
REM The first parameter is the device index of the camera to grab.
REM Windows assigns them in an unpredictable manner.  A laptop's built in camera
REM is usually device index 0 but if a USB camera is plugged in at boot, it
REM might be camera 0.
REM
if exist ./python/Scripts/activate.bat call ./python/Scripts/activate
start ntserver
timeout 3
start ipcamera
timeout 3
start stream-camera.bat %1
timeout 3
start runCameraVision.bat
if exist ./python/Scripts/deactivate.bat call ./python/Scripts/deactivate
