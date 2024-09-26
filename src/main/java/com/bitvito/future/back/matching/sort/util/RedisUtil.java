//package com.bitvito.future.back.matching.sort.util;
//
//import io.lettuce.core.RedisFuture;
//import io.lettuce.core.cluster.RedisAdvancedClusterAsyncCommandsImpl;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.data.redis.core.RedisCallback;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.core.script.DefaultRedisScript;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//import org.springframework.scripting.support.ResourceScriptSource;
//import org.springframework.stereotype.Component;
//import org.springframework.util.CollectionUtils;
//
//import javax.annotation.Resource;
//import java.util.Collection;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.TimeUnit;
//
///**
// * redis util
// *
// * @param <V>
// * @author lvjiarui
// */
//@Component
//@Slf4j
//public class RedisUtil<V> {
//
//    @Resource
//    private RedisTemplate<String, V> redisTemplate;
//
//
//    /**
//     * 默认过期时长，单位：秒
//     */
//    public final long DEFAULT_EXPIRE = 60 * 60 * 24;
//    /**
//     * 加锁过期时长，单位：秒
//     */
//    public final long LOCString_EXPIRE = 60 * 60;
//    /**
//     * 不设置过期时长
//     */
//    public final long NOT_EXPIRE = -1;
//
//    /**
//     * 将参数中的字符串值设置为键的值，不设置过期时间
//     *
//     * @param key
//     * @param value 必须要实现 Serializable 接口
//     */
//    public void set(String key, V value) {
//        redisTemplate.opsForValue().set(key, value);
//    }
//
//    /**
//     * 将参数中的字符串值设置为键的值，设置过期时间
//     *
//     * @param key
//     * @param value   必须要实现 Serializable 接口
//     * @param timeout
//     */
//    public void set(String key, V value, Long timeout) {
//        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
//    }
//
//    /**
//     * 获取与指定键相关的值
//     *
//     * @param key
//     * @return
//     */
//    public Object get(String key) {
//        return redisTemplate.opsForValue().get(key);
//    }
//
//    /**
//     * 设置某个键的过期时间
//     *
//     * @param key 键值
//     * @param ttl 过期秒数
//     */
//    public boolean expire(String key, Long ttl) {
//        return redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
//    }
//
//    /**
//     * 判断某个键是否存在
//     *
//     * @param key 键值
//     */
//    public boolean hasStringey(String key) {
//        return redisTemplate.hasKey(key);
//    }
//
//    /**
//     * 向集合添加元素
//     *
//     * @param key
//     * @param value
//     * @return 返回值为设置成功的value数
//     */
//    public Long sAdd(String key, V... value) {
//        return redisTemplate.opsForSet().add(key, value);
//    }
//
//    /**
//     * 获取集合中的某个元素
//     *
//     * @param key
//     * @return 返回值为redis中键值为key的value的Set集合
//     */
//    public Set<V> sGetMembers(String key) {
//        return redisTemplate.opsForSet().members(key);
//    }
//
//    /**
//     * 将给定分数的指定成员添加到键中存储的排序集合中
//     *
//     * @param key
//     * @param value
//     * @param score
//     * @return
//     */
//    public Boolean zAdd(String key, V value, double score) {
//        return redisTemplate.opsForZSet().add(key, value, score);
//    }
//
//
//    /**
//     * zset 自增
//     *
//     * @param key
//     * @param value
//     * @param score
//     * @return
//     */
//    public Double zAddIncr(String key, V value, long score) {
//        return redisTemplate.opsForZSet().incrementScore(key, value, score);
//    }
//
//
//    /**
//     * 返回指定排序集中给定成员的分数
//     *
//     * @param key
//     * @param value
//     * @return
//     */
//    public Double zScore(String key, V value) {
//        return redisTemplate.opsForZSet().score(key, value);
//    }
//
//
//    /**
//     * 获取集合中最大分数
//     *
//     * @param key
//     * @return
//     */
//    public Object getZsetMaxScore(String key) {
//
//        Set<V> set = redisTemplate.opsForZSet().rangeByScore(key, 0d, Double.MAX_VALUE, 0, 1);
//        if (CollectionUtils.isEmpty(set)) {
//            return null;
//        }
//        return set.iterator().next();
//    }
//
//
//    /**
//     * 删除指定的键
//     *
//     * @param key
//     * @return
//     */
//    public Boolean delete(String key) {
//        return redisTemplate.delete(key);
//    }
//
//    /**
//     * 删除多个键
//     *
//     * @param keys
//     * @return
//     */
//    public Long delete(Collection<String> keys) {
//        return redisTemplate.delete(keys);
//    }
//
//    /**
//     * 删除zset value
//     *
//     * @param key
//     * @param value
//     * @return
//     */
//    public boolean zSetRem(String key, V value) {
//
//        Long res = redisTemplate.opsForZSet().remove(key, value);
//
//        return null != res && res > 0;
//    }
//
//
//    /**
//     * 删除zset value
//     *
//     * @param key
//     * @param score
//     * @return
//     */
//    public Long incr(String key, Long score) {
//        return redisTemplate.opsForValue().increment(key, score);
//    }
//
//    public void convertAndSend(String channel, Object message){
//        redisTemplate.convertAndSend(channel, message);
//    }
//
//
//    public Long executeLua(String luaFileName, List<String> keys, Object... values) {
//        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/" + luaFileName)));
//        // 指定返回类型
//        redisScript.setResultType(Long.class);
//
//        // 参数一：redisScript，参数二：key列表，参数三：arg（可多个）
//
//        return redisTemplate.execute(redisScript, keys, values);
//    }
//
//    public String executeLuaReturnString(String luaFileName, List<String> keys, Object... values) {
//        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
//        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/" + luaFileName)));
//        // 指定返回类型
//        redisScript.setResultType(String.class);
//
//        // 参数一：redisScript，参数二：key列表，参数三：arg（可多个）
//
//        return redisTemplate.execute(redisScript, keys, values);
//    }
//
//
//
//    /**
//     * 集群缓存lua脚本 返回 sha1
//     * @param luaFileName
//     * @return
//     */
//    public String  loadScript(String luaFileName) {
//        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//
//        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/" + luaFileName)));
//        String script = redisScript.getScriptAsString();
//        return redisTemplate.execute((RedisCallback<String>) connection -> {
//            Object nativeConnection = connection.getNativeConnection();
//            byte[] bScript= new StringRedisSerializer().serialize(script);
//            RedisAdvancedClusterAsyncCommandsImpl redisAdvancedClusterAsyncCommands = (RedisAdvancedClusterAsyncCommandsImpl) nativeConnection;
//            RedisFuture<String> redisFuture = redisAdvancedClusterAsyncCommands.scriptLoad(bScript);
//            try {
//                return redisFuture.get();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return null;
//        });
//
//    }
//
//
//    public void hset(String key, String keyStr, String toJSONString) {
//        redisTemplate.opsForHash().put(key,keyStr,toJSONString);
//    }
//}
