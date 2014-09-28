var Packet = require('../public/core/packet');
var redisClient = require('redis').createClient("6379", "localhost");

function PacketProcessor(socket) {
  this.socket = socket;
}

PacketProcessor.prototype.process = function(socket) {
  var that = this;
  socket.on('message', function(message) {
    var packet = new Packet(message.header, message.body);
    if(packet.checkSelf()) {
      console.log(packet);
      switch(packet.header) {
        case "REQUEST_AUTH":
          that.onUserInfo(packet.body, socket);
          break;
        case "REQUEST_STATISTICS_HISTORY":
          that.onRequestStatistics(packet.body, socket);
          break;
      }
    }
  });
}

PacketProcessor.prototype.onUserInfo = function(data, socket) {
  console.log(data.name + "/" + data.email);
  var body = { status : "ok" };
  var packet = new Packet("RESPONSE_AUTH", body);
  socket.emit("message", packet);
}

PacketProcessor.prototype.onRequestStatistics = function(data, socket) {
  var dateTimeString = data.dateTimeString;
  var type = "CustomerAgeGrades";
  var count = data.count;
  var keys = [];
  var datetimeNow = new Date();
  if(datetimeNow.getSeconds() > 30)
    datetimeNow.setSeconds(30);
  else
    datetimeNow.setSeconds(0);

  var multi = redisClient.multi();
  for(var i = count - 1; i >= 0; i--) {
    var dateTimeString = this.getDatetimeString(datetimeNow, 30 * 1000 * i);
    var key = data.branch + ":" + dateTimeString + ":" + type;
    //var key = dateTimeString + ":" + type;
    keys.push(key);
    multi = multi.hgetall(key);
  }

  multi.exec(function(err, result) {
    var statistics = [];
    for(var i in result) {
      var counts = [];
      for(p in result[i]) {
        counts.push({type:p, count:result[i][p]})
      }
      statistics.push({
        branch:keys[i].split(":")[0],
        date:keys[i].split(":")[1],
        counts:counts
      });
    }
    console.log(statistics);

    var packet = new Packet("RESPONSE_STATISTICS_HISTORY", statistics);
    packet.emit(socket);
  });
}

PacketProcessor.prototype.getDatetimeString = function(srcDate, offset) {
  //2014-09-28T10:44:32.808Z
  var date = new Date(srcDate.getTime() - offset);
  date.setHours(date.getHours() + 9); // for KST
  var dateString = date.toISOString().slice(0, 10).replace(/-/g, "");
  var timeString = date.toISOString().slice(11, 19).replace(/:/g, "");
  return dateString + timeString;
};

module.exports = PacketProcessor;
