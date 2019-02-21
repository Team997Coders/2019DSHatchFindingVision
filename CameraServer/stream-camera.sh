#!/bin/bash
gst-launch-1.0 v4l2src device=/dev/video${1:-0} num-buffers=-1 ! video/x-raw,framerate=15/1,width=640,height=480 ! jpegenc ! queue ! multipartmux boundary=spionisto ! queue leaky=2 ! tcpclientsink host=127.0.0.1 port=${2:-9999}
