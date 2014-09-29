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

## 2. 프로젝트 구조

### 2-1. 경로

* datagenerator
  - 샘플 데이터 생성 스크립트
* realtimeprocessing
  - flume redis Sink 및 storm topology 구현
  - java(maven) 프로젝트
* realtimevisualization
  - 데이터 시각화 서버 및 클라이언트 구현


![tree](http://zoona.com/wordpress/wp-content/uploads/2014/09/Screen-Shot-2014-09-29-at-11.02.00-AM.png)

### 2-2. Dependencies
**Maven 설치**
```bash
wget http://mirror.apache-kr.org/maven/maven-3/3.2.3/binaries/apache-maven-3.2.3-bin.tar.gz
tar xvfz apache-maven-3.2.3-bin.tar.gz
```

환경설정
```bash
vi ~/.bash_profile
```
```bash
export M2_HOME=$HOME/apache-maven-3.2.3
PATH=$PATH:$HOME:$M2_HOME/bin
```

```bash
. ~/.bash_profile
```

**node.js, express.js, bower.js 설치**
```
curl -sL https://rpm.nodesource.com/setup | bash -
yum install -y nodejs
sudo npm install express express-generator -g
sudo npm install bower
```

**redis 설치**

```bash
wget http://download.redis.io/releases/redis-2.8.17.tar.gz
tar xvfz redis-2.8.17.tar.gz
cd redis-2.8.17
make
```

환경설정

```bash
vi ~/.bash_profile
```

```bash
PATH=$PATH:$HOME/redis-2.8.17/src
```

```bash
. ~/.bash_profile
```

실행

```bash
redis-server ~/redis-2.8.17/redis.conf
```

```bash
redis-client
```

**flume 설치**

```bash
wget http://apache.tt.co.kr/flume/1.5.0.1/apache-flume-1.5.0.1-bin.tar.gz
tar xvfz apache-flume-1.5.0.1-bin.tar.gz
```

환경설정

```bash
vi ~/.bash_profile
```

```bash
PATH=$PATH:$HOME/apache-flume-1.5.0.1-bin/bin
```

```bash
. ~/.bash_profile
```

**pip, redis python library 설치**

```bash
easy_install pip
pip install redis
```

### 2-1. 데이터

python 스크립트

미리 정해놓은 데이터셋으로 부터 무작위로 추출하여 커피 주문 이벤트를 생성

텍스트 파일에 JSON 형태로 쓰여지도록 작성

```python
import random, time, json
import redis

# sample dataset

branches = [
  'jeongja', 'seohyeon', 'sunae', 'migeum'
];

coffeeTypes = [
  'Espresso', 'Espresso Macchiato', 'Espresso con Panna',
  'Caffe Latte', 'Flat White', 'Cafe Breve',
  'Cappuccino', 'Caffe Mocha', 'Americano']

paymentMethods = ['Cash', 'Credit', 'Debit'];

customerAgeGrades = [
  '0010', '1020', '2030', '3040', '4050',
  '5060', '6070', '7080', '8090', '9999'];

# order time interval
timeInterval = 1 * 0.25
# for testing, log
count = 0
orderCount = 0
limit = 10

# output path
outputFile = open("/tmp/CoffeeOrders.txt", "w", 0)
logFile = open("/tmp/CoffeeLog.txt", "w", 0)

# generating
while 1:
  branch = branches[random.randint(0, 3)];
  quantity = random.randint(1, 10)
  paymentMethod = paymentMethods[random.randint(0, 2)]
  customerAgeGrade = customerAgeGrades[random.randint(0, 9)]
  orders = []
  for i in range(quantity):
    orders.append(coffeeTypes[random.randint(0, 8)])
  #
  jsonString = json.JSONEncoder().encode({
    "branch": branch,
    "orders": orders,
    "paymentMethod": paymentMethod,
    "customerAgeGrade": customerAgeGrade
  })
  # write out
  outputFile.write(jsonString + "\n")
  time.sleep(timeInterval)
  # print and log
  print jsonString + "\n";
  count += 1
  orderCount += len(orders)
  logFile.write("row : " + str(count) + ", count : " + str(orderCount) + "\n")
  #if count > limit:
  #	break;

outputFile.close()
logFile.close()
print "generate Complete"
```

CoffeeOrders.txt
![json](http://zoona.com/wordpress/wp-content/uploads/2014/09/Screen-Shot-2014-09-29-at-12.57.38-PM.png)

### 2-3. 수집

flume을 이용하여 수집 및 Queue에 list 형태로 저장 (Redis를 Queue로 사용)

생성되어지는 커피 주문 이벤트를 flume exec source를 이용해서 수집하고
```
tail -f CoffeeOrders.txt
```

Redis에 저장할 수 있도록 RedisSink를 구현하여 구동

### 2-3. 처리

Storm을 이용해서 Redis list에 수집되는 데이터를 순차적으로 불러와서 통계를 내고,

처리된 실시간 통계는 Redis에 Publish

처리에 필요한 RedisSpout, CountBolt를 구현하여 구동

### 2-4. 저장

Storm에서 처리되는 실시간 통계를 주기적으로 저장

Redis에 hash형태로 저장하는 SaveBolt를 구현하고,

CountBolt에서 주기적으로 SaveBolt에 emit하도록 구현

### 2-5. 서비스 및 시각화

Storm의 CountBolt에서 publish하는 실시간 통계를 subscribe해서

클라이언트에게 전송해주는 기능과,

과거 주기 데이터를 쿼리해서 전송하는 기능의 서비스를 express.js를 사용해서 구현

클라이언트 view에서는 과거 주기데이터와, 수신되는 실시간 데이터를 이용해

실시간으로 업데이트 되는 차트를 보여주도록 d3.js를 이용해 구현

## 3. 사용하는 오픈소스 패키지

### 3-1. Flume

![logo](http://flume.apache.org/_static/flume-logo.png)

다양한 소스에서 발생한 대량의 로그 데이터를 중앙 데이터 스토어로 효과적으로 수집 집계(aggregating)하거나 이동시킬 수 있는 신뢰할수있는 분산 시스템

스트림 지향의 데이터 플로우를 기반으로 하며 지정된 모든 서버로 부터 로그를 수집한 후 하둡 HDFS와 같은 중앙 저장소에 적재하여 분석하는 시스템을 구축해야 할 때 적합

데이터 소스를 커스터마이징 할 수 있기 때문에 로그 데이터 수집에 제한되지 않고, 소셜미디어 데이터, 이메일 메세지등 다량의 이벤트 데이터를 전송하는데에 사용할 수 있다

Goals

* Reliability - 장애가 발생시 로그의 유실없이 전송할 수 있는 기능
* Scalability - Agent의 추가 및 제거가 용이
* Manageability - 간결한 구조로 관리가 용이
* Extensibility - 새로운 기능을 쉽게 추가할 수 있음

Core Concepts

* Event : Flume이 source로부터 destination까지 전송하는 데이터 단위 (byte payload)
* Flow : Event의 흐름
* Client : Agent로 데이터를 전달하는 interface의 구현
* Agent: Event를 수집, 저장, 전달하는 독립된 프로세스
* Source : Event를 수집해서 Channel에 저장
* Sink : Channel로 부터 가져와서 목적지, 혹은 다른 Agent로 전달
* Channel : Source에서 Sink로 보내는 Event의 임시 저장소


![data flow model](https://flume.apache.org/_images/UserGuide_image00.png)

Multi Agent Flow

* Agent와 Agent간 Event 전달하는 구조가 가능
* Avro Sink에서 목적 Agent의 Avro Source로 전달

![setting multi-agent flow](https://flume.apache.org/_images/UserGuide_image03.png)

Consolidation

* 많은수의 로그 생성 클라이언트가 저장 서브시스템에 부착된 적은수의 로그 수집 에이전트에 데이터를 보내는 것

![consolidation](https://flume.apache.org/_images/UserGuide_image02.png)

Multiplexing

* 이벤트를 하나 이상의 목적지에 전달하는 멀티 플렉싱(multiplexing)을 지원한다.
* 플로우 멀티플렉서를 정의함으로서 이벤트를 복사(replicate)하거나 선택해 하나 또는 그이상의 체널에 해당 이벤트를 전달할 수 있다.

![Multiplexing the flow](https://flume.apache.org/_images/UserGuide_image01.png)

Reliability and Failure Handling

Reliability

* Channel-based transaction을 통해 메세지 전송의 신뢰성 제공


* Agent 1 : Tx begin
* Agent 1 : Channel take event
* Agent 1 : Sink send
* Agent 2 : Tx begin
* Agent 2 : Channel put
* Agent 2 : Tx commit
* Agent 1 : Tx commit

![tx](https://blogs.apache.org/flume/mediaresource/a15d9347-da9e-4824-b45f-6c00f0720590)

Failover

* Agent간 전송에 장애가 발생했을 경우 전송측 Agent에서 이벤트를 버퍼링
* 장애가 복구 되면 다시 Buffering된 이벤트 전송

![failover](https://blogs.apache.org/flume/mediaresource/ac9d1c83-1089-4730-9546-fe8de509b34c)

Source

외부 클라이언트나 다른 Sink로부터 데이터를 읽고 설정된 Channel에 저장

* Avro
* Thrift
* Exec
* JMS
* Spooling Directory
* Sequence Generator
* Syslog TCP
* Multiport Syslog TCP
* Syslog UDP
* HTTP Source
* Custom

Sink

목적지로 이벤트 송신

* HDFS
* Avro
* Thrift
* IRC
* File Roll
* Null
* HBase
* AsyncHBase
* MorphlineSolr
* ElasticSearch
* Custom

Channel

Source와 Sink 사이의 중간 컴퍼넌트로 flow 신뢰성을 보장

* File
* Memory
* JDBC

Configuration

```bash
agent.sources = spoolDirSource
agent.channels = memoryChannel
agent.sinks = fileRollSink

agent.channels.memoryChannel.type = memory
agent.channels.memoryChannel.capacity = 1000

agent.sources.spoolDirSource.type = spoolDir
agent.sources.spoolDirSource.spoolDir = /home/hadoop/spool
agent.sources.spoolDirSource.channels = memoryChannel

agent.sinks.fileRollSink.type = file_roll
agent.sinks.fileRollSink.sink.directory = /home/hadoop/fileroll
agent.sinks.fileRollSink.channel = memoryChannel
```

Channel Selector

Source에 적용되어 Fan out 적용

* Replicating (default)
* Multiplexing
* Custom

```c
# replicating
a1.sources = r1
a1.channels = c1 c2 c3
a1.source.r1.selector.type = replicating
a1.source.r1.channels = c1 c2 c3
a1.source.r1.selector.optional = c3
```

```c
# multiplexing
a1.sources = r1
a1.channels = c1 c2 c3 c4
a1.sources.r1.selector.type = multiplexing
a1.sources.r1.selector.header = state
a1.sources.r1.selector.mapping.CZ = c1
a1.sources.r1.selector.mapping.US = c2 c3
a1.sources.r1.selector.default = c4
```

Sink Processor

* Default Sink Processor
* Failover Sink Processor
* Load balancing Sink Processor

```c
# failover
a1.sinkgroups = g1
a1.sinkgroups.g1.sinks = k1 k2
a1.sinkgroups.g1.processor.type = failover
a1.sinkgroups.g1.processor.priority.k1 = 5
a1.sinkgroups.g1.processor.priority.k2 = 10
a1.sinkgroups.g1.processor.maxpenalty = 10000
```

```c
# load balancing
a1.sinkgroups = g1
a1.sinkgroups.g1.sinks = k1 k2
a1.sinkgroups.g1.processor.type = load_balance
a1.sinkgroups.g1.processor.backoff = true
a1.sinkgroups.g1.processor.selector = random
#a1.sinkgroups.g1.processor.selector = round_robin
```

```c
# full example
# channels
agent.channels = mem_channel
agent.channels.mem_channel.type = memory

# sources
agent.sources = event_source
agent.sources.event_source.type = avro
agent.sources.event_source.bind = 127.0.0.1
agent.sources.event_source.port = 10000
agent.sources.event_source.channels = mem_channel

# sinks
agent.sinks = main_sink backup_sink

agent.sinks.main_sink.type = avro
agent.sinks.main_sink.hostname = 127.0.0.1
agent.sinks.main_sink.port = 10001
agent.sinks.main_sink.channel = mem_channel

agent.sinks.backup_sink.type = avro
agent.sinks.backup_sink.hostname = 127.0.0.1
agent.sinks.backup_sink.port = 10002
agent.sinks.backup_sink.channel = mem_channel

# sink groups
agent.sinkgroups = failover_group
agent.sinkgroups.failover_group.sinks = main_sink backup_sink
agent.sinkgroups.failover_group.processor.type = failover
agent.sinkgroups.failover_group.processor.priority.main_sink = 10
agent.sinkgroups.failover_group.processor.priority.backup_sink = 5
```

Event Serializer

file_roll, hdfs sink에 지원

event의 body를 output stream으로 출력하거나 event를 serialize

* Body Text Serializer
* Avro Event Serializaer

```
# Body Text
a1.sinks = k1
a1.sinks.k1.type = file_roll
a1.sinks.k1.channel = c1
a1.sinks.k1.sink.directory = /var/log/flume
a1.sinks.k1.sink.serializer = text
a1.sinks.k1.sink.serializer.appendNewline = false
```

```
# Avro Event
a1.sinks.k1.type = hdfs
a1.sinks.k1.channel = c1
a1.sinks.k1.hdfs.path = /flume/events/%y-%m-%d/%H%M/%S
a1.sinks.k1.serializer = avro_event
a1.sinks.k1.serializer.compressionCodec = snappy
```


Interceptor

Source 설정에 적용
한개의 Source에 여러 Interceptor 적용가능
Tagging, filtering, routing에 사용될 수 있다


* Timestamp Interceptor
* Host Interceptor
* Static Interceptor
* UUID Interceptor
* Morphline Interceptor
* Regex Filtering Interceptor
* Regex Extractor Interceptor

```
a1.sources = r1
a1.sinks = k1
a1.channels = c1
a1.sources.r1.interceptors = i1 i2
a1.sources.r1.interceptors.i1.type = org.apache.flume.interceptor.HostInterceptor$Builder
a1.sources.r1.interceptors.i1.preserveExisting = false
a1.sources.r1.interceptors.i1.hostHeader = hostname
a1.sources.r1.interceptors.i2.type = org.apache.flume.interceptor.TimestampInterceptor$Builder
a1.sinks.k1.filePrefix = FlumeData.%{CollectorHost}.%Y-%m-%d
a1.sinks.k1.channel = c1
```

**Practice**

spoolDir source - memoryChannel - file_roll sink
* spoolDir Source로 /tmp/spool 디렉토리 감시 후 생성 파일 수집
* memory channel에 저장
* fileRoll Sink로 /tmp/fileroll에 저장

```bash
agent.sources = spoolDirSource
agent.channels = memoryChannel
agent.sinks = fileRollSink

agent.channels.memoryChannel.type = memory
agent.channels.memoryChannel.capacity = 1000

agent.sources.spoolDirSource.type = spoolDir
agent.sources.spoolDirSource.spoolDir = /tmp/spool
agent.sources.spoolDirSource.channels = memoryChannel

agent.sinks.fileRollSink.type = file_roll
agent.sinks.fileRollSink.sink.directory = /tmp/fileroll
agent.sinks.fileRollSink.channel = memoryChannel
```

```bash
vi case01.properties
# 내용 입력

mkdir /tmp/fileroll
mkdir /tmp/spool

flume-ng agent --conf-file=case01.conf -n agent

ls -al /tmp/fileroll
echo hello > /tmp/spool/test.txt
ls -al /tmp/fileroll
ls -al /tmp/spool
```

exec source - fileChannel - hdfs sink
* tail로 buffer 파일의 내용을 수집
* file channel에 저장
* hdfs에 저장

```bash
agent.sources = execSource
agent.channels = fileChannel
agent.sinks = hdfsSink

agent.sources.execSource.type = exec
agent.sources.execSource.command = tail -f /home/hadoop/buffer
agent.sources.execSource.batchSize = 5
agent.sources.execSource.channels = fileChannel
agent.sources.execSource.interceptors = timestampInterceptor
agent.sources.execSource.interceptors.timestampInterceptor.type = timestamp

agent.sinks.hdfsSink.type = hdfs
agent.sinks.hdfsSink.hdfs.path = hdfs://bigdata05-01/flume/%Y%m%d-%H%M%S
agent.sinks.hdfsSink.hdfs.fileType = DataStream
agent.sinks.hdfsSink.hdfs.writeFormat = Text
agent.sinks.hdfsSink.channel = fileChannel

agent.channels.fileChannel.type = file
agent.channels.fileChannel.checkpointDir = /tmp/flume/checkpoint
agent.channels.fileChannel.dataDirs = /tmp/flume/data
```

```bash
vi case02.properties
# 내용 입력

touch /tmp/buffer

su - hdfs
hadoop fs -mkdir /flume
hadoop fs -chmod 777 /flume
exit

flume-ng agent --conf-file=case02.conf -n agent

echo hello >> /tmp/buffer

hadoop fs -ls /flume
hadoop fs -ls /flume/20140929-131158
hadoop fs -cat /flume/20140929-131158/FlumeData.1411963919649
```

### 3-2. Storm

### 3-3. Redis

![logo](http://download.redis.io/logocontest/82.png)

Overview

* `RE`mote `DI`ctionary `S`erver
* Open source NoSQL
* In memory database

Usage

* Job Queue
* Session store
* Realtime Ranking

장점

* Speed
* Command Level Atomic Operation
* Lots of Client Lib(java, node.js, objective-c, c#, erlang, ruby ...)

단점

* Snapshot IO overhead
* whole dataset be loaded into main memory

* RDB - 메모리의 snapshot을 떠서 그대로 저장
  * restart 시간이 빠름름
  * snapshot 추출시간이 오래거림
* AOF - log file append
  * log write 속도가 빠름
  * 디스크 용량


**Data Types**

String

* contain any kind of data
* value < 512Mb

Lists

* list of strings
* sorted by insertion order
* length < 2^32 - 1

```bash
LPUSH mylist a   # now the list is "a"
LPUSH mylist b   # now the list is "b","a"
RPUSH mylist c   # now the list is "b","a","c" (RPUSH was used this time)
```

Sets

* unordered collection of strings
* Adding the same element multiple times will result in a set having a single copy of this element
* you can do unions, intersections, differences of sets in very short time
* length < 2^32 - 1

Hashes

* maps between string fields and string values
* used mainly to represent objects

```bash
HMSET user:1000 username antirez password P1pp0 age 34
HGETALL user:1000
HSET user:1000 password 12345
HGETALL user:1000
```

Sorted sets

* every member of a Sorted Set is associated with score
* you can add, remove, or update elements in a very fast way
* you can also get ranges by score or by rank (position) in a very fast way

Pub/Sub

implement the `Publish/Subscribe messaging paradigm`

* `SUBSCRIBE` will listen to a channel
* `PUBLISH` allows you to push a message into a channel

Commands

http://redis.io/commands

### 3-3. Node.js

node.js is

* Server-side Javascript
* Built on Google’s V8
* Evented, non-blocking I/O.
* CommonJS module system
* 8000 lines of C/C++, 2000 lines of Javascript


node.js is not

* Web Framework
* For Beginners
* Multi-threaded

적용 예

* Scalable web servers for web apps
* Websocket Server
* RESTful API Server
* Realtime Data apps
* Fast File Upload Client
* Ad Server
 

node programming model

* Event-Driven
* Non-Blocking I/O Model
* Single-Thread
* No DOM implementation provided


Blocking vs Non-Blocking


Event Loop


NPM

----

Express.js

* Easy route URLs to callbacks
* Middleware
* Environment based configuration
* Redirection helpers
* File Uploads


## 4. 실습

### 4-1. 프로젝트 설정

eclipse에서 Maven Project를 생성하거나

콘솔에서 다음과 같이 Maven Project를 생성

```bash
$ mvn archetype:create -DgroupId=bigdata.practice -DartifactId=realtime-processing
```

----

pom.xml

* jdk version
* output jar에 dependency 포함

```xml
<build>
  <plugins>
    <!-- jdk version -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <source>1.6</source>
        <target>1.6</target>
      </configuration>
    </plugin>
    <!-- include dependency -->
    <plugin>
      <artifactId>maven-assembly-plugin</artifactId>
      <configuration>
        <archive>
          <manifest>
            <mainClass>fully.qualified.MainClass</mainClass>
          </manifest>
        </archive>
        <descriptorRefs>
          <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
      </configuration>
      <executions>
        <execution>
          <id>make-assembly</id>
          <phase>package</phase>
          <goals>
            <goal>single</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

### 4-2. 데이터 생성

미리 정의해놓은 데이터셋에서

```python
coffeeTypes = [
  'Espresso', 'Espresso Macchiato', 'Espresso con Panna',
  'Caffe Latte', 'Flat White', 'Cafe Breve',
  'Cappuccino', 'Caffe Mocha', 'Americano']

paymentMethods = ['Cash', 'Credit', 'Debit'];

customerAgeGrades = ['0010', '1020', '2030', '3040', '4050', '5060', '6070', '7080', '8090', '9999'];

```

무작위로 필드를 뽑아 파일에 기록

```python
while 1:
  quantity = random.randint(1, 10)
  paymentMethod = paymentMethods[random.randint(0, 2)];
  customerAgeGrade = customerAgeGrades[random.randint(0, 9)];
  orders = []
  for i in range(quantity):
    orders.append(coffeeTypes[random.randint(0, 8)])

  jsonString = json.JSONEncoder().encode({
    "orders": orders,
    "paymentMethod": paymentMethod,
    "customerAgeGrade": customerAgeGrade
  })

  outputFile.write(jsonString + "\n")
```

```json
{"customerAgeGrade": "1020", "paymentMethod": "Cash", "orders": ["Americano", "Espresso"]}
{"customerAgeGrade": "3040", "paymentMethod": "Cash", "orders": ["Espresso Macchiato"]}
{"customerAgeGrade": "2030", "paymentMethod": "Credit", "orders": ["Caffe Mocha", "Cafe Breve"]}
{"customerAgeGrade": "0010", "paymentMethod": "Credit", "orders": ["Caffe Latte", "Flat White", "Espresso", "Caffe Mocha"]}
{"customerAgeGrade": "3040", "paymentMethod": "Debit", "orders": ["Caffe Mocha", "Espresso", "Caffe Mocha", "Flat White", "Espresso", "Caffe Mocha", "Caffe Latte", "Espresso con Panna", "Flat White"]}
{"customerAgeGrade": "8090", "paymentMethod": "Cash", "orders": ["Caffe Latte", "Espresso", "Caffe Mocha", "Espresso con Panna", "Espresso Macchiato", "Flat White", "Cafe Breve", "Espresso con Panna", "Cafe Breve", "Cafe Breve"]}
{"customerAgeGrade": "5060", "paymentMethod": "Credit", "orders": ["Caffe Mocha", "Flat White", "Caffe Latte", "Caffe Mocha", "Caffe Mocha"]}
{"customerAgeGrade": "4050", "paymentMethod": "Debit", "orders": ["Espresso", "Espresso"]}
{"customerAgeGrade": "4050", "paymentMethod": "Credit", "orders": ["Espresso", "Cafe Breve", "Espresso", "Cappuccino", "Caffe Mocha", "Espresso", "Espresso con Panna"]}
```

### 4-3. 수집

#### 4-3-1. Redis Sink 작성

```java
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

```

#### 4-3-2. Properties 작성 및 실행

toRedisQueue.properties

```bash
agent.sources = exec
agent.channels = mem
agent.sinks = redis

# channel
agent.channels.mem.type = memory
agent.channels.mem.capacity = 1000
agent.channels.mem.transactionCapacity = 100

# source
agent.sources.exec.type = exec
agent.sources.exec.command = tail -100f /tmp/CoffeeOrders.txt
agent.sources.exec.batchSize = 1000
agent.sources.exec.channels = mem

# sink
agent.sinks.redis.channel = mem
agent.sinks.redis.type = practice.bigdata.realtimeprocessing.flume.sink.RedisSink
agent.sinks.redis.redisHost = localhost
agent.sinks.redis.redisPort = 6379
agent.sinks.redis.redisKey = CoffeeOrderQueue

```

flume-ng 실행

```bash
flume-ng agent --conf-file realtiprocessing/conf/toRedisQueue.properties --conf realtimeprocessing/conf --classpath realtimeprocessing/target/realtimeprocessing-0.0.1-SNAPSHOT-jar-with-dependencies.jar --name agent
```

### 4-4. 처리

#### 4-4-1. Redis Spout 작성

```java
public class RedisSpout extends BaseRichSpout {

  private static final long serialVersionUID = 1L;
  Logger logger = LoggerFactory.getLogger(this.getClass());
  private SpoutOutputCollector collector;
  // redis info
  private String redisHost;
  private int redisPort;
  private String redisKey = "CoffeeOrderQueue";
  private Jedis jedis;

  public RedisSpout(String redisHost, int redisPort) {
    this.redisHost = redisHost;
    this.redisPort = redisPort;
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream("redis_input_stream", new Fields("branch", "customerAgeGrade",
        "paymentMethod", "orders"));

  }

  @Override
  public void open(Map conf, TopologyContext context,
      SpoutOutputCollector collector) {
    this.collector = collector;
    jedis = new Jedis(redisHost, redisPort);
    jedis.connect();

  }

  @Override
  public void nextTuple() {
    String valueString = jedis.lpop(redisKey);
    if (valueString == null) {
      Utils.sleep(5);
    } else {
      CoffeeOrder coffeeOrder = new CoffeeOrder(valueString);
      collector.emit("redis_input_stream",
          new Values(coffeeOrder.getBranch(), coffeeOrder.getCustomerAgeGrade(),
              coffeeOrder.getPaymentMethod(), coffeeOrder.getOrders()));
    }
  }
}

```

#### 4-4-2. CounterBolt 작성

```java
public class CounterBolt extends BaseRichBolt {

  private static final long serialVersionUID = 1L;
  Logger logger = LoggerFactory.getLogger(this.getClass());
  OutputCollector collector;
  Jedis jedis;
  String redisHost;
  int redisPort;
  SimpleDateFormat dateFormatter;
  int saveInterval = 30;
  String lastDateString = "";
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
    String branch = input.getStringByField("branch");
    String customerAgeGrade = input.getStringByField("customerAgeGrade");
    String paymentMethod = input.getStringByField("paymentMethod");
    List<String> orders = (List<String>) input.getValueByField("orders");
    // get statistics
    // customerAgeGrade is key
    Statistics statistics = statisticsMap.get(branch);
    if (statistics == null) {
      statistics = new Statistics();
    }
    // increase counts
    increaseMapValue(statistics.getCustomerAgeGrades(), customerAgeGrade);
    increaseMapValue(statistics.getPaymentMethods(), paymentMethod);
    for (String order : orders)
      increaseMapValue(statistics.getOrders(), order);
    statisticsMap.put(branch, statistics);
    // emit to SaveBolt (each 'saveInterval' seconds)
    String dateString = getDateString();

    logger.info(dateString + "/ " + lastDateString + " : " + dateString.equals(lastDateString));
    if (!dateString.equals(lastDateString)) {
      collector.emit("counter_stream", new Values(branch, dateString, statistics));
      statisticsMap.clear();
      lastDateString = dateString;
    }

    publishStatistics(branch, dateString, statistics);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declareStream("counter_stream", new Fields("branch", "dateString",
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

  public void publishStatistics(String branch, String dateString,
      Statistics statistics) {
    JsonArray customerAgeGradesArray = new JsonArray();
    for (String key : statistics.getCustomerAgeGrades().keySet()) {
      JsonObject count = new JsonObject();
      count.addProperty("type", key);
      count.addProperty("count", statistics.getCustomerAgeGrades().get(key));
      customerAgeGradesArray.add(count);
    }
    JsonObject body = new JsonObject();
    body.addProperty("branch", branch);
    body.addProperty("date", dateString);
    body.add("counts", customerAgeGradesArray);

    JsonObject packet = new JsonObject();
    packet.addProperty("header", "PUBLISH_STATISTICS");
    packet.add("body", body);

    jedis.publish("CHANNEL_REDIS_PUBSUB", packet.toString());
  }
}

```
#### 4-4-3. Save Bolt 작성

```java
public class SaveBolt extends BaseRichBolt {

  private static final long serialVersionUID = 1L;
  Logger logger = LoggerFactory.getLogger(this.getClass());
  OutputCollector collector;
  Jedis jedis;

  String redisHost;
  int redisPort;

  public SaveBolt(String redisHost, int redisPort) {
    this.redisHost = redisHost;
    this.redisPort = redisPort;
  }

  @Override
  public void cleanup() {
    super.cleanup();
    jedis.disconnect();
  }

  @Override
  public void prepare(Map stormConf, TopologyContext context,
      OutputCollector collector) {
    this.collector = collector;
    jedis = new Jedis(redisHost, redisPort);
    jedis.connect();
  }

  @Override
  public void execute(Tuple input) {
    String branch = input.getStringByField("branch");
    String dateString = input.getStringByField("dateString");
    Statistics statistics = (Statistics) input.getValueByField("statistics");

    String key = branch + ":" + dateString + ":CustomerAgeGrades";
    increaseCounts(key, statistics.getCustomerAgeGrades());
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // TODO Auto-generated method stub

  }

  private void increaseCounts(String hashKey, Map<String, Long> map) {
    Set<String> keys = map.keySet();
    for(String key : keys) {
      jedis.hincrBy(hashKey, key, map.get(key));
    }
  }
}

```
#### 4-4-4. Topology 작성 및 실행

```java
public class CoffeeTopology {
  private static String redisHost = "localhost";
  private static int redisPort = 6379;
  private static String topologyID = "CoffeeTopology";

  public static void main(String[] args) {
    // build topology
    TopologyBuilder builder = new TopologyBuilder();
    builder.setSpout("redisSpout", new RedisSpout(redisHost, redisPort));
    builder.setBolt("counterBolt", new CounterBolt(redisHost, redisPort), 20)
        .fieldsGrouping("redisSpout", "redis_input_stream",
            new Fields("branch"));
    builder.setBolt("saveBolt", new SaveBolt(redisHost, redisPort), 5)
        .shuffleGrouping("counterBolt", "counter_stream");
    // config
    Config conf = new Config();
    conf.setDebug(true);
    if (args.length > 1) {
      topologyID = args[1];
    }
    // submit
    SubmitToLocal(conf, builder);
    // SubmitToCluster(conf, builder);
  }

  public static void SubmitToLocal(Config conf, TopologyBuilder builder) {
    LocalCluster cluster = new LocalCluster();
    cluster.submitTopology(topologyID, conf, builder.createTopology());
  }

  public static void SubmitToCluster(Config conf, TopologyBuilder builder) {
    try {
      StormSubmitter.submitTopology(topologyID, conf, builder.createTopology());
    } catch (AlreadyAliveException e) {
      e.printStackTrace();
    } catch (InvalidTopologyException e) {
      e.printStackTrace();
    }
  }
}
```

5. 실행 scripts

clone source
```bash
git clone https://github.com/zoona/practice-realtimeprocessing.git
cd practice-realtimeprocessing
```

data generator
```bash
python datagenerator/generate.py
```

flume
```bash
flume-ng agent --conf-file realtimeprocessing/conf/toRedisQueue.properties --conf realtimeprocessing/conf --classpath realtimeprocessing/target/realtimeprocessing-0.0.1-SNAPSHOT-jar-with-dependencies.jar --name agent
```

storm
```bash
storm jar realtimeprocessing/target/realtimeprocessing-0.0.1-SNAPSHOT-jar-with-dependencies.jar practice.bigdata.realtimeprocessing.storm.CoffeeTopology
```

service
```bash
cd realtimevisualization
npm install
bower install
npm start
```
