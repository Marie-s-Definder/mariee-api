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
@Table(name = "data")
public class Data {
    @Id
    @GeneratedValue()
    public Long id;

    public Long robotid;

    public String devicename;

    public Date date;

    public String name;

    public String result;

    public Long status;

    public String imgpath;
}
