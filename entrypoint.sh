#!/bin/sh
# Wykorzystujemy exec, aby Java przejęła PID 1 (ważne dla sygnałów zamknięcia)
# Przekazujemy JAVA_OPTS oraz wszystkie argumenty skryptu ($@)
exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher "$@"