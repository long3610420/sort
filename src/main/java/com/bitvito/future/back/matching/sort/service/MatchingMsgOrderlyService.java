package com.bitvito.future.back.matching.sort.service;

//import com.aex.common.base.dto.CurrencyPairDTO;
//import com.aex.common.base.nacos.currencyPair.CurrencyPairCache;
import com.bitvito.future.back.matching.constant.OrderAction;
import com.bitvito.future.back.matching.entity.Order;
import com.bitvito.future.back.matching.entity.OrderRequest;
import com.bitvito.future.back.matching.sort.config.MatchingConfig;
import com.bitvito.future.back.matching.sort.config.PulsarConfig;
import com.bitvito.future.back.matching.sort.idgen.common.Result;
import com.bitvito.future.back.matching.sort.idgen.common.Status;
import com.bitvito.future.back.matching.sort.util.JsonUtils;
import com.bitvito.monitor.metric.profile.PrometheusMetricProfilerProcessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.BatchReceivePolicy;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@DependsOn({"matchingConfig"})
public class MatchingMsgOrderlyService implements CommandLineRunner {

    public static final int RETRY_LIMIT = 100_000_000;
    public static final int RETRY_INTERVAL = 1000;
    public static final long EXPIRE_TIME = 24 * 60 * 60;
    private static final PrometheusMetricProfilerProcessor LISTENER_METRIC = new PrometheusMetricProfilerProcessor(
            "sort_time", "message:listener", "sort use time",
            new String[]{"contractId", "node", "size"});
    //private final ConcurrentHashMap<Integer, Long> requestIds = new ConcurrentHashMap<>();
    //private final ConcurrentHashMap<Integer, Long> redisIds = new ConcurrentHashMap<>();
    //private final ConcurrentHashMap<Integer, ReentrantLock> mapLocks = new ConcurrentHashMap<Integer, ReentrantLock>();
    private final Executor exector = Executors.newSingleThreadExecutor();
    //private ButterflyIdGenerator butterflyIdGenerator;
    private final Map<String, Producer<String>> producerMap = new HashMap<>();
    //private String group;
    //private String orderlyTopic;
    //private DefaultMQProducer matchingMsgOrderlyProducer;
    private Integer machineId;
    private Consumer consumer;
    private Gson gson;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MatchingConfig matchingConfig;
    private PulsarClient pulsarClient;
    private String topic;
    private String contractName;
    private String prodTopic;
    private Set<Long> orderIds;
    //@Resource
    //private IdGeneratorService idGeneratorService;
    @Autowired
    private SegmentService segmentService;
    @Value("${bitvito.currencyPairIds}")
    private String currencyPairIds;
    private Set cpSet = new HashSet();
    private volatile boolean initSsOk = false;
    private Cache<Long, Boolean> graphs;

