package shu.scie.mariee.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@Entity
@NoArgsConstructor
@Table(name = "data")
public class Data {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long robotid;

    public String devicename;

    public Date date;

    public String name;

    public String result;

    public Long status;

    public String imgpath;

    public Long getby;
}
