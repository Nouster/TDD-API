package fr.univlr.info.AppointmentAPIV1.controller;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Date;

public class AppointmentDateValidator
        implements ConstraintValidator<AppointmentDateConstraint, Appointment> {
    @Override
    public void initialize(AppointmentDateConstraint dateCst) {
    }

    @Override
    public boolean isValid(Appointment appointment, ConstraintValidatorContext ctxt) {
        if (appointment == null) {
            return true; // Si l'objet est null, on laisse la validation se faire ailleurs
        }

        // Ici grâce aux getter de mon objet appointment, je récupère la date dans le but de controler ce qu'il contient
        Date startDate = appointment.getStartDate();
        Date endDate = appointment.getEndDate();

        // Je vérifie que l'une des deux dates n'est pas null
        if (startDate == null || endDate == null) {
            ctxt.disableDefaultConstraintViolation();
            ctxt.buildConstraintViolationWithTemplate("Start date or end date cannot be null")
                    .addConstraintViolation();
            return false;
        }

        // Je m'assure que la date de début est bien avant la date de fin
        if (startDate.after(endDate)) {
            return false;
        }

        // Je vérifie que les deux dates sont dans le futur par rapport à la date actuelle
        Date currentDate = new Date();
        if (startDate.before(currentDate) || endDate.before(currentDate)) {
            return false; // Parce que l'une des dates est dans le passé
        }

        return true;
    }

}
