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
@Table(name = "data_info")
public class DataInfo {
    @Id
    @GeneratedValue()
    public Long id;

    public Long device_id;

    public String name;

    public String lower_limit;

    public String upper_limit;

    public String box;

    public String unit;

    public String type;
}
