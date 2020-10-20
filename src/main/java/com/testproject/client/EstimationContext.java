package com.testproject.client;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class EstimationContext {
    private volatile AtomicInteger count = new AtomicInteger(0);
    private int size;
    private double estimation;
    private volatile boolean isContinuing = true;

    /**
     * Gets estimation or progress percentage in case of the calculations is not finished
     *
     * @return current progress
     */
    public String getEstimation() {
        return size == count.get() ? Double.toString(estimation) : (int) (count.get() * 1.0 / size * 100) + "%";
    }
}
