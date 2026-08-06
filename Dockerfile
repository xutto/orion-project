## Usa una imagen base de Java
#FROM openjdk:21-jdk
#
## Define el directorio de trabajo dentro del contenedor
#WORKDIR /app
#
## Copia el archivo JAR de la aplicación al contenedor
#COPY target/orion-application-1.0.0-SNAPSHOT.jar /app/orion-application.jar
#
## Copia el archivo de configuración `application.yml` al contenedor
#COPY src/main/resources/application.yml /app/application.yml
#
## Configura la variable de entorno por defecto para la opción de la UI
#ENV ENABLED_UI=false
#ENV ORION_P2P_LISTEN=""
#
## Expone el puerto que será especificado dinámicamente
#EXPOSE 5000
#
## Comando para arrancar la aplicación con los valores configurados
#CMD java -Dorion.ui=$ENABLED_UI -Dorion.p2p.listen=$ORION_P2P_LISTEN -jar /app/orion-application.jar

# Usa la imagen base de temurin (o similar)
#FROM eclipse-temurin:21-jdk
FROM openjdk:21-jdk-slim

WORKDIR /app

RUN mkdir -p /usr/local/app/ORION-FILES/share

COPY files/* /usr/local/app/ORION-FILES/share/

COPY target/orion-application-1.0.0-SNAPSHOT.jar /app/orion-application.jar

#COPY src/main/resources/application.yml /app/application.yml

#ENV ENABLED_UI=false
#ENV ORION_P2P_LISTEN="5001"
#ENV JAVAFX_HEADLESS="TRUE"
#ENV USERPROFILE=/usr/local/app
EXPOSE 8080

# Instala Xvfb para framebuffer virtual y otras dependencias mínimas
#RUN apt-get update && apt-get install -y \
#    xvfb \
#    libgl1-mesa-glx \
#    libxrender1 \
#    libxtst6 \
#    libxi6 \
#    && rm -rf /var/lib/apt/lists/*

RUN apt-get update && apt-get install -y \
    xvfb \
    libgl1-mesa-glx \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libfreetype6 \
    && apt-get clean \

# Crea un buffer virtual para correr aplicaciones gráficas
ENV DISPLAY=:99
RUN Xvfb :99 -screen 0 1920x1080x16 &


# Crear un script de entrada
#COPY start.sh /start.sh
#RUN chmod +x /start.sh

# Usa Xvfb para ejecutar JavaFX
#ENTRYPOINT ["/start.sh"]

# NO FUNCIONA , PORQUE JAVAFX NECESITA ENTORNO GRAFICO



