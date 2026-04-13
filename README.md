# CYFlyBy – Contrôle aérien

## Description

CYFlyBy est une application de simulation de contrôle aérien développée en Scala.  
Elle permet de gérer des vols (décollage, atterrissage) tout en respectant des règles de sécurité (météo, piste, communication, etc.).

L’application propose :
- un backend en Scala (avec acteurs)
- une API HTTP
- une interface web interactive

---

## Fonctionnalités

- Gestion de plusieurs avions
- Décollage et atterrissage
- Conditions météo (bonne, mauvaise, critique)
- Gestion de la piste (libre / occupée)
- Détection d’intrusion et conflits
- Visualisation des avions via un schéma graphique
- Journal des événements en temps réel

---

## Architecture

Le projet est structuré en plusieurs parties :

- `actors/` : logique métier avec les acteurs
- `app/` : serveur HTTP et gestion globale
- `domain/` : règles métier
- `formal/` : modèle de Pétri et vérification
- `resources/public/` : interface web (HTML / CSS / JS)

---

## Lancer le projet

### Prérequis

- Java
- sbt (Scala Build Tool)

### Commandes

A lancer depuis le sbt shell terminal sur Intellij.

```bash
cd CYFlyBy
cd untitled
sbt run
```

Ouvrir sur une page ineternet ce lien :
http://localhost:8080
