package fr.univlr.info.AppointmentAPIV1.store;

import fr.univlr.info.AppointmentAPIV1.controller.AppointmentNotFoundException;
import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment,Long> {
    // Cette méthode me permet de renvoyer une liste de rendez-vous en passant un docteur en argument
    List<Appointment> findByDoctor(String doctor) throws AppointmentNotFoundException;
    // Filter les rendez-vous après la date passée en paramétre
    List<Appointment> findByStartDateAfter(Date date) throws AppointmentNotFoundException;

}
