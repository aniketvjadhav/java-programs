import java.util.concurrent.*;
import java.util.*;

class MySemaphores
{
	private static int custNumberQueue[];									// queue for storing customer number
	private static String custActivityQueue[];								// queue for storing customer activity

	private static Semaphore semFinished [];								// semaphore array for fair treatment to customers
	private static Semaphore semCreateCustomer	= new Semaphore (1,true);	// allowing only one customer to get created at one time.
	private static Semaphore semStoreCapacity	= new Semaphore (10,true);	// total capacity of the store
	private static Semaphore semCustomerReady	= new Semaphore (0,true);	// semaphore to tell whether customer is ready or not to the postal worker
	private static Semaphore semMutex1			= new Semaphore (1,true);	// mutual exclusion for queue operations
	private static Semaphore semWorkerFree		= new Semaphore (3,true);	// check whether the worker is free or not
	private static Semaphore semScale			= new Semaphore (1,true);	// only one scale as resource

	private static int enqueue1=0, dequeue1=0, enqueue2=0, dequeue2=0;		// queue handlers
	
	
	// Create a method which will create array of total number of customers 
	public static void createFairCustomers(int i)
	{
		semFinished = new Semaphore[i];

		for (int j=0; j<i; j++)
		{
			semFinished[j] = new Semaphore(0,true);
		}
	}

	// semaphore operations for customer ready 

	public static void customerReadyWait() throws InterruptedException
	{
		semCustomerReady.acquire();
	}

	public static void customerReadySignal()
	{
		semCustomerReady.release();
	}
	
	// creating a queue for customer number of desired length
	public static void createCustNumberQueue(int i)
	{
		custNumberQueue = new int[i];
	}

	// creating a queue for customer activity of desired length
	public static void createCustActivityQueue(int i)
	{		
		custActivityQueue = new String[i];
		for (int j=0; j<i; j++)
		{
			custActivityQueue[j] = new String();
		}
	}
	
	// adding the customer number to the queue
	public static void enqueueCustomerNumber(int i)
	{
		custNumberQueue[enqueue1++] = i;
	}
	
	// adding the customer activity to the queue
	public static void enqueueCustomerActivity(String activity)
	{		
		custActivityQueue[enqueue2++] = activity;
	}

	// removing the customer number from the queue
	public static int dequeueCustomerNumber()
	{
		return custNumberQueue[dequeue1++];
	}
	
	// removing the customer activity from the queue
	public static String dequeueCustomerActivity()
	{	
		return custActivityQueue[dequeue2++];
	}

	// create customers in order
	public static void createCustomerWait() throws InterruptedException
	{
		semCreateCustomer.acquire();
	}
	
	public static void releaseCustomerSignal()
	{
		semCreateCustomer.release();
	}

	// enter customer in the store. only 10 are allowed.
	public static void enterCustomerWait() throws InterruptedException
	{
		semStoreCapacity.acquire();
	}
	
	// exit semaphore so that new customer can enter the store
	public static void exitCustomerSignal()
	{
		semStoreCapacity.release();
	}
	
	// worker free to indicates the new customer that a worker is free. if not free then wait
	public static void workerFreeWait() throws InterruptedException
	{
		semWorkerFree.acquire();
	}

	public static void workerFreeSignal()
	{
		semWorkerFree.release();
	}

	// create mutual exclusion semaphore
	public static void queue1Wait() throws InterruptedException
	{
		semMutex1.acquire();
	}
	
	public static void queue1Signal()
	{
		semMutex1.release();
	}

	// customer waits for the postal worker service permission
	public static void askServiceWait(int i) throws InterruptedException
	{
		semFinished[i].acquire();
	}
	
	// giving the service permission to the customer
	public static void askServiceSignal(int i)
	{
		semFinished[i].release();
	}
	
	// waiting for the customer to put the activity on the queue
	public static void performActivityWait(int i) throws InterruptedException
	{
		semFinished[i].acquire();
	}
	
	// indicating that customer has put the activity on the queue
	public static void performActivitySignal(int i)
	{
		semFinished[i].release();
	}
	
	//waits till the activity of the worker is finished.
	public static void finishActivityWait(int i) throws InterruptedException
	{
		semFinished[i].acquire();
	}

