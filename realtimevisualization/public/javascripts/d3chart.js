function D3Chart(divID) {
  this.divID = "#" + divID;
  this.dataset = null;
  this.containerWidth = 0;
  this.containerHeight = 0;
  this.margin = {
    top : 20,
    right : 60,
    bottom : 20,
    left : 30
  };
  this.width = 0;
  this.height = 0;
  this.color;
  this.x;
  this.y;
  this.xAxis;
  this.yAxis;
  this.svg;
  this.root;
  this.duration = 250;
  this.colors = ["#866146", "#DEA972", "#d0743c", "#F0B75E", "#F1EDBE", "#866406", "#DEC472", "#f3743c", "#F4370E"];
  this.colorDomain = ['0010', '1020', '2030', '3040', '4050', '5060', '6070', '7080', '9999'];
  this.maxBarNumber = 20;

  this.containerWidth = $(this.divID).width();
  this.containerHeight = $(this.divID).height();
  this.width = this.containerWidth - this.margin.left - this.margin.right;
  this.height = this.containerHeight - this.margin.top - this.margin.bottom;

  this.svg = d3.select(this.divID).append("svg")
  .attr("width", this.width + this.margin.left + this.margin.right)
  .attr("height", this.height + this.margin.top + this.margin.bottom);
  console.log(this.svg);

  this.root = this.svg.append("g")
  .attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");

  this.color = d3.scale.ordinal().range(this.colors);
  this.color.domain(this.colorDomain);

  this.x = d3.scale.ordinal()
  .rangeRoundBands([ 0, this.width ], .1);
  this.y = d3.scale.linear()
  .range([ this.height, 0 ]);

  this.xAxis = d3.svg.axis()
    .scale(this.x)
    .orient("bottom")
    .tickFormat(function(d) {
      return d;
    })

  this.yAxis = d3.svg.axis()
    .scale(this.y)
    .orient("left")
    .ticks(4)

  this.root.append("g")
    .attr("class", "xaxis")
    .attr("transform", "translate(" + 0 + "," + this.height + ")")
    .call(this.xAxis);

  this.root.append("g")
    .attr("class", "yaxis")
    .attr("transform", "translate(" + 0 + "," + 0 + ")")
    .call(this.yAxis)

  // legend
  var legend = this.root.selectAll(".legend")
  .data(this.color.domain().slice().reverse())
  .enter()
    .append("g").attr("class", "legend")
    .attr("transform", function(d, i) { return "translate(40," + i * 20 + ")";});

  legend.append("rect")
  .attr("x", this.width - 10)
  .attr("width", 18)
  .attr("height", 18)
  .style("fill", this.color);
  legend.append("text")
  .attr("x", this.width - 14)
  .attr("y", 9)
  .attr("dy", ".35em")
  .style("text-anchor", "end")
  .style("font-size", 10)
  .text(function(d) { return d; });
}
D3Chart.prototype.updateAxis = function() {
  // update domain
  this.x.domain(this.dataset.map(function(d) {
    return d.date.substring(10, 14);
  }));
  this.y.domain([ 0, d3.max(this.dataset, function(d) {
    return d.total;
  }) ]);
  // update axis
  this.svg.select(".xaxis").transition().duration(this.duration).call(this.xAxis);
  this.svg.select(".yaxis").transition().duration(this.duration).call(this.yAxis);
}

D3Chart.prototype.updateBar = function() {
  var that = this;
  // bar group
  var barGroup = this.root.selectAll(".barGroup").data(this.dataset);

  barGroup.enter().append("g")
  .attr("class", "barGroup")
  .attr("transform", function(d, i) { return "translate(" + that.x(d.date.substring(10, 14)) + ",0)";});

  barGroup.transition().duration(this.duration)
  .attr("class", "barGroup")
  .attr("transform", function(d, i) { return "translate(" + that.x(d.date.substring(10, 14)) + ",0)";});

  // each bar
  var rect = barGroup.selectAll("rect")
  .data(function(d) { return d.element; });

  rect.enter().append("rect")
    .attr("width", this.x.rangeBand())
    .attr("y", function(d) { return that.y(d.y1); })
    .attr("height", function(d) { return that.y(d.y0) - that.y(d.y1);})
    .style("fill", function(d) { return that.color(d.name);});

  rect.transition().duration(this.duration)
  .attr("width", this.x.rangeBand())
  .attr("y", function(d) { return that.y(d.y1); })
  .attr("height", function(d) { return that.y(d.y0) - that.y(d.y1);})
  .style("fill", function(d) { return that.color(d.name);});
}

D3Chart.prototype.updateChart = function() {
  var that = this;
  this.dataset.forEach(function(d) {
    function findCount(d, c) {
      for ( var i in d) {
        if (d[i].type == c) {
          return parseInt(d[i].count);
        }
      }
      return 0;
    }
    var y0 = 0;
    d.element = that.color.domain().map(function(name) {
      return {
        name : name,
        y0 : y0,
        y1 : y0 += findCount(d.counts, name)
      };
    });
    d.total = d.element[d.element.length - 1].y1;
  });

  this.updateAxis();
  this.updateBar();
}
