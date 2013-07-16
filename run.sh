make clean
make setup
make
adb devices | tail -n +2 | cut -sf 1 | xargs -I X adb -s X uninstall org.irl.ccnping
adb devices | tail -n +2 | cut -sf 1 | xargs -I X adb -s X install -r bin/CCNPing-debug.apk 
