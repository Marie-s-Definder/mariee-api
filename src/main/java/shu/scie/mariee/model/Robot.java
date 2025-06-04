package shu.scie.mariee.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Entity
@NoArgsConstructor
@Table(name = "robot")
public class Robot {
    @Id
    @GeneratedValue()
    public Long id;

    public String building;

    public String room;

    public String name;//

    public Long status;

}
