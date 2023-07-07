package com.mojito.learn.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * @author liufq
 * @since 2022-08-09 19:40:15
 */
@Slf4j
public class EtcdUtils {

    /**
     * 生成utf8类型的ByteSequence
     *
     * @param key
     * @return
     */
    public static ByteSequence getByteSequenceKey(String key) {
        return ByteSequence.from(key, StandardCharsets.UTF_8);
    }

    /**
     * 生成一个ByteSequence类型的key
     *
     * @param prefix
     * @param key
     * @return
     */
    public static ByteSequence getByteSequenceKey(String prefix, String key) {
        return ByteSequence.from(getStringKey(prefix, key).getBytes(StandardCharsets.UTF_8));
    }


    /**
     * 获取utf-8编码的ByteSequence值
     *
     * @param value
     * @return
     */
    public static ByteSequence getByteSequenceValue(String value) {
        return ByteSequence.from(value, StandardCharsets.UTF_8);
    }

    public static String getStringKey(String prefix, String key) {
        return (prefix + (prefix.endsWith("/") ? "" : "/") + key);
    }

    public static String getStringValue(ByteSequence bs) {
        return bs.toString(StandardCharsets.UTF_8);
    }

    /**
     * 获取GetResponse
     *
     * @param kvClient
     * @param key
     * @param isPrefix
     * @return
     */
    @SneakyThrows
    public static GetResponse getResponse(KV kvClient, String key, boolean isPrefix) {
        try {
            return kvClient.get(EtcdUtils.getByteSequenceKey(key),
                    GetOption.newBuilder().isPrefix(isPrefix).build()).get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("ETCD连接超时", e);
        }
    }

    @SneakyThrows
    public static GetResponse getResponse(KV kvClient, String key, boolean isPrefix, boolean onlyKeys) {
        return kvClient.get(EtcdUtils.getByteSequenceKey(key),
                GetOption.newBuilder().isPrefix(isPrefix).withKeysOnly(onlyKeys).build()).get(10, TimeUnit.SECONDS);

    }


    /**
     * 预先查询到的所有的keyValue，然后通过key获取对应值，如不存在获取默认值
     *
     * @param keyValues
     * @param keyPrefix
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getFirstOrDefault(List<KeyValue> keyValues, String keyPrefix, String key, String defaultValue) {
        return keyValues.stream().filter(kv -> kv.getKey().equals(getByteSequenceKey(keyPrefix, key))).findFirst()
                .map(w -> w.getValue().toString(StandardCharsets.UTF_8)).orElse(defaultValue);
    }

    public static String getFirstOrDefaultByKey(List<KeyValue> keyValues, String key, String defaultValue) {
        return keyValues.stream().filter(kv -> kv.getKey().equals(getByteSequenceKey(key))).findFirst()
                .map(w -> w.getValue().toString(StandardCharsets.UTF_8)).orElse(defaultValue);
    }

    /**
     * @param keyValues
     * @param keyPrefix
     * @param key
     * @return
     */
    public static String getFirstOrDefault(List<KeyValue> keyValues, String keyPrefix, String key) {
        return getFirstOrDefault(keyValues, keyPrefix, key, "");
    }

    /**
     * 获取key对应的mod_version字段
     *
     * @param keyValues
     * @param keyPrefix
     * @param key
     * @param defaultReversion
     * @return
     */
    public static Long getReversionOrDefault(List<KeyValue> keyValues, String keyPrefix, String key, Long defaultReversion) {
        return keyValues.stream().filter(kv -> kv.getKey().equals(getByteSequenceKey(keyPrefix, key))).findFirst()
                .map(KeyValue::getModRevision).orElse(defaultReversion);
    }


    /**
     * 通过key精确查询，然后看这个值是否存在，否则返回null
     *
     * @param keyValues
     * @param defaultValue
     * @return
     */
    public static String getFirstOrDefault(List<KeyValue> keyValues, String defaultValue) {
        return keyValues.stream().map(value -> value.getValue().toString(StandardCharsets.UTF_8)).findFirst().orElse(null);
    }

    /**
     * 通过前缀查所有的值
     *
     * @param keyValues
     * @param keyPrefix
     * @return
     */
    public static List<String> getValuesByPrefix(List<KeyValue> keyValues, String keyPrefix) {
        List<KeyValue> values = keyValues.stream().filter(kv -> kv.getKey().startsWith(getByteSequenceKey(keyPrefix))).collect(Collectors.toList());
        return values.stream().map(kv -> kv.getValue().toString(StandardCharsets.UTF_8)).collect(Collectors.toList());
    }

    /**
     * 根据前缀查询返回所有的id
     *
     * @param kvClient
     * @param keyPrefix
     * @param idIndex
     * @return
     */
    public static List<String> getGroups(KV kvClient, String keyPrefix, int idIndex) {
        try {
            GetResponse rsp = kvClient.get(
                    getByteSequenceKey(keyPrefix),
                    GetOption.newBuilder().isPrefix(true).withKeysOnly(true).build()).get();
            Map<String, List<KeyValue>> idMap = rsp.getKvs().stream().collect(Collectors.groupingBy(kv -> kv.getKey().toString(StandardCharsets.UTF_8).split("/")[idIndex]));
            return new ArrayList<>(idMap.keySet());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void putValue(Lease leaseClient, KV kvClient, String key, String value, Long ttl) {
        try {
            Long leaseId = null;
            if (ttl != 0L) {
                leaseId = leaseClient.grant(ttl).get().getID();
                PutResponse putResponse = kvClient.put(getByteSequenceKey(key), getByteSequenceValue(value),
                        PutOption.newBuilder().withLeaseId(leaseId).build()).get();
            } else {
                PutResponse putResponse = kvClient.put(getByteSequenceKey(key), getByteSequenceValue(value)).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("写值失败", e);
        }
    }

    public static void putValue(KV client, String key, String value) {
        try {
            PutResponse putResponse = client.put(getByteSequenceKey(key), getByteSequenceValue(value)).get();

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("写值失败", e);
        }
    }

    public static void putObject(KV client, String keyPrefix, Object obj) {
        putObject(client, keyPrefix, obj, null, null);
    }

    public static void putObject(KV client, String keyPrefix, Object obj, Lease leaseClient, Long ttl) {
        // 没有实现事务--后期优化
        Map<String, String> values = new HashMap<>();
        try {

            for (Field field : obj.getClass().getDeclaredFields()) {
                JsonProperty annotation = field.getAnnotation(JsonProperty.class);
                if (annotation != null) {
                    field.setAccessible(true);
                    values.put(annotation.value(), String.valueOf(field.get(obj)));
                }
            }
            if (values.keySet().size() == 0) {
                throw new RuntimeException("写入的模型有误");
            }
            values.forEach((key, value) -> {
                try {
                    if (leaseClient != null && ttl != null) {
                        putValue(leaseClient, client, getStringKey(keyPrefix, key), value, ttl);
                    } else {
                        putValue(client, getStringKey(keyPrefix, key), value);
                    }
                } catch (Exception ex) {
                    log.error(String.format("写入%s失败", key), ex);
                }
            });
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
}
