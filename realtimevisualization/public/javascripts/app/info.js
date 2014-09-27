'use strict';

var info = angular.module('app', []);

info.controller('infoCtrl', function($scope, $window) {
  $scope.name = "zooNa";
});

info.directive('infoBox', function() {
  return {
    link: function(scope, elem, attr) {

      elem.html("<b>name : " + scope.data + "</b><br>");
      scope.$watch("data", function(oldVal, newVal) {
        console.log("ccc");
        elem.html("<b>name : " + scope.data + "</b><br>");
      });

      elem.bind("click", function(e) {
        scope.data = scope.data + "!";
        scope.$apply();
      });

      elem.bind('mouseenter', function(e) {
        elem.css('background-color', 'yellow');
      });

      elem.bind('mouseleave', function(e) {
        elem.css('background-color', 'white');
      });

      elem.bind('mousedown', function(e) {
        elem.css('background-color', 'red');
      });

      elem.bind('mouseup', function(e) {
        elem.css('background-color', 'white');
      });
    },
    restrict: 'E',
    scope: { data: '=' }
  };
});