    @Override
    public void run(String... args) throws Exception {
        gson = new Gson();
        //group = matchingConfig.getRocket().getGroupName();
        //orderlyTopic = matchingConfig.getRocket().getTopicOrderlyPrefix();
        machineId = matchingConfig.getIdGenerator().getMachineId();
        contractName = matchingConfig.getSystem().getContractName();
        pulsarClient = PulsarClient.builder().serviceUrl(matchingConfig.getPulsar().getUrl()).build();
        //initMatchingResultProducer(matchingConfig.getRocket());
        topic = matchingConfig.getPulsar().getTopic();
        prodTopic = matchingConfig.getPulsar().getProdTopic();
        //initMatchingResultProducerPulsar(matchingConfig.getPulsar());
        //butterflyIdGenerator = initIdGenerator(matchingConfig.getIdGenerator());
        graphs = Caffeine.newBuilder()
                .maximumSize(100_000)
                //.expireAfterWrite(Duration.ofMinutes(30))
                //.refreshAfterWrite(Duration.ofMinutes(1))
                .build();
        String match_sort_order_ids = "MATCH_SORT_ORDER_IDS";
        Set<Long> members = redisTemplate.opsForSet().members(match_sort_order_ids);
        if (members != null) {
            for (Long orderId : members) {
                graphs.put(orderId, true);
            }
        }
        String[] split = currencyPairIds.split(",");
        for (String cpId : split) {
            cpSet.add(Integer.parseInt(cpId));
        }
        startPulsarConsumer(matchingConfig.getPulsar());
        exector.execute(() -> {
            received(consumer);
        });
        SignalHandler signalHandler = signal -> {
            try {
                if (consumer != null) {
                    consumer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.info(" system exit signal handle: " + signal.getName());
            // 监听信号量，通过System.exit(0)正常关闭JVM，触发关闭钩子执行收尾工作
            ConcurrentMap<Long, Boolean> longBooleanConcurrentMap = graphs.asMap();
            cpSet.forEach(n -> {
                Producer<String> stringProducer = producerMap.get(n);
                try {
                    if (stringProducer != null) {
                        stringProducer.close();
                    }
                } catch (PulsarClientException e) {
                    e.printStackTrace();
                }
            });

            Set<Long> longs = longBooleanConcurrentMap.keySet();
            if (longs.size() > 0) {
                redisTemplate.delete(match_sort_order_ids);
                redisTemplate.opsForSet().add(match_sort_order_ids, longs.toArray());
                redisTemplate.expire(match_sort_order_ids, 1, TimeUnit.DAYS);
            }
            System.exit(0);
        };
        Signal sg = new Signal("INT"); // kill -15 pid
        Signal.handle(sg, signalHandler);
        Signal sg1 = new Signal("TERM"); // kill -15 pid
        Signal.handle(sg1, signalHandler);
        Signal sg2 = new Signal("HUP"); // kill -15 pid
        Signal.handle(sg2, signalHandler);
    }

    private void startPulsarConsumer(PulsarConfig pulsarConfig) throws PulsarClientException {
        //PulsarClient pulsarClient = PulsarClient.builder().serviceUrl(pulsarConfig.getUrl()).build();
        consumer = pulsarClient.newConsumer(Schema.BYTES)
                .batchReceivePolicy(BatchReceivePolicy.builder()
                        .maxNumMessages(100).maxNumBytes(1024 * 1024)
                        .timeout(8, TimeUnit.MILLISECONDS).build())
                .topic(pulsarConfig.getTopic()).subscriptionName(pulsarConfig.getTopic())
                .subscriptionType(SubscriptionType.Failover)
                .subscribe();
    }

    protected void initMatchingResultProducerPulsar(PulsarConfig pulsarConfig) throws PulsarClientException {
        //if(pulsarConfig.getCreateTopic()!=null) {
        //    if (pulsarConfig.getCreateTopic()) {
        //        for (int i = 1001; i < 1006; i++) {
        //            Producer<String> producer = pulsarClient.newProducer(Schema.STRING)
        //                    .topic(pulsarConfig.getProdTopic() + i)
        //                    .create();
        //            producerMap.put("" + i, producer);
        //        }
        //    }
        //}
        log.info("Start mq initMatchingResultProducerPulsar, {}", pulsarConfig);
    }

    //private ButterflyIdGenerator initIdGenerator(IdGeneratorConfig idGeneratorConfig) {
    //IdGenerator.init(idGeneratorConfig.getDataCenterId(), idGeneratorConfig.getMachineId());
    //IdGeneratorOptions options = new IdGeneratorOptions(idGeneratorConfig.getMachineId().shortValue());
    //YitIdHelper.setIdGenerator(options);
    //OldIdGenerator.init(0, idGeneratorConfig.getMachineId());
    //ZkButterflyConfig config = new ZkButterflyConfig();
    //config.setHost(idGeneratorConfig.getZkAddress());
    //ButterflyIdGenerator generator = ButterflyIdGenerator.getInstance(config);
    //// 设置起始时间，如果不设置，则默认从2020年2月22日开始
    //generator.setStartTime(2021, 9, 1, 0, 0, 0);
    //// 添加业务空间，如果业务空间不存在，则会注册
    //generator.addNamespaces(contractName);

    //return null;// generator;
    //}


    private void handle(String message, Map<Integer, List<OrderRequest>> messageMap) throws IOException {
        //OrderRequest request = gson.fromJson(message, OrderRequest.class);
        List<OrderRequest> requests = JsonUtils.decode(message, new TypeReference<ArrayList<OrderRequest>>() {
        });
        if (requests == null) {
            return;
        }
        for (int i = 0; i < requests.size(); i++) {
            OrderRequest request = requests.get(i);
            Integer currencyPairId = request.getCurrencyPairId();
            if (currencyPairId == null) {
                return;
//        Lock lock = mapLocks.computeIfAbsent(currencyPairId, l -> new ReentrantLock())
            }
            if (!cpSet.contains(currencyPairId)) {
                return;
            }
            try {
                if (!initSsOk) {
//                    segmentService.init();
                    initSsOk = true;
                }
//            lock.lock();
                Result result = segmentService.getId(contractName);
                if (result.getStatus().equals(Status.EXCEPTION)) {
                    throw new Exception(result.toString());
                }
                long requestId = result.getId();

                //long requestId = 0L;
                //Long redisId = redisIds.get(businessType);
                //if (redisId == null) {
                //    Long generatorId = idGeneratorService.generatorId("" + businessType);
                //    redisIds.put(businessType, generatorId);
                //    requestId = generatorId * 1000;
                //} else {
                //    requestId = requestIds.get(businessType) + 1;
                //    if (requestId > redisId * 1000 + 999) {
                //        Long generatorId = idGeneratorService.generatorId("" + businessType);
                //        requestId = generatorId * 1000;
                //        redisIds.put(businessType, generatorId);
                //    }
                //}
                //requestIds.put(businessType, requestId);

                //butterflyIdGenerator.getUUid("usdt");
//            String uniqueKey = String.format(Constant.MATCHING_ORDER_KEY, request.getAction() != OrderAction.WITHDRAWAL_ORDER.getCode() ? request.getOrder().getId() : request.getOrderIds().get(0), request.getAction() == OrderAction.ORDER.getCode() ? 0 : 1); // 下单标识：0 撤单：1
//            Boolean exist = redisTemplate.opsForValue().setIfPresent(uniqueKey, currencyPairId + "-" + System.currentTimeMillis(), EXPIRE_TIME, TimeUnit.SECONDS);
//            if (exist) {
//                log.warn(" request exist message: {} request: {}", message, request);
//                return;
//            }
                Order order = request.getOrder();
                if (order != null) {
                    order.setCreateTime(System.currentTimeMillis());
                    Boolean ifPresent = graphs.getIfPresent(order.getId());
                    if (ifPresent != null && ifPresent) {
                        log.warn("Has put into same order in set {}", order);
                        continue;
                    }
                    if (OrderAction.ORDER.getCode() == request.getAction()) {
                        graphs.put(order.getId(), true);
                    }
//                    CurrencyPairDTO byCurrencyPairId = CurrencyPairCache.get(order.getBusinessType(), order.getCurrencyPairId());
//                    if (byCurrencyPairId != null && byCurrencyPairId.getMinChangeQuantity() != null) {
                        //log.info("order.getCurrencyPairId() {},getMinChangeQuantity scale:{}", order.getCurrencyPairId(), byCurrencyPairId.getMinChangeQuantity());
//                        int scale = byCurrencyPairId.getMinChangeQuantity().scale();
//                        order.setScale(scale);
//                        order.setExecutedRatio(byCurrencyPairId.getStopLossRatio());
//                    } else {
                        order.setScale(18);
                        order.setExecutedRatio(new BigDecimal("0.2"));
//                    }
                }
                request.setRequestId(requestId);
                //String s = gson.toJson(request);
                //Message msg = new Message(orderlyTopic + currencyPairId, s.getBytes());
                //msg.putUserProperty(Constant.MATCHING_SORT_MACHINE_ID, machineId.toString());
                List<OrderRequest> list = messageMap.get(currencyPairId);
                if (CollectionUtils.isEmpty(list)) {
                    list = Lists.newArrayList();
                    messageMap.put(currencyPairId, list);
                }
                list.add(request);
                messageMap.put(currencyPairId, list);
            } catch (Exception e) {
                log.error("handle request error:{}", e);
            } finally {
//            lock.unlock();
            }
        }
    }

    private void sendOrderlyMessage(Map<Integer, List<OrderRequest>> messages) {
        //SendResult sendResult;
        AtomicBoolean success = new AtomicBoolean(false);
        int retryCount = -1;
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        for (Map.Entry<Integer, List<OrderRequest>> entry : messages.entrySet()) {
            success.set(false);
            List<OrderRequest> messageList = entry.getValue();
            Integer key = entry.getKey();
            if (CollectionUtils.isEmpty(messageList)) {
                continue;
            }

            while (!success.get() && ++retryCount <= RETRY_LIMIT) {
                try {
                    //sendResult = matchingMsgOrderlyProducer.send(messageList);
                    Producer<String> producer = producerMap.get("" + key);
                    if (producer == null) {
                        Producer<String> producerNew = pulsarClient.newProducer(Schema.STRING)
                                .topic(prodTopic + key)
                                .create();
                        producerMap.put("" + key, producerNew);
                        producer = producerNew;
                    }
                    Producer<String> finalProducer = producer;
                    Lists.partition(messageList, 100).forEach(n -> {
                        success.set(true);
                        MessageId send = null;
                        String message = gson.toJson(n);
                        try {
                            send = finalProducer.send(message);
                        } catch (Exception e) {
                            log.error("sendOrderlyMessage error PulsarClientException", e);
                            success.set(false);
                        }

                        if (!success.get()) {
                            log.info("sendResult !success<-----: {}, body:{}, currencyId: {}", send, messageList.size(), key);
                            try {
                                Thread.sleep(RETRY_INTERVAL);
                            } catch (InterruptedException e) {
                                log.error("sendOrderlyMessage error InterruptedException", e);
                            }
                        } else {
                            log.info("sendResult end<-----: {}, body:{}, currencyId: {}", send, message, key);
                        }
                    });
                    //success = sendResult.getSendStatus() == SendStatus.SEND_OK;
                    //} catch (InterruptedException e) {
                    //    log.error("send matching request error, InterruptedException", e);
                    //} catch (RemotingException e) {
                    //    log.error("send matching request error, RemotingException", e);
                    //} catch (MQClientException e) {
                    //    log.error("send matching request error, MQClientException", e);
                    //} catch (MQBrokerException e) {
                    //    log.error("send matching request error, MQBrokerException", e);
                } catch (Exception e) {
                    if (!success.get()) {
                        log.info("sendResult !success<-----: {}, body:{}, currencyId: {}", messageList.size(), key);
                        try {
                            Thread.sleep(RETRY_INTERVAL);
                        } catch (InterruptedException ex) {
                            log.error("send matching request error, Exception {}", e);
                        }
                    }
                    log.error("send matching request error, Exception {}", e);
                }
            }
        }

        if (!success.get()) {
            log.error("send matching request error messages: {}", messages);
        }
    }

    public void received(Consumer consumer) {

        while (true) {
            Messages<Message> messsages = null;
            long begin = 0;
            try {
                messsages = consumer.batchReceive();
                if (messsages==null){
                    continue;
                }
                begin = System.nanoTime();
                int size = messsages.size();
                LISTENER_METRIC.startTimer(contractName, "" + machineId, "");
                LISTENER_METRIC.inc(contractName, "" + machineId, "");
                Map<Integer, List<OrderRequest>> messageMap = Maps.newConcurrentMap();
                for (org.apache.pulsar.client.api.Message message : messsages) {
                    String orderRequest = new String(message.getData(), StandardCharsets.UTF_8);
                    log.info("order source:{}", orderRequest);
                    handle(orderRequest, messageMap);
                }
                sendOrderlyMessage(messageMap);

                if (null != messsages && size != 0) {
                    consumer.acknowledge(messsages);
                }
                //if (size == 0) {
                //    Thread.sleep(50);
                //}
            } catch (PulsarClientException e) {
                try {
                    consumer = pulsarClient.newConsumer(Schema.BYTES)
                        .batchReceivePolicy(BatchReceivePolicy.builder()
                            .maxNumMessages(100).maxNumBytes(1024 * 1024)
                            .timeout(8, TimeUnit.MILLISECONDS).build())
                        .topic(matchingConfig.getPulsar().getTopic()).subscriptionName(matchingConfig.getPulsar().getTopic())
                        .subscriptionType(SubscriptionType.Failover)
                        .subscribe();
                } catch (PulsarClientException pulsarClientException) {
                    log.error(" received message error {}", e);
                }
                LISTENER_METRIC.error(contractName, "" + machineId, "");
                log.error(" received message error {}", e);
            } catch (IOException e) {
                LISTENER_METRIC.error(contractName, "" + machineId, "");
                log.error(" received message error {}", e);
            } catch (Exception e) {
                LISTENER_METRIC.error(contractName, "" + machineId, "");
                log.error(" received message error {}", e);
            } finally {
                long end = System.nanoTime();
                LISTENER_METRIC.observe(end - begin, TimeUnit.NANOSECONDS, contractName, "" + machineId, "");
            }

        }
    }
}
