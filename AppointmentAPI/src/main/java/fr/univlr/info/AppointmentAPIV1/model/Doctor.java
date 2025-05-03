package fr.univlr.info.AppointmentAPIV1.model;


import org.springframework.hateoas.EntityModel;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
public class Doctor extends EntityModel<Doctor> {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    @OneToMany(mappedBy = "doctorInfo")
    private List<Appointment> appointments;

    public Doctor() {}

    public Doctor(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }

    @Override
    public String toString() {
        return "Doctor{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", appointments=" + appointments +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Doctor)) return false;
        Doctor doctor = (Doctor) o;
        return Objects.equals(id, doctor.id) && Objects.equals(name, doctor.name) && Objects.equals(appointments, doctor.appointments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, appointments);
    }
}
