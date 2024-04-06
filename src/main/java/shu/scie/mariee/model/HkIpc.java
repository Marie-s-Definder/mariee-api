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
@Table(name = "HkIpc")
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

}

