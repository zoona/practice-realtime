package practice.bigdata.realtimeprocessing.storm.Bolt;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import practice.bigdata.realtimeprocessing.model.Statistics;
import redis.clients.jedis.Jedis;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class CounterBolt extends BaseRichBolt {

  private static final long serialVersionUID = 1L;
  Logger logger = LoggerFactory.getLogger(this.getClass());
  OutputCollector collector;
  Jedis jedis;
  String redisHost;
  int redisPort;
  SimpleDateFormat dateFormatter;
  int saveInterval = 30;
  String lastDateString;
  Map<String, Statistics> statisticsMap = new HashMap<String, Statistics>();

  public CounterBolt(String redisHost, int redisPort) {
    this.redisHost = redisHost;
    this.redisPort = redisPort;
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context,
      OutputCollector collector) {
    this.collector = collector;
    jedis = new Jedis(redisHost, redisPort);
    jedis.connect();

    this.dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
  }

  @Override
  public void execute(Tuple input) {
    // get fields from tuple
    String customerAgeGrade = input.getStringByField("customerAgeGrade");
    String paymentMethod = input.getStringByField("paymentMethod");
    @SuppressWarnings("unchecked")
    List<String> orders = (List<String>) input.getValueByField("orders");
    // get statistics
    // customerAgeGrade is key
    Statistics statistics = statisticsMap.get(customerAgeGrade);
    if (statistics == null) {
      statistics = new Statistics();
    }
    // increase counts
    increaseMapValue(statistics.getCustomerAgeGrades(), customerAgeGrade);
    increaseMapValue(statistics.getPaymentMethods(), paymentMethod);
    for (String order : orders)
      increaseMapValue(statistics.getOrders(), order);
    statisticsMap.put(customerAgeGrade, statistics);
    // emit to SaveBolt (each 'saveInterval' seconds)
    String dateString = getDateString();
    if (lastDateString != dateString) {
      collector.emit("counter_stream", new Values(dateString, statistics));
      statisticsMap.clear();
      lastDateString = dateString;
    }
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream("counter_stream", new Fields("dateString",
        "statistics"));
  }

  public void increaseMapValue(Map<String, Long> map, String key) {
    Long count = map.get(key);
    if (count != null)
      map.put(key, count + 1);
    else
      map.put(key, 1L);
  }

  public String getDateString() {
    Calendar c = Calendar.getInstance();
    c.setTime(new Date());
    int second = (c.get(Calendar.SECOND) / saveInterval) * saveInterval;
    c.set(Calendar.SECOND, second);
    c.set(Calendar.MILLISECOND, 0);
    Date date = c.getTime();
    return this.dateFormatter.format(date);
  }
}
