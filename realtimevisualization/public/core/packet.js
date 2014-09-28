/**
 * a simple class that wrap json data and has some other functions
 *
 * @author zooNa
 * @date 22/04/2014
 */
var Packet = (function() {

  var Packet = function(header, body) {
    this.header = header;
    this.body = body;
  };

  Packet.prototype.toJSONString = function() {
    return JSON.stringify(this);
  };

  Packet.prototype.checkSelf = function() {
    if(this.header === undefined || this.body === undefined)
      return false;
    else
      return true;
  };

  Packet.prototype.fromJSONString = function(jsonString) {
    var jsonObj = JSON.parse(jsonString);

    this.header = jsonObj.header;
    this.body = jsonObj.body;
    return this;
  }

  Packet.prototype.emit = function(socket) {
    if(this.checkSelf()) {
      socket.send(this);
      console.log("emit " + this.header);
      return true;
    }
    else
      console.log("invalid packet");
      return false;
  }
  return Packet;
})();


if (typeof module !== 'undefined' && typeof module.exports !== 'undefined')
  module.exports = Packet;
else
  window.Packet = Packet;
