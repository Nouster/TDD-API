package fr.univlr.info.AppointmentAPIV1.controller;

public class DoctorNotFoundException extends RuntimeException {
    public DoctorNotFoundException(String name) {
        super("Could not find doctor called : " + name);
    }}
