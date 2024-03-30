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
@Table(name = "preset")
public class Preset {

    @Id
    @GeneratedValue()
    public Long id;

    public String data_info_id;

    public Long device;

    public Long robot_id;

    public Long p;

    public Long t;

    public Long z;
}
