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
@Table(name = "Iotreadonly")
public class Iotreadonly {
    @Id
    @GeneratedValue()
    public Long id;

    public String description;

    public String url;

    public String datatype;

    public Long presetid;

}
