# Répartition des tâches — Projet Akka/Scala/Réseaux de Pétri
## Application : Gestion d'une école

---

## 👤 Équipe 1 — Théorie & Vérification formelle (1 personne)

> **Rôle** : Garant formel du projet. Son travail sert de référence aux deux autres équipes.
> ⚠️ Cette équipe doit commencer **en premier** pour que les invariants soient disponibles avant le développement.

### Tâches

- [ ] Rédiger l'état de l'art sur la **vérification formelle** pour les systèmes critiques
- [ ] Rédiger l'état de l'art sur la **modélisation par réseaux de Pétri** (propriétés structurelles, invariants, représentation des systèmes distribués)
- [ ] Constituer la **bibliographie de référence** du projet
- [ ] Formaliser les **invariants métier** de l'application école, par exemple :
  - Un étudiant ne peut pas s'inscrire à un cours sans être admis
  - Une salle ne peut pas dépasser sa capacité maximale
  - Une note ne peut être saisie que si l'étudiant est inscrit au cours
- [ ] Formaliser les **propriétés LTL** (sûreté et vivacité) de l'application
- [ ] Développer l'**analyseur de propriétés structurelles** du réseau de Pétri :
  - Détection de deadlocks
  - Calcul des p-invariants et t-invariants
- [ ] Rédiger le **rapport de vérification** final des propriétés structurelles et invariants

---

## 👥 Équipe 2 — Conception & Modélisation Pétri (2 personnes)

> **Rôle** : Architectes du système. Ils font le pont entre le métier et le modèle formel.

### Tâches

- [ ] Définir l'**architecture complète des acteurs Akka** :
  - Liste des acteurs (ex. : EtudiantActor, CoursActor, InscriptionActor, NotesActor…)
  - Hiérarchie de supervision
  - Protocoles de communication (messages échangés)
- [ ] Identifier et documenter les **flux de messages critiques** et les interactions entre acteurs
- [ ] Construire le **réseau de Pétri** à partir de l'architecture :
  - Définition des places, transitions et marquage initial
  - Capture de tous les chemins de communication et états possibles
- [ ] Générer et explorer l'**espace d'états** du réseau de Pétri
- [ ] Travailler en coordination avec l'Équipe 3 pour produire la **simulation comparée** (comportement réel vs modèle formel)

---

## 👥 Équipe 3 — Implémentation Scala/Akka & Tests (2 personnes)

> **Rôle** : Développeurs. Ils implémentent ce que l'Équipe 2 a conçu, en respectant les invariants définis par l'Équipe 1.

### Tâches

- [ ] Mettre en place le **projet Scala/Akka** (structure, dépendances, dépôt GitHub)
- [ ] Implémenter les **acteurs Akka** selon l'architecture définie par l'Équipe 2 :
  - Définition des acteurs et protocoles de communication
  - Gestion de la concurrence
  - Supervision et tolérance aux pannes
- [ ] Respecter les **invariants métier** définis par l'Équipe 1 dans le code
- [ ] Écrire les **tests unitaires** des acteurs et des flux critiques
- [ ] Réaliser les **tests de simulation** pour observer le comportement réel du système
- [ ] Fournir les résultats de simulation à l'Équipe 2 pour la comparaison avec le modèle Pétri
- [ ] Gérer le **dépôt GitHub** : structure claire, README complet

---

## 📅 Planning suggéré

| Phase | Équipe 1 | Équipe 2 | Équipe 3 |
|---|---|---|---|
| **Semaine 1-2** | Bibliographie + invariants LTL | Architecture acteurs + messages | Setup projet Scala/Akka |
| **Semaine 3-4** | Analyseur Pétri | Construction réseau de Pétri | Implémentation acteurs principaux |
| **Semaine 5-6** | Vérification formelle | Exploration espace d'états | Tests + simulation |
| **Semaine 7-8** | Rapport de vérification | Simulation comparée | Finalisation + GitHub |

---

## 📦 Livrables par équipe

| Livrable | Équipe responsable |
|---|---|
| Bibliographie & état de l'art | Équipe 1 |
| Formalisation LTL + invariants métier | Équipe 1 |
| Analyseur de propriétés structurelles | Équipe 1 |
| Rapport de vérification | Équipe 1 |
| Architecture des acteurs Akka (diagramme + doc) | Équipe 2 |
| Réseau de Pétri de l'application | Équipe 2 |
| Exploration de l'espace d'états | Équipe 2 |
| Simulation comparée (réel vs formel) | Équipe 2 + 3 |
| Application Scala/Akka fonctionnelle | Équipe 3 |
| Tests unitaires et de simulation | Équipe 3 |
| Dépôt GitHub | Équipe 3 |
