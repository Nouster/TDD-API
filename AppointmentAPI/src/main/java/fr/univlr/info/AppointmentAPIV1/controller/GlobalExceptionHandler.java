package fr.univlr.info.AppointmentAPIV1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Classe de gestion globale des exceptions.
 *
 * Cette classe intercepte toutes les exceptions levées par les contrôleurs,
 * et renvoie des réponses HTTP appropriées au client, avec des statuts
 * et des messages personnalisés.
 *
 * Elle me permet d'éviter que les exceptions non capturées ne provoquent des erreurs serveur (500),
 * et assure une meilleure séparation des responsabilités (un des principes SOLID avec le single responsible).
 *
 * Annotée avec @ControllerAdvice, elle agit sur l'ensemble de l'application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DoctorNotFoundException.class)
    public ResponseEntity<String> handleDoctorNotFound(DoctorNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<String> handleAppointmentNotFound(AppointmentNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
}