	// signaling the customer that postal worker has finished performing the activity
	public static void finishActivitySignal(int i)
	{
		semFinished[i].release();
	}
	
	// waits till customer has ended his activity.
	public static void finishCustomerActivityWait(int i) throws InterruptedException
	{
		semFinished[i].acquire();
	}
	
	// customer tells that its activity has finished
	public static void finishCustomerActivitySignal(int i)
	{
		semFinished[i].release();
	}

	// semaphore for scale resource
	public static void scaleWait() throws InterruptedException
	{
		semScale.acquire();
	}
	
	public static void scaleSignal()
	{
		semScale.release();
	}

}


class PostOfficeCustomers implements Runnable
{
	private	int i;
	private String activity;
	
	
	PostOfficeCustomers (int i, String activity) // constructor for defining the customer number and his activities
	{
		this.i = i;
		this.activity = activity;
	}
	
	void customerCreated()
	{
		System.out.println("Customer " + this.i + " created and wants to " + this.activity);
	}

	void customerEnter()
	{
		System.out.println("Customer " + this.i + " enters post office");
	}

	void exitPostOffice()
	{
		System.out.println("Customer " + this.i + " leaves post office");
	}

	void finishActivity()
	{
		String message = new String();
		if (this.activity.equalsIgnoreCase("buy stamps"))
		{
			message = "buying stamps";
		}

		if (this.activity.equalsIgnoreCase("mail a letter"))
		{
			message = "mailing a letter";
		}
		
		if (this.activity.equalsIgnoreCase("mail a package"))
		{
			message = "mailing a package";
		}

		System.out.println("Customer " + this.i + " finished " + message);
	}

	public void run()
	{
		try
		{
			customerCreated();											// create the customer
			MySemaphores.releaseCustomerSignal();						// signal to the main method that customer has created

			MySemaphores.enterCustomerWait();							// wait for entering the store as only 10 customers can enter
			customerEnter();											// customer entering if the semaphore permits
			
			MySemaphores.workerFreeWait();								// check whether worker is free or not. if free proceed
	
			MySemaphores.queue1Wait();									// mutual exclusion for editing the queue
			MySemaphores.enqueueCustomerNumber(this.i);					// adding the customer number to custNumberQueue
			MySemaphores.customerReadySignal();							// customer signals the ready signal to the worker
			MySemaphores.queue1Signal();								// releasing the mutual exclusion
			
			MySemaphores.askServiceWait(this.i);						// customer waits for the postal worker service permission
			
			MySemaphores.queue1Wait();									// mutual exclusion for editing the queue
			MySemaphores.enqueueCustomerActivity(this.activity);		// adding the customer activity to custActivityQueue
			MySemaphores.performActivitySignal(this.i);					// tells worker to provide service
			MySemaphores.queue1Signal();								// releases the mutual exclusion
			
			MySemaphores.finishActivityWait(this.i);					// waits till the activity of the worker is finished.
			finishActivity();											// after worker finishes, the customer will end his activity
			MySemaphores.finishCustomerActivitySignal(this.i);			// signaling that customer's activity has completed
			MySemaphores.workerFreeSignal();							// signaling worker has completed serving the customer
			exitPostOffice();											// customer is exiting the post office
			MySemaphores.exitCustomerSignal();							// signaling that customer has exited the post office

		}
		catch (InterruptedException e)
		{

		}		
	}
}

class PostOfficeWorker implements Runnable
{
	private	int i;

	PostOfficeWorker (int i)
	{
		this.i = i;
	}

	public void run()
	{
		try
		{
			int customerNumber;
			String customerActivity;
			postWorkerCreated();

			while(true)
			{
				MySemaphores.customerReadyWait();							// waiting for customer to come

				MySemaphores.queue1Wait();									// mutual exclusion for editing the queue
				customerNumber = MySemaphores.dequeueCustomerNumber();		// removing the customer number from the queue
				provideService(this.i, customerNumber);						// provide the service to the customer
				MySemaphores.askServiceSignal(customerNumber);				// giving the service permission to the customer
				MySemaphores.queue1Signal();								// releasing the mutual exclusion

				MySemaphores.performActivityWait(customerNumber);			// waiting for the customer to put the activity on the queue			

				MySemaphores.queue1Wait();									// mutual exclusion for editing the queue
				customerActivity = MySemaphores.dequeueCustomerActivity();	// removing the customer activity from the queue
				performActivity(this.i, customerNumber, customerActivity);	// perform activity on the customer's request
				MySemaphores.queue1Signal();								// releasing the mutual exclusion
 
				finishedActivity(this.i, customerNumber);					// postal worker has finished performing the said activity
				MySemaphores.finishActivitySignal(customerNumber);			// signaling the customer that postal worker has finished performing the activity
				MySemaphores.finishCustomerActivityWait(customerNumber);	// waits till customer has ended his activity.
			}
		}
		catch (Exception e)
		{
		}
	}

	void postWorkerCreated()
	{
		System.out.println("Postal worker " + this.i + " created");
	}

	void provideService(int worker, int customer)
	{
		System.out.println("Postal worker " + worker + " serving customer " + customer);
	}

	void performActivity(int worker, int customer, String activity) throws InterruptedException
	{

		System.out.println("Customer " + customer + " asks postal worker " + worker + " to " + activity);

		if (activity.equalsIgnoreCase("buy stamps"))
		{
			//Thread.sleep(1000);
			Thread.sleep(100);
		}

		if (activity.equalsIgnoreCase("mail a letter"))
		{
			//Thread.sleep(1500);
			Thread.sleep(150);
		}
		
		if (activity.equalsIgnoreCase("mail a package"))
		{
			MySemaphores.scaleWait();
			System.out.println("Scales in use by postal worker " + worker);
			//Thread.sleep(2000);
			Thread.sleep(200);
			System.out.println("Scales released by postal worker " + worker);
			MySemaphores.scaleSignal();
		}

	}

	void finishedActivity(int worker, int customer)
	{
		System.out.println("Postal worker " + worker + " finished serving customer " + customer);
	}

}

public class TestPostOffice
{
	static String getRandomActivity()
	{
		String x[] = {"mail a package", "buy stamps", "mail a letter"}; // array of activities
		Random r = new Random();
		int activity = r.nextInt(3); // randomly one number will be generated between 0 and 2
		
		return x[activity]; //activity in the array will be returned.
	}

	public static void main(String[] args) throws InterruptedException
	{
		// Creating constant number of customers and workers
		final int totalCustomers = 50;
		final int totalWorkers = 3;
		
		PostOfficeCustomers c[] = new PostOfficeCustomers[totalCustomers];		// Create Array of objects for Customers
		
		Thread customers[] = new Thread[totalCustomers];				// Create Array of threads for those customers
		PostOfficeWorker w[] = new PostOfficeWorker[totalWorkers];		// Create Array of objects for Workers
		Thread worker[] = new Thread[totalWorkers];						// Create Array of threads for those workers

		// Creating each thread for each customers and starting those threads
		MySemaphores.createFairCustomers(totalCustomers);				// create fair customers
		MySemaphores.createCustNumberQueue(totalCustomers);				// create queue
		MySemaphores.createCustActivityQueue(totalCustomers);			// create activity queue


		String activity = new String();

		for (int i=0; i<totalCustomers; i++)
		{
			MySemaphores.createCustomerWait(); // for maintaining the order of creation.
			c[i] = new PostOfficeCustomers(i,getRandomActivity());
			customers[i] = new Thread(c[i]);
			customers[i].start(); // start customer threads
		}

		// Creating each thread for each worker and starting those threads
		for (int i=0; i<3; i++)
		{
			w[i] = new PostOfficeWorker(i);
			worker[i] = new Thread(w[i]);
			worker[i].start(); //start worker threads
		}

		for( int i = 0; i < totalCustomers; i++ ) 
		{
			try
			{
				customers[i].join(); //join the customer threads after finishing their job.
				System.out.println("Joined customer " + i);
			}
			catch (InterruptedException e)
			{
			}
		}

		for( int i = 0; i < totalWorkers; i++ ) 
		{
			try
			{
				worker[i].join(); //join the worker threads. but there is infinite loop so it wont be joined.
			}
			catch (InterruptedException e)
			{
			}
		}
	}
}
