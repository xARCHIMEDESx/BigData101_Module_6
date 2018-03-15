Realtime Anomaly Detection
==========================

This project is a prototype for real time anomaly detection.
It uses [Numenta's Hierarchical Temporal Memory](https://numenta.org/) -  a technology which emulates the work of the cortex.

# What was done before
1)	Everything concerning the intellectual part - the HTM network.
2) Prototype of visualization based on zeppelin notebook.

# What was done by me
Spark steaming application which takes the input stream from Kafka, 
detects anomalies using online learning and outputs enriched records back into Kafka.

# Run sequence

## One time actions

### Zookeeper
Run Zookeeper:
```
bin\zkServer.cmd
```

### Kafka
Run Kafka:
```
bin\windows\kafka-server-start.bat .\config\server.properties
```

Create kafka topics called "monitoring20" and "monitoringEnriched2", e.g.:
```
bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 10 --topic monitoring20
bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic monitoringEnriched2
```

## Zeppelin
Run :
```
bin\zeppelin.cmd
```

Import anomaly-detector-kafka.json notebook into Zeppelin.

In case of problems with Windows path - replace spark-submit on spark-submit2
in bin\interpreter.cmd for spark.

## Running the application during development cycle

1) Start streaming application (com.epam.bdcc.spark.AnomalyDetector), to wait for incoming data.

2) Start visualizer prototype in zeppelin:
    - Start "Visualizer Section".
    - Start "Streaming Section".
    - Use "Stop Streaming Application" to stop streaming section.

3) Run the com.epam.bdcc.kafka.TopicGenerator, to fill in raw Kafka topic.

It takes records from csv (data\one_device_2015-2017.csv) and puts them into raw Kafka topic. 
See records schema in com.epam.bdcc.htm.MonitoringRecord