import random, time, json

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
timeInterval = 1 * 0.025
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
