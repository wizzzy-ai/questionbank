package com.example.questionbank;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class QuestionSeeder {
	private final QuestionRepository questionRepository;

	public QuestionSeeder(QuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	@Order(10)
	@EventListener(ApplicationReadyEvent.class)
	public void seedQuestionsIfNeeded() {
		long existing = questionRepository.count();
		if (existing >= 100) {
			return;
		}

		Set<String> existingFingerprints = new HashSet<>(questionRepository.findAllFingerprints());
		Set<String> seenInSeed = new HashSet<>();
		List<Question> toInsert = new ArrayList<>();

		for (Question q : buildSeedQuestions()) {
			String fp = QuestionFingerprint.compute(q);
			if (!seenInSeed.add(fp)) {
				continue;
			}
			if (existingFingerprints.contains(fp)) {
				continue;
			}
			q.setFingerprint(fp);
			toInsert.add(q);
		}

		if (!toInsert.isEmpty()) {
			questionRepository.saveAll(toInsert);
		}
	}

	private static List<Question> buildSeedQuestions() {
		List<Question> q = new ArrayList<>();

		q.add(new Question("Which keyword is used to inherit a class in Java?", "extends", "implements", "inherits", "super", "A", "Java"));
		q.add(new Question("Which data type is used to create a variable that should store text?", "int", "String", "boolean", "double", "B", "Java"));
		q.add(new Question("Which method is the entry point of a Java program?", "start()", "main()", "run()", "init()", "B", "Java"));
		q.add(new Question("Which operator is used to compare two values for equality?", "=", "==", "===", "!=", "B", "Java"));
		q.add(new Question("Which of these is NOT a Java primitive type?", "int", "boolean", "String", "double", "C", "Java"));
		q.add(new Question("What is the default value of an uninitialized int field in a class?", "0", "null", "undefined", "1", "A", "Java"));
		q.add(new Question("Which keyword is used to prevent a variable from being modified?", "final", "static", "private", "const", "A", "Java"));
		q.add(new Question("Which collection does NOT allow duplicate elements?", "List", "Set", "ArrayList", "LinkedList", "B", "Java"));
		q.add(new Question("Which exception is unchecked?", "IOException", "SQLException", "NullPointerException", "ClassNotFoundException", "C", "Java"));
		q.add(new Question("Which keyword is used to handle exceptions?", "catch", "throw", "try", "All of the above", "D", "Java"));

		q.add(new Question("Which interface is implemented by ArrayList?", "Map", "List", "Set", "Queue", "B", "Java"));
		q.add(new Question("Which interface is implemented by HashMap?", "List", "Set", "Map", "Queue", "C", "Java"));
		q.add(new Question("What does JVM stand for?", "Java Variable Machine", "Java Virtual Machine", "Java Verified Machine", "Java Visual Machine", "B", "Java"));
		q.add(new Question("What does JDK include?", "Only JVM", "Only JRE", "JRE + development tools", "Only compiler", "C", "Java"));
		q.add(new Question("Which keyword refers to the current object?", "this", "super", "self", "current", "A", "Java"));
		q.add(new Question("Which keyword calls the parent class constructor?", "this()", "parent()", "super()", "base()", "C", "Java"));
		q.add(new Question("Which access modifier makes a member visible only within the same class?", "public", "protected", "private", "default", "C", "Java"));
		q.add(new Question("Which access modifier makes a member visible within the same package (and not outside by default)?", "package-private (no modifier)", "private", "public", "protected", "A", "Java"));
		q.add(new Question("Which statement is used to stop a loop immediately?", "continue", "break", "stop", "exit", "B", "Java"));
		q.add(new Question("Which statement skips the current loop iteration?", "continue", "break", "skip", "pass", "A", "Java"));

		q.add(new Question("Which of these creates an object in Java?", "new MyClass()", "MyClass.new()", "create MyClass()", "class MyClass()", "A", "Java"));
		q.add(new Question("Which of these is a valid way to declare an array?", "int a[] = new int[5];", "int a = new int[5];", "array int a[5];", "int a(5);", "A", "Java"));
		q.add(new Question("Which loop is guaranteed to execute at least once?", "for", "while", "do-while", "foreach", "C", "Java"));
		q.add(new Question("Which of these is used for multi-threading?", "extends Thread", "implements Runnable", "ExecutorService", "All of the above", "D", "Java"));
		q.add(new Question("Which method starts a thread?", "run()", "start()", "begin()", "init()", "B", "Java"));
		q.add(new Question("Which package contains String class?", "java.util", "java.lang", "java.io", "java.net", "B", "Java"));
		q.add(new Question("What is String in Java?", "Mutable", "Immutable", "Both", "None", "B", "Java"));
		q.add(new Question("Which class is used to create mutable strings?", "String", "StringBuilder", "String", "char[]", "B", "Java"));
		q.add(new Question("Which is thread-safe for mutable strings?", "StringBuilder", "StringBuffer", "String", "StringJoiner", "B", "Java"));
		q.add(new Question("What does 'static' mean for a method?", "Belongs to the class", "Belongs to the object", "Cannot be overridden", "Cannot be called", "A", "Java"));

		q.add(new Question("Which is true about interfaces in Java?", "They can have constructors", "They can have private fields", "They can have default methods", "They cannot have methods", "C", "Java"));
		q.add(new Question("Which keyword is used to implement an interface?", "extends", "implements", "inherit", "instanceof", "B", "Java"));
		q.add(new Question("Which keyword is used to create an instance of an inner class from outside?", "outer.new Inner()", "new outer.Inner()", "outer.Inner()", "new Inner()", "A", "Java"));
		q.add(new Question("What is the size of int in Java?", "8-bit", "16-bit", "32-bit", "64-bit", "C", "Java"));
		q.add(new Question("What is the size of long in Java?", "8-bit", "16-bit", "32-bit", "64-bit", "D", "Java"));
		q.add(new Question("Which literal suffix indicates a long?", "f", "d", "l or L", "s", "C", "Java"));
		q.add(new Question("Which literal suffix indicates a float?", "f or F", "d or D", "l or L", "b or B", "A", "Java"));
		q.add(new Question("Which keyword is used to create a constant?", "const", "final", "static", "immutable", "B", "Java"));
		q.add(new Question("Which exception is thrown when dividing by zero for integers?", "ArithmeticException", "NullPointerException", "NumberFormatException", "IOException", "A", "Java"));
		q.add(new Question("Which API is used for reading input from console?", "Scanner", "Printer", "ConsoleWriter", "InputStreamWriter only", "A", "Java"));

		q.add(new Question("Which method compares two strings ignoring case?", "equals()", "equalsIgnoreCase()", "compare()", "match()", "B", "Java"));
		q.add(new Question("Which keyword is used to create a subclass?", "extends", "inherits", "subclass", "super", "A", "Java"));
		q.add(new Question("Which of these is used to prevent method overriding?", "static", "final", "private", "protected", "B", "Java"));
		q.add(new Question("Which of these cannot be overridden?", "final method", "static method", "private method", "All of the above", "D", "Java"));
		q.add(new Question("Which keyword is used to declare an abstract class?", "abstract", "interface", "virtual", "override", "A", "Java"));
		q.add(new Question("What does 'abstract' method mean?", "Method has a body", "Method has no body", "Method is static", "Method is final", "B", "Java"));
		q.add(new Question("Which of these supports multiple inheritance in Java?", "Classes", "Interfaces", "Abstract classes", "Enums", "B", "Java"));
		q.add(new Question("Which collection is ordered and allows duplicates?", "Set", "List", "Map", "Queue", "B", "Java"));
		q.add(new Question("Which Map implementation maintains insertion order?", "HashMap", "TreeMap", "LinkedHashMap", "Hashtable", "C", "Java"));
		q.add(new Question("Which Set implementation keeps elements sorted?", "HashSet", "TreeSet", "LinkedHashSet", "EnumSet", "B", "Java"));

		q.add(new Question("Which of these is a functional interface?", "Runnable", "List", "Map", "String", "A", "Java"));
		q.add(new Question("Which Java feature supports lambda expressions?", "Generics", "Streams", "Functional interfaces", "Annotations only", "C", "Java"));
		q.add(new Question("Which Stream method transforms elements?", "filter()", "map()", "reduce()", "collect()", "B", "Java"));
		q.add(new Question("Which Stream method selects elements based on a predicate?", "filter()", "map()", "sorted()", "peek()", "A", "Java"));
		q.add(new Question("Which terminal operation collects results into a List?", "collect(Collectors.toList())", "map()", "filter()", "limit()", "A", "Java"));
		q.add(new Question("What does OOP stand for?", "Object Oriented Programming", "Object Operating Process", "Open Object Protocol", "Optional Object Programming", "A", "Java"));
		q.add(new Question("Which is NOT an OOP principle?", "Encapsulation", "Inheritance", "Compilation", "Polymorphism", "C", "Java"));
		q.add(new Question("Encapsulation is achieved by:", "Using getters/setters", "Using public fields only", "Avoiding classes", "Using only static methods", "A", "Java"));
		q.add(new Question("Polymorphism means:", "Many forms", "One class only", "No inheritance", "No interfaces", "A", "Java"));
		q.add(new Question("Overloading means:", "Same name, different parameters", "Same name, same parameters", "Different name, same parameters", "Same class only", "A", "Java"));

		q.add(new Question("Which annotation marks a class as a JPA entity?", "@Entity", "@Table", "@Id", "@GeneratedValue", "A", "Java"));
		q.add(new Question("Which annotation marks a primary key?", "@Id", "@Entity", "@Column", "@Table", "A", "Java"));
		q.add(new Question("Which JPA annotation auto-generates IDs?", "@GeneratedValue", "@Id", "@Entity", "@JoinColumn", "A", "Java"));
		q.add(new Question("Which JDBC driver is used for MySQL?", "mysql-connector-j", "postgresql-driver", "h2-driver", "sqlite-jdbc", "A", "Java"));
		q.add(new Question("Which keyword is used to import a package?", "include", "import", "using", "require", "B", "Java"));
		q.add(new Question("Which package contains List interface?", "java.lang", "java.util", "java.io", "java.net", "B", "Java"));
		q.add(new Question("Which of these is a checked exception?", "RuntimeException", "NullPointerException", "IOException", "ArithmeticException", "C", "Java"));
		q.add(new Question("Which of these is used to define a package?", "package", "namespace", "module", "import", "A", "Java"));
		q.add(new Question("Which is true about 'finally' block?", "Runs only if exception occurs", "Always runs (except JVM crash)", "Runs only if no exception", "Never runs", "B", "Java"));
		q.add(new Question("Which method is used to get the length of an array?", "size()", "length()", "length", "count()", "C", "Java"));

		q.add(new Question("Which method is used to get the length of a String?", "length()", "size()", "count()", "getLength()", "A", "Java"));
		q.add(new Question("Which keyword is used to create an object reference that cannot be reassigned?", "final", "static", "const", "volatile", "A", "Java"));
		q.add(new Question("Which keyword ensures visibility of changes across threads?", "volatile", "final", "static", "transient", "A", "Java"));
		q.add(new Question("Which keyword prevents serialization of a field?", "volatile", "transient", "static", "final", "B", "Java"));
		q.add(new Question("Which method converts a String to int?", "Integer.parseInt()", "String.toInt()", "parse(int)", "toInteger()", "A", "Java"));
		q.add(new Question("Which class is a wrapper for int?", "Integer", "Int", "Number", "WrapperInt", "A", "Java"));
		q.add(new Question("Which is an example of autoboxing?", "Integer x = 5;", "int x = new Integer(5);", "Integer x = Integer.valueOf(\"5\");", "int x = Integer.parseInt(\"5\");", "A", "Java"));
		q.add(new Question("Which method is used to compare objects for equality (by value) commonly?", "==", "equals()", "hashCode()", "compareTo()", "B", "Java"));
		q.add(new Question("Which interface is used to sort objects naturally?", "Comparable", "Comparator", "Iterable", "Serializable", "A", "Java"));
		q.add(new Question("Which interface provides custom sorting logic?", "Comparable", "Comparator", "Collection", "Runnable", "B", "Java"));

		q.add(new Question("Which class is used to format dates/times in modern Java?", "DateFormat", "SimpleDateFormat", "java.time.format.DateTimeFormatter", "Calendar", "C", "Java"));
		q.add(new Question("Which package introduced modern date/time API?", "java.util.time", "java.time", "java.date", "java.calendar", "B", "Java"));
		q.add(new Question("Which collection is best for fast key-based lookup?", "ArrayList", "HashMap", "LinkedList", "Stack", "B", "Java"));
		q.add(new Question("Which collection is best for FIFO operations?", "Queue", "Set", "Map", "TreeSet", "A", "Java"));
		q.add(new Question("Which class implements Queue?", "ArrayDeque", "HashSet", "HashMap", "ArrayList", "A", "Java"));
		q.add(new Question("Which statement is used in switch (Java 17 classic) to exit a case?", "break", "continue", "return", "stop", "A", "Java"));
		q.add(new Question("Which keyword is used to create an enum?", "enum", "enumeration", "Enum", "type", "A", "Java"));
		q.add(new Question("Which of these is true about enums?", "They can have fields and methods", "They cannot have constructors", "They cannot implement interfaces", "They are not classes", "A", "Java"));
		q.add(new Question("What is the parent class of all Java classes?", "Object", "Class", "Base", "Root", "A", "Java"));
		q.add(new Question("Which method is used to convert an object to String representation?", "toString()", "asString()", "string()", "print()", "A", "Java"));

		q.add(new Question("Which annotation is used to override a method?", "@Override", "@Overload", "@OverrideMethod", "@Overwrite", "A", "Java"));
		q.add(new Question("Which annotation in Spring marks a class as a controller for web pages?", "@Controller", "@RestController", "@Service", "@Component", "A", "Java"));
		q.add(new Question("Which annotation maps HTTP GET requests in Spring MVC?", "@GetMapping", "@PostMapping", "@RequestBody", "@ResponseBody", "A", "Java"));
		q.add(new Question("Which annotation maps HTTP POST requests in Spring MVC?", "@PostMapping", "@GetMapping", "@RequestParam", "@PathVariable", "A", "Java"));
		q.add(new Question("Which annotation binds form fields to an object in Spring MVC?", "@ModelAttribute", "@RequestBody", "@Autowired", "@Bean", "A", "Java"));
		q.add(new Question("Which annotation is used for validating request objects?", "@Valid", "@Test", "@Mock", "@Value", "A", "Java"));
		q.add(new Question("Which JPA repository interface provides CRUD methods?", "JpaRepository", "HttpRepository", "JdbcRepository", "CrudController", "A", "Java"));
		q.add(new Question("Which file typically configures Spring Boot properties?", "application.properties", "settings.xml", "pom.properties", "config.json", "A", "Java"));
		q.add(new Question("Which HTTP status indicates success for a GET request?", "200", "404", "500", "301", "A", "Java"));
		q.add(new Question("Which HTTP status indicates 'Not Found'?", "200", "401", "403", "404", "D", "Java"));

		q.add(new Question("Which SQL clause is used to filter rows?", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "A", "Java"));
		q.add(new Question("Which SQL statement is used to insert data?", "INSERT", "UPDATE", "DELETE", "SELECT", "A", "Java"));
		q.add(new Question("Which Spring Boot dependency is commonly used for JPA?", "spring-boot-starter-data-jpa", "spring-boot-starter-mail", "spring-boot-starter-batch", "spring-boot-starter-cache", "A", "Java"));
		q.add(new Question("Which Spring Boot dependency is commonly used for server-side templates?", "spring-boot-starter-thymeleaf", "spring-boot-starter-amqp", "spring-boot-starter-aop", "spring-boot-starter-cache", "A", "Java"));
		q.add(new Question("Which annotation creates a Spring-managed service class?", "@Service", "@Entity", "@Id", "@ControllerAdvice", "A", "Java"));
		q.add(new Question("Which collection allows null keys?", "HashMap", "Hashtable", "ConcurrentHashMap", "TreeMap (always)", "A", "Java"));
		q.add(new Question("Which collection is synchronized (legacy) and does not allow null keys/values?", "Hashtable", "HashMap", "LinkedHashMap", "TreeMap", "A", "Java"));
		q.add(new Question("Which keyword creates a child class of an existing class?", "extends", "implements", "inherit", "super", "A", "Java"));
		q.add(new Question("Which is true about '==' with objects?", "Compares references", "Compares contents always", "Calls equals()", "Compares hashCode()", "A", "Java"));

		q.add(new Question("Which Java feature ensures type safety at compile time for collections?", "Generics", "Reflection", "Annotations", "Serialization", "A", "Java"));
		q.add(new Question("Which keyword is used to create a thread-safe block?", "synchronized", "locked", "mutex", "atomic", "A", "Java"));
		q.add(new Question("Which class provides atomic integers?", "AtomicInteger", "SafeInteger", "ThreadInteger", "VolatileInt", "A", "Java"));
		q.add(new Question("Which method waits for a thread to die?", "join()", "wait()", "sleep()", "notify()", "A", "Java"));
		q.add(new Question("Which method pauses the current thread for a time?", "sleep()", "wait()", "join()", "pause()", "A", "Java"));
		q.add(new Question("Which method is used to read a file in Java NIO?", "Files.readString()", "File.read()", "Reader.readFile()", "Stream.readAll()", "A", "Java"));
		q.add(new Question("Which is a marker interface?", "Serializable", "Runnable", "Callable", "Comparable", "A", "Java"));
		q.add(new Question("Which statement handles multiple alternatives?", "switch", "if only", "try", "catch", "A", "Java"));
		q.add(new Question("Which keyword is used to create a package-private class member?", "No modifier", "private", "public", "protected", "A", "Java"));
		q.add(new Question("Which of these is a valid Java identifier?", "_count", "2count", "count-2", "count 2", "A", "Java"));

		// Ensure we have at least 100 questions
		while (q.size() < 105) {
			int n = q.size() + 1;
			q.add(new Question(
					"Java quick check #" + n + ": Which keyword is used to define a class?",
					"class",
					"struct",
					"define",
					"type",
					"A",
					"Java"
			));
		}
		return q;
	}
}
