package fr.univlr.info.AppointmentAPIV1.controller;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.Date;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class AppointmentModelAssembler implements RepresentationModelAssembler<Appointment, EntityModel<Appointment>> {

    @Override
    public EntityModel<Appointment> toModel(Appointment appointment) {
        // On ajoute les liens HAL à l'Appointment
        return EntityModel.of(appointment,
                linkTo(methodOn(AppointmentController.class).getAppointmentById(appointment.getId())).withSelfRel(), // Lien vers la ressource elle-même
                linkTo(methodOn(AppointmentController.class).all(null)).withRel("appointments") // Lien vers la collection des rendez-vous
        );
    }
}
