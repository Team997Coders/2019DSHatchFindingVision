# CameraVision application

This is a sample project based on the [WPILib sample build system](https://github.com/wpilibsuite/VisionBuildSamples) for building Java based vision targeting for running on systems other than the roboRIO.

Before building this project, edit the following settings in the build.gradle file.

```java
// Change the line below if you change the name of your main Java class
mainClassName = 'Main'
// Change the line below to change the name of the output jar
def projectName = 'CameraVision'
// Put in your FRC team number
def team = '997'
// Put in a host address for network tables simulator server or leave blank for roboRio
def nthost = 'localhost'
// Put in a URL for the front web camera
def frontCameraURL = 'http://localhost:1337/mjpeg_stream'
// Put in a URL for the back web camera
def backCameraURL = 'http://localhost:1336/mjpeg_stream'
```

The nthost parameter contains the host (ip address or hostname) of a host running the network tables server.  It will receive the interpreted output from the frames decoded from this app.  If you leave this blank, then the roboRio will automatically try to be detected.  You can pass --noNT and any network tables writing will be bypassed.

The cameraURL parameter can be any IP camera which streams MJPEG images over HTTP.  For more on this protocol, see this [article](https://stackoverflow.com/questions/2060953/httpwebresponse-with-mjpeg-and-multipart-x-mixed-replace-boundary-myboundary).

When you build this project with `gradlew build`, it assume that your target is Windows.  If you want to build for another target, use `gradlew build -x test -Ptarget="<target>"` where target is one of `windows, arm-raspbian, armhf`.

The build will place packaged up binary artifacts in the ./build/distributions directory.

The ./grip subdirectory contains the [GRIP](https://github.com/WPIRoboticsProjects/GRIP) project files used to build the included grip pipelines.

You can run this application with the following command `java -jar CameraVision-all.jar`.  It presents some help on the command line if the parameters are incorrect.

```
The following option is required: [--team | -t]
Usage: CameraVision [options]
  Options:
    --frontcameraurl, -f
      Use specified MJPEG over http streaming source for front camera
      Default: <empty string>
    --backcameraurl, -b
      Use specified MJPEG over http streaming source for back camera
      Default: <empty string>
    --help

    --nont, -n
      Do not call out to network tables to write interpreted values
      Default: false
    --nthost, -h
      NetworkTables server host IP address (usually roborio but could be localhost for testing)
      Default: <empty string>
  * --team, -t
      FIRST team number
      Default: 0
```

## Usage
Once this application is run, it publishes one HTTP endpoints:
`http://localhost:1186` is the image processed image stream in MJPEG over HTTP format

The application also writes image processed interpreted values to network tables:

| Key                                  | Type    | Description                                                                                           |
| ------------------------------------ | ------- | ----------------------------------------------------------------------------------------------------- |
| `Vision\RangeInInches`               | number  | Range to selected target in inches                                                                    |
| `Vision\CameraAngleInDegrees`        | number  | Horizontal angle of camera relative to robot, -90 to 90 degrees, with 0 being perpendicular to front  |
| `Vision\AngleToTargetInDegrees`      | number  | Position of robot relative to target, -90 to 90 degrees, with 0 degrees being perpendicular to target |
| `Vision\NormalizedPointFromCenter\X` | number  | -1 to 1 representing orientation of camera angle to getting target in horizontal center of FOV        |
| `Vision\NormalizedPointFromCenter\Y` | number  | -1 to 1 representing orientation of camera angle to getting target in vertical center of FOV          |

Given that the cameras are not in the center of the robot, the normalized points will have to be adjusted to compensate for that.

Note the this application supports two cameras. On the Pi, USB ports are mapped via udev rules
to the following symlinks:

| Port          | Symlink           | Physical location                 |
| ------------- | ----------------- | --------------------------------- |
| `1-1.4:1.0`   | `/dev/videofront` | Top port farthest from ethernet   |
| `1-1.2:1.0`   | `/dev/videoback`  | Top port closest to ethernet      |

For the Pi3 B+ (rev a020d3), the following udev rules should exist in `/etc/udev/rules.d/99-usb.rules`:
```
KERNEL=="video*", KERNELS=="1-1.3:1.0", SYMLINK+="videofront"
KERNEL=="video*", KERNELS=="1-1.1.2:1.0", SYMLINK+="videoback"
```

For the Pi3 B:
```
KERNEL=="video*", KERNELS=="1-1.4:1.0", SYMLINK+="videofront"
KERNEL=="video*", KERNELS=="1-1.2:1.0", SYMLINK+="videoback"
```
