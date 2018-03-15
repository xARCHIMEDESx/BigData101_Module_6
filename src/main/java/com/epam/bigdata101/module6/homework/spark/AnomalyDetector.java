package com.epam.bigdata101.module6.homework.spark;

import com.epam.bigdata101.module6.homework.htm.HTMNetwork;
import com.epam.bigdata101.module6.homework.htm.MonitoringRecord;
import com.epam.bigdata101.module6.homework.htm.ResultState;
import com.epam.bigdata101.module6.homework.kafka.KafkaHelper;
import com.epam.bigdata101.module6.homework.utils.GlobalConstants;
import com.epam.bigdata101.module6.homework.utils.PropertiesLoader;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function0;
import org.apache.spark.api.java.function.Function3;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaMapWithStateDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Tuple2;
import java.util.HashMap;
import java.util.Properties;

public class AnomalyDetector implements GlobalConstants {
   
	private static final Logger LOGGER = LoggerFactory.getLogger(AnomalyDetector.class);
		
    public static void main(String[] args) throws Exception {
        //load a properties file from class path, inside static method
        final Properties applicationProperties = PropertiesLoader.getGlobalProperties();
        if (!applicationProperties.isEmpty()) {
            final String appName = applicationProperties.getProperty(SPARK_APP_NAME_CONFIG);
            final String rawTopicName = applicationProperties.getProperty(KAFKA_RAW_TOPIC_CONFIG);
            final String enrichedTopicName = applicationProperties.getProperty(KAFKA_ENRICHED_TOPIC_CONFIG);
            final String checkpointDir = applicationProperties.getProperty(SPARK_CHECKPOINT_DIR_CONFIG);
            final Duration batchDuration = Duration.apply(Long.parseLong(applicationProperties.getProperty(SPARK_BATCH_DURATION_CONFIG)));
            
            // function for checkpoint directory usage
            Function0<JavaStreamingContext> createContextFunc = new Function0<JavaStreamingContext>() {
				private static final long serialVersionUID = 1L;
				@Override
        		public JavaStreamingContext call() {
					SparkConf conf = new SparkConf().setAppName(appName).setMaster("local[*]");
        			JavaStreamingContext jsc = new JavaStreamingContext(conf, batchDuration);
        			jsc.checkpoint(checkpointDir);
        			return jsc;
        		}
        	};        	
            
            try (JavaStreamingContext jsc = JavaStreamingContext.getOrCreate(checkpointDir, createContextFunc)) {      	
            	
            	// reading records from Kafka topic
            	JavaDStream<ConsumerRecord<String, MonitoringRecord>> inputKafkaStream = KafkaUtils.createDirectStream(
            			jsc,
            			LocationStrategies.PreferConsistent(),
            			KafkaHelper.createConsumerStrategy(rawTopicName)
            	);  
                      
            	// creating pairs: DeviceID-Record
            	JavaPairDStream<String,MonitoringRecord> recordsPerDevice = inputKafkaStream.mapToPair(record -> 
            			new Tuple2<>(KafkaHelper.getKey(record.value()), record.value())); 	
            	
            	// passing records to mapWithState function
            	JavaMapWithStateDStream<String, MonitoringRecord, HTMNetwork, MonitoringRecord> enrichedRecords = 
            			recordsPerDevice.mapWithState(StateSpec.function(mappingFunc));  
            	
            	// creating Kafka producer and writing enriched records to topic
                try {
                	enrichedRecords.foreachRDD(rdd -> {
                		rdd.foreachPartition(partition -> {
                			try(KafkaProducer<String, MonitoringRecord> producer = KafkaHelper.createProducer()) {
                				partition.forEachRemaining(record -> {
                					ProducerRecord<String, MonitoringRecord> enrichedRecord = 
                							new ProducerRecord<>(enrichedTopicName, KafkaHelper.getKey(record), record);
                					producer.send(enrichedRecord);
                				
                				});
                			}
                		});
                	});
                } catch (Exception e) {
                	LOGGER.error("Error occured while writing to an enriched topic", e);
                } 

                jsc.start();
                jsc.awaitTermination();
            }     
        }
    }

	private static Function3<String, Optional<MonitoringRecord>, State<HTMNetwork>, MonitoringRecord> mappingFunc =
			(deviceID, recordOpt, state) -> {
                // case 0: timeout
                if (!recordOpt.isPresent())
                    return null;

                // either new or existing device
                if (!state.exists())
                    state.update(new HTMNetwork(deviceID));
                HTMNetwork htmNetwork = state.get();
                String stateDeviceID = htmNetwork.getId();
                if (!stateDeviceID.equals(deviceID))
                    throw new Exception("Wrong behaviour of Spark: stream key is $deviceID%s, while the actual state key is $stateDeviceID%s");
                MonitoringRecord record = recordOpt.get();

                // get the value of DT and Measurement and pass it to the HTM
                HashMap<String, Object> m = new java.util.HashMap<>();
                m.put("DT", DateTimeFormat.forPattern("YY-MM-dd HH:mm").withZone(DateTimeZone.UTC).parseDateTime((record.getDateGMT()
                		+ " " + record.getTimeGMT())));
                m.put("Measurement", Double.parseDouble(record.getSampleMeasurement()));
                ResultState rs = htmNetwork.compute(m);
                record.setPrediction(rs.getPrediction());
                record.setError(rs.getError());
                record.setAnomaly(rs.getAnomaly());
                record.setPredictionNext(rs.getPredictionNext());

                return record;
            };            
        

}