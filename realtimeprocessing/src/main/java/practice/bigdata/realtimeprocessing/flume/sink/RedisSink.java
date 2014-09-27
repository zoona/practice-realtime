package practice.bigdata.realtimeprocessing.flume.sink;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

public class RedisSink extends AbstractSink implements Configurable {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  String redisHost;
  int redisPort;
  String redisKey;

  private Jedis jedis;

  @Override
  public Status process() throws EventDeliveryException {
    // TODO Auto-generated method stub
    Status status = null;
    // Start transaction
    Channel channel = getChannel();
    Transaction transaction = channel.getTransaction();
    transaction.begin();
    try {
      Event event = channel.take();
      Long result = jedis.rpush(redisKey, new String(event.getBody(), "utf-8"));
      if (result > 0) {
        logger.info("lpush : " + new String(event.getBody(), "utf-8")
            + " into " + redisKey + "(" + result + ")");
        transaction.commit();
        status = Status.READY;
      } else {
        logger.error("RPUSH FAILED");
        transaction.rollback();
        status = Status.BACKOFF;
      }
    } catch (Throwable t) {
      transaction.rollback();
      status = Status.BACKOFF;
      if (t instanceof Error) {
        throw (Error) t;
      }
    } finally {
      transaction.close();
    }
    return status;
  }

  @Override
  public void configure(Context context) {
    redisHost = context.getString("redisHost");
    redisPort = context.getInteger("redisPort");
    redisKey = context.getString("redisKey");
  }

  @Override
  public synchronized void start() {
    // TODO Auto-generated method stub
    super.start();
    jedis = new Jedis(redisHost, redisPort);
  }

  @Override
  public synchronized void stop() {
    // TODO Auto-generated method stub
    super.stop();
    jedis.disconnect();
  }

}
