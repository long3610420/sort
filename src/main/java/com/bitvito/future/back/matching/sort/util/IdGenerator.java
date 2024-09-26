package com.bitvito.future.back.matching.sort.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class IdGenerator {
    private final static long TIMESTAMP_START = 1500000000000L;

    // "0" + 41-bits-timestamp + 5-bits-data-center + 5-bits-machine + 13-bits-sequenceId
    private final static long SEQUENCE_BIT = 13;
    private final static long MACHINE_BIT = 5;
    private final static long DATA_CENTER_BIT = 5;


    private final static long MAX_DATA_CENTER_NUM = ~(-1L << DATA_CENTER_BIT);
    private final static long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    private final static long MACHINE_OFFSET = SEQUENCE_BIT;
    private final static long DATA_CENTER_OFFSET = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTAMP_OFFSET = DATA_CENTER_OFFSET + DATA_CENTER_BIT;

    private static IdGenerator defaultIdGenerator;
    private static Map<Long, IdGenerator> auxiliaryGenerators = new ConcurrentHashMap<>();

    private final long dataCenterId;
    private final long machineId;
    private long sequenceId = 0L;
    private long lastTimestamp = -1L;

    private IdGenerator(long dataCenterId, long machineId) {
        if (dataCenterId > MAX_DATA_CENTER_NUM || dataCenterId < 0) {
            throw new IllegalArgumentException(String.format("DataCenterId should be in [0, %s]", MAX_DATA_CENTER_NUM));
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException(String.format("MachineId should be in [0, %s]", MAX_MACHINE_NUM));
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    public static synchronized void init(long defaultBusinessId, long machineId) {
        if (defaultIdGenerator == null) {
            defaultIdGenerator = new IdGenerator(defaultBusinessId, machineId);
        } else {
            throw new IllegalStateException("IdGenerator has been initialized.");
        }
    }

    public static synchronized void register(long businessId) {
        if (defaultIdGenerator == null) {
            throw new IllegalStateException("IdGenerator not initialized.");
        }
        auxiliaryGenerators.putIfAbsent(businessId, new IdGenerator(businessId, defaultIdGenerator.machineId));
    }

    public static synchronized long nextId(long businessId) {
        IdGenerator idGenerator = auxiliaryGenerators.get(businessId);
        if (idGenerator == null) {
            throw new IllegalStateException(String.format("auxiliary generator of %d has not been registered.", businessId));
        }
        return idGenerator.generateNextId();
    }

    public static long nextId() {
        return defaultIdGenerator.generateNextId();
    }

    public static long getTimestamp(long id) {
        return (id >> TIMESTAMP_OFFSET) + TIMESTAMP_START;
    }

    public static long getMachineId(long id) {
        return (id >> MACHINE_OFFSET) & MAX_MACHINE_NUM;
    }

    public static long getDataCenterId(long id) {
        return (id >> DATA_CENTER_OFFSET) & MAX_DATA_CENTER_NUM;
    }

    private synchronized long generateNextId() {
        long currentTimestamp = current();
        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Cannot generate id, because time is earlier than last");
        }

        if (currentTimestamp == lastTimestamp) {
            sequenceId = (sequenceId + 1) & MAX_SEQUENCE;
            if (sequenceId == 0L) {
                currentTimestamp = waitToNextMillis();
            }
        } else {
            sequenceId = 0L;
        }

        lastTimestamp = currentTimestamp;

        return (currentTimestamp - TIMESTAMP_START) << TIMESTAMP_OFFSET | dataCenterId << DATA_CENTER_OFFSET | machineId << MACHINE_OFFSET | sequenceId;
    }

    public static void tryWaitIdGenerator(Long lastIdGenerated, Long delayTime, Consumer<Long> onWait) throws InterruptedException {
        if (lastIdGenerated == null) return;
        long timestamp = IdGenerator.getTimestamp(lastIdGenerated);
        long timeRemained = timestamp - System.currentTimeMillis() + delayTime;
        if (timeRemained > 0) {
            onWait.accept(timeRemained);
            Thread.sleep(timeRemained);
        }
    }

    private long waitToNextMillis() {
        long mill = current();
        while (mill <= lastTimestamp) {
            mill = current();
        }
        return mill;
    }

    private long current() {
        return System.currentTimeMillis();
    }
}
