package shu.scie.mariee.model;

import java.util.concurrent.ScheduledFuture;

public class ScheduledFutureHolder {
    private ScheduledFuture<?> scheduledFuture;

    private Class<? extends Runnable> runnableClass;

    private Long interval_time;;

    public ScheduledFuture<?> getScheduledFuture() {
        return scheduledFuture;
    }

    public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    public Class<? extends Runnable> getRunnableClass() {
        return runnableClass;
    }

    public void setRunnableClass(Class<? extends Runnable> runnableClass) {
        this.runnableClass = runnableClass;
    }

    public Long getinterval() {
        return interval_time;
    }

    public void setinterval(Long interval_time) {
        this.interval_time = interval_time;
    }

    @Override
    public String toString() {
        return "ScheduledFutureHolder{" +
                "scheduledFuture=" + scheduledFuture +
                ", runnableClass=" + runnableClass +
                ", interval_time='" + interval_time + '\'' +
                '}';
    }
}
