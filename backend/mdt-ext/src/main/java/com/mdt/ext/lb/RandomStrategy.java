package com.mdt.ext.lb;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** GAP-5：随机（Random）策略 */
public class RandomStrategy implements LoadBalancerStrategy {

    @Override
    public int select(List<String> backends, String requestKey) {
        return ThreadLocalRandom.current().nextInt(backends.size());
    }

    @Override public String name() { return "RANDOM"; }
}
