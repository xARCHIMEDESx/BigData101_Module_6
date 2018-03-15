package com.epam.bigdata101.module6.homework.serde;

import com.epam.bigdata101.module6.homework.htm.MonitoringRecord;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class KryoMonitoringRecordSerDe implements Serializer<MonitoringRecord>, Deserializer<MonitoringRecord>{
	
    private static final Logger LOGGER = LoggerFactory.getLogger(KryoMonitoringRecordSerDe.class);
    
    private ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            return new Kryo();
        };
    };

	@Override
	public byte[] serialize(String topic, MonitoringRecord data) {	
		try(Output output = new Output(1024)){
			try {
				kryos.get().writeObject(output, data);
			} catch (Exception e) {
				LOGGER.error("Error occured while serializing MonitoringRecord", e);
			}
		    return output.toBytes();
		} 
	}

	@Override
	public MonitoringRecord deserialize(String topic, byte[] data) {
		MonitoringRecord record = null;
		try(Input input = new Input(1024)){
			try {
				input.setBuffer(data);
				record = kryos.get().readObject(input, MonitoringRecord.class);	
			} catch (Exception e) {
				LOGGER.error("Error occured while deserializing MonitoringRecord", e);
			}
			return record;
		}
	}

	@Override
	public void close() {
		kryos = null;
	}
	
	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {}
}


