$(function() {
// connect to server
var host = window.location.host.split(':')[0];
var socket = io.connect('http://' + host);

socket.on('connect', function() {
  console.log("connected to " + host);
  var body = { name : 'hozoon', email : 'zoona@zoona.com' };
  var packet = new Packet('REQUEST_AUTH', body);
  packet.emit(socket);
});

socket.on('disconnect', function() {
  console.log("disconnected from " + host);
});

socket.on('message', function(message) {
  var packet = new Packet(message.header, message.body);
  if(packet.checkSelf()) {
    switch(packet.header) {
      case "RESPONSE_AUTH":
        var status = packet.body.status;
        if(status == "ok") {
          // request statistics
          var body = { /*branch: "jeongja",*/ type:"CustomerAgeGrades", count:20 }
          var packetReqStatistics = new Packet(
            'REQUEST_STATISTICS_HISTORY', body);
          packetReqStatistics.emit(socket);
        }
        else {
          console.log("rejected");
        }
        break;
      case "RESPONSE_STATISTICS_HISTORY":
        d3chart.dataset = [];
        d3chart.dataset = packet.body;
        d3chart.updateChart();
        break;
      case "PUBLISH_STATISTICS":
        console.log(packet);
        break;
      default:
        console.log(packet.head);
        break;
    }
  }
});
});
