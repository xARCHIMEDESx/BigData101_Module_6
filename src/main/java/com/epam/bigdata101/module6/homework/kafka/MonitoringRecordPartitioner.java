package com.epam.bigdata101.module6.homework.kafka;

import com.epam.bigdata101.module6.homework.htm.MonitoringRecord;

import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.Cluster;

import java.util.List;
import java.util.Map;

public class MonitoringRecordPartitioner extends DefaultPartitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        if (value instanceof MonitoringRecord) {
        	final List<PartitionInfo> partitionInfoList = cluster.availablePartitionsForTopic(topic);
            final int partitionCount = partitionInfoList.size();            
            return Math.abs(((String)key).hashCode()) % partitionCount;  
        } else {
        	return super.partition(topic, key, keyBytes, value, valueBytes, cluster);
        }
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> map) {}
}