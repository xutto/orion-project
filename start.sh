#!/bin/bash
# Inicia Xvfb (servidor de framebuffer para simular un display gráfico)
Xvfb :1 -screen 0 1024x768x16 &

# Configura la variable de entorno DISPLAY para que JavaFX use el framebuffer
export DISPLAY=:1

# Arranca la aplicación JavaFX
java -Dorion.ui=$ENABLED_UI \
     -Dorion.p2p.listen=$ORION_P2P_LISTEN \
     -Djavafx.headless=$JAVAFX_HEADLESS \
     -jar /app/orion-application.jar
