package shu.scie.mariee.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Entity
@NoArgsConstructor
@Table(name = "hkipc")
public class HkIpc {

    @Id
    @GeneratedValue()
    public Long id;

    public String name;

    public String ip;

    public String username;

    public String password;

    public String ptzChannel;

    public String streamChannel;

    public String liveUrl;

    public String slide_ip;

    public Long slide_port;

    public Long interval_time;
}

