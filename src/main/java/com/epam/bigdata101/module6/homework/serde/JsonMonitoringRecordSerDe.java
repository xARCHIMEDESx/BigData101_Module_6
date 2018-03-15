package com.epam.bigdata101.module6.homework.serde;

import com.epam.bigdata101.module6.homework.htm.MonitoringRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class JsonMonitoringRecordSerDe implements Deserializer<MonitoringRecord>, Serializer<MonitoringRecord>{
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMonitoringRecordSerDe.class);
    private ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public byte[] serialize(String topic, MonitoringRecord record) {
    	byte[] data = null;
    	try {
    		data = mapper.writeValueAsString(record).getBytes();
    	} catch (Exception e){
    		LOGGER.error("Error occured while serializing MonitoringRecord", e);
    	}    	
    	return data;
    }

    @Override
    public MonitoringRecord deserialize(String topic, byte[] data) {
        MonitoringRecord record = null;
        try {
            record = mapper.readValue(data, MonitoringRecord.class);
        } catch (Exception e) {
            LOGGER.error("Error occured while deserializing MonitoringRecord", e);
        }        
        return record;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}
    
    @Override
    public void close() {
    	mapper = null;
    }
}