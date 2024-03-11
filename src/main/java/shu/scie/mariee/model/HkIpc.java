package shu.scie.mariee.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class HkIpc {
    public String id;
    public String ip;
    public String username;
    public String password;
    public String ptzChannel;
    public String streamChannel;
    public String liveUrl;
}
