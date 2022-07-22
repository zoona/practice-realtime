
# 실시간 처리 실습

## 1. 개요

실시간으로 입력되는 데이터를 오픈소스를 이용하여

수집, 처리, 저장하고, 그 결과를 실시간 차트로 시각화 하는 과정을 실습하는 과정

### 1-1. 프로젝트의 목적

* 커피를 주문하면, 주문 내역이 텍스트 파일에 저장된다고 가정

* 커피 주문 데이터를 수집해서 일정 시간 간 주문자의 연령, 주문 방법, 주문한 커피 종류 등의 통계를 산출하여 DB에 저장

* 실시간으로 처리되는 통계를 차트 형태로 시각화

### 1-2. 결과물

`branch:datestring:statistictype`형태의 key로 구분되는 시간 단위 통계 저장
![redis](http://zoona.com/wordpress/wp-content/uploads/2014/09/Screen-Shot-2014-09-29-at-9.51.05-AM.png)

실시간 업데이트 되는 chart
![chart](http://zoona.com/wordpress/wp-content/uploads/2014/09/Screen-Shot-2014-09-29-at-9.44.04-AM.png)

