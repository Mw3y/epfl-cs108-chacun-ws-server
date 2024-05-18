# Étape 1: Utiliser une image de base avec Gradle et JDK
FROM gradle:jdk21 as builder

# Copier les fichiers de configuration de Gradle
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts /home/gradle/src/
COPY --chown=gradle:gradle gradle /home/gradle/src/gradle

# Copier les sources du projet
COPY --chown=gradle:gradle src /home/gradle/src/src

# Définir le répertoire de travail
WORKDIR /home/gradle/src

# Construire l'application avec Gradle
RUN gradle build --no-daemon

# Étape 2: Créer une image de déploiement légère avec JRE uniquement
FROM openjdk:21-slim

# Copier le jar exécutable depuis l'étape de construction
COPY --from=builder /home/gradle/src/build/libs/*.jar /app/server-websocket.jar

# Exposer le port sur lequel le serveur WebSocket sera accessible
EXPOSE 8080

# Commande pour démarrer l'application
CMD ["java", "--enable-preview", "-jar", "/app/server-websocket.jar"]
