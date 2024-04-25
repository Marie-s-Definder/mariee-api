package shu.scie.mariee.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@Entity
@NoArgsConstructor
@Table(name = "device")
public class Device {
    @Id
    @GeneratedValue()
    public Long id;

    public Long robotid;

    public String name;

    public Long status;
}
