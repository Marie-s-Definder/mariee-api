package shu.scie.mariee.service;
import shu.scie.mariee.model.ScheduledFutureHolder;

import java.util.HashMap;
/**
 * ClassName: TempPreset
 * Package: shu.scie.mariee.service
 * Description:
 *
 * @Author Jetson
 * @Create 2024/5/23 16:32
 * @Version 1.0
 */
public abstract class TempPreset {
//    static StringBuffer robotid_preset ;
    public static HashMap<String, String> robotid_preset = new HashMap<>();
    public static HashMap<String, String> need_on = new HashMap<>();
//    public static HashMap<String, ScheduledFutureHolder> robotid_interval = new HashMap<>();
}

//class ChangeNeeoOn implements Runnable {
//    String deviceid;
//    public ChangeNeeoOn(String deviceid){
//        this.deviceid = deviceid;
//    }
//    @Override
//    public void run() {
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//    }
//}
