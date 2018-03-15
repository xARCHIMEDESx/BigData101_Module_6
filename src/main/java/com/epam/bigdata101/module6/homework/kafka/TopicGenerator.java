package com.epam.bigdata101.module6.homework.kafka;

import com.epam.bigdata101.module6.homework.htm.MonitoringRecord;
import com.epam.bigdata101.module6.homework.utils.GlobalConstants;
import com.epam.bigdata101.module6.homework.utils.PropertiesLoader;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class TopicGenerator implements GlobalConstants {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicGenerator.class);
    private static final String DELIMETER = ",";

    public static void main(String[] args) {
    	Properties applicationProperties = PropertiesLoader.getGlobalProperties();
        if (!applicationProperties.isEmpty()) {
            final String sampleFile = applicationProperties.getProperty(GENERATOR_SAMPLE_FILE_CONFIG);
            final String topicName = applicationProperties.getProperty(KAFKA_RAW_TOPIC_CONFIG);
            
            /*
             * creating Kafka producer 
             * reading data from file
             * creating MonitoringRecord object per every line 
             * creating ProducerRecord and sending in to topic
             */
            try(KafkaProducer<String, MonitoringRecord> producer = KafkaHelper.createProducer()) {
            	try {
            		try(BufferedReader reader = new BufferedReader(new FileReader(sampleFile))) {
            			String line = reader.readLine(); 
            			while (line != null) {
            				MonitoringRecord monitoringRecord = new MonitoringRecord(line.split(DELIMETER));
            				String key = KafkaHelper.getKey(monitoringRecord);
            				ProducerRecord<String, MonitoringRecord> record = new ProducerRecord<>(topicName, key, monitoringRecord);
            				producer.send(record);
            				line = reader.readLine();
            			}
            		}
            	} catch (FileNotFoundException e) {
            		LOGGER.error("Input file not found", e);
            	} catch (IOException e) {
            		LOGGER.error("Error occured while reading data from the file", e);
            	} catch (Exception e) {
            		LOGGER.error("Error occured while writing to a topic", e);
            	}
            }
        }
    }
}
