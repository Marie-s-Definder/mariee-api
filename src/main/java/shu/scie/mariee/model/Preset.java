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

    public String dataInfoId;

    public Long device;

    public Long robotId;

    public Long p;

    public Long t;

    public Long z;

    public Long slidePresetId;
}
