#!/bin/bash
gst-launch-1.0 videotestsrc pattern=ball ! video/x-raw, framerate=15/1, width=320, height=240 !  jpegenc ! multipartmux boundary=spionisto ! tcpclientsink host=127.0.0.1 port=9999

