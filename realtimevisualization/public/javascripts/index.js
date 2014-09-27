var dataset = [
  {"ageGrade":"1020", "count":40},
  {"ageGrade":"2030", "count":20},
  {"ageGrade":"3040", "count":120},
  {"ageGrade":"4050", "count":10},
  {"ageGrade":"5060", "count":70},
  {"ageGrade":"6070", "count":40},
];

var margin = { top : 50, right : 60, bottom : 20, left : 30 };

var chartSelection = d3.select("#chart1");

var width = chartSelection.style('width').replace("px", "");
var height = chartSelection.style('height').replace("px", "");

// set scale x
var scaleX = d3.scale.ordinal()
  .rangeRoundBands([0, width - (margin.left + margin.right)], 0.2);

// set scale y
var scaleY = d3.scale.linear()
  .range([height - (margin.top + margin.bottom), 0]);

// set x axis
var axisX = d3.svg.axis().scale(scaleX)
  .orient("bottom");

// set y axis
var axisY = d3.svg.axis().scale(scaleY)
  .orient("left");

var svgSelection = chartSelection
  .append("svg")
    .attr("width", width)
    .attr("height", height);

root = this.svgSelection.append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

// x axis element
root.append("g")
  .attr("class", "axisX")
  .attr("transform",
    "translate(" + 0 + "," + (height - (margin.top + margin.bottom)) + ")"
    )
  .call(axisX);

// y axis element
root.append("g")
  .attr("class", "axisY")
  .attr("transform", "translate(" + 0 + "," + 0 + ")")
  .call(axisY);

// update domain
scaleX.domain(dataset.map(function(d) { return d.ageGrade; }));
scaleY.domain([ 0, d3.max(dataset, function(d) { return d.count;})]);

var barGroup = root.selectAll(".barGroup").data(this.dataset);

barGroup.data(dataset).enter()
  .append("rect")
    .attr("width", scaleX.rangeBand())
    .attr("y", function(d) { return scaleY(d.count); })
    .attr("height", function(d) {
      return (height - (margin.top + margin.bottom)) - scaleY(d.count);
    })
    .style("fill", function(d) {
      return d3.rgb(100 - d.count, 100 - d.count, 100 - d.count);
    })
    .attr("transform", function(d) {
      return "translate(" + scaleX(d.ageGrade) + ",0)";
    });
