var Packet = require('../public/core/packet');

function RedisSubscriber(port, host, channel) {
  this.redis = require('redis');
  this.redisClient = this.redis.createClient(port, host, null);
  this.channel = channel;
  this.socket = null;
  this.lastPushTime = null;
}

RedisSubscriber.prototype.subscribe = function() {
  var that = this;
  this.redisClient.on('message', function(channel, message) {
    // send packet to all
    //console.log(packet);
    var packet = new Packet().fromJSONString(message);
    var now = new Date().getTime();
    if(that.socket != null) {
      if(now - that.lastPushTime >= 1000) {
        packet.emit(that.socket);
        that.lastPushTime = now;
      }
    }
  });

  this.redisClient.on('subscribe', function(channel, count) {
    console.log("subscribes from '" + channel + "'. count: " + count);
  });

  this.redisClient.subscribe(this.channel);
};

RedisSubscriber.prototype.unsubscribe = function() {
  this.redisClient.unsubscribe(this.channel);
};

RedisSubscriber.prototype.disconnect = function() {
  if (this.redisClient !== null)
    this.redisClient.quit();
};

module.exports = RedisSubscriber;
