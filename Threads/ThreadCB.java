package osp.Threads;
import java.util.Vector;
import java.util.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{
    
    /**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
    static int min=1;
    static int max=1000;
    static double throughput;
    static long timer1;
    static long timer2;
    static double difftime;
    static long timer3;
    static long timer4;
    static double difftime1; 
    static GenericList readyq;
    static int number_of_threads;
  
    public ThreadCB()
    {
       super();
        // your code goes here

    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    public static void init()
    {
       
       number_of_threads=0;
       readyq=new GenericList();
        // your code goes here

    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null
   
        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
        int result,count;
      
       
        count=task.getThreadCount();
        if(count>=MaxThreadsPerTask)
        {
               ThreadCB.dispatch();
                return null;
        }
        
        if(number_of_threads==0)
        timer1=System.currentTimeMillis();
        ThreadCB thread1=new ThreadCB();
        number_of_threads++;
        MyOut.print("osp.Threads.ThreadCB","Total Number of threads "+ Integer.toString(number_of_threads));
        result=task.addThread(thread1);
        if(result==FAILURE)
             return null; 
        thread1.setTask(task);
        Random random = new Random();
        int randomNumber = random.nextInt(max - min) + min;
        System.out.println(randomNumber);
        thread1.setPriority(randomNumber);
        thread1.setStatus(ThreadReady);
        readyq.append(thread1);
        ThreadCB.dispatch();
        return thread1;
        
        // your code goes here

    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    {
        
                
               
      int status,i;
      PageTable page;
      TaskCB task;
      status=this.getStatus();
      if(status==ThreadReady)
      {
          ThreadCB.readyq.remove(this);
      }
      else if(status==ThreadRunning)
      {
         page=MMU.getPTBR();
         task=page.getTask();
         MMU.setPTBR(null);
         task.setCurrentThread(null);
                  
       }
       else
       {
                      
        }
        this.setStatus(ThreadKill);
        for(i=0;i<Device.getTableSize();i++)
        {
          Device.get(i).cancelPendingIO(this);
          }
         ResourceCB.giveupResources(this);
         task=this.getTask();
         timer2=System.currentTimeMillis();
         Long t=(timer2-timer1);
         difftime=t.doubleValue();
         MyOut.print("osp.Threads.ThreadCB","Number of Threads "+ number_of_threads);
         throughput=number_of_threads/difftime;
         MyOut.print("osp.Threads.ThreadCB","ThroughPut " + throughput);
         task.removeThread(this);
         int num=task.getThreadCount();
         if(num==0)
            task.kill();
         ThreadCB.dispatch();

        // your code goes here

    }

    /** Suspends the thread that is currenly on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {
         
        PageTable page;
        TaskCB task;
        switch(this.getStatus())
        {
           case ThreadRunning:
            {
             this.setStatus(ThreadWaiting);
             page=MMU.getPTBR();
             task=page.getTask();
             MMU.setPTBR(null);
             task.setCurrentThread(null);
             event.addThread(this);
             break;
            } 
           default:
            {
             this.setStatus(this.getStatus()+1);
             event.addThread(this);
             break;
            }
        }
       ThreadCB.dispatch();
             
        // your code goes here

    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
      
      switch(this.getStatus())
      {
          case ThreadReady:
          case ThreadRunning:
              return;
          case ThreadWaiting:

           {
               this.setStatus(ThreadReady);
               ThreadCB.readyq.append(this);
               break;
            }
           default:
            {
               this.setStatus(this.getStatus()-1);
               break;
            }
               // your code goes here

       }
       ThreadCB.dispatch();
     }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one theread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    /*public static int do_dispatch()
    {
        
        String number;
        ThreadCB runningthread=null,currentthread;
        try
       { 
        runningthread=MMU.getPTBR().getTask().getCurrentThread();
       }catch(NullPointerException e)//If there is no running thread
       {
            
          }
       if(runningthread!=null)//there is a running thread
       {
            runningthread.getTask().setCurrentThread(null); 
	    MMU.setPTBR(null);
	   runningthread.setStatus(ThreadReady); 
	   readyq.append(runningthread);
       }
       
           
        if(readyq.isEmpty()) { //running thread but no ready thread and no running with no empty thread
	MMU.setPTBR(null);
	return FAILURE; 
	}

	else {//running thread and empty thread
	currentthread = (ThreadCB) readyq.removeHead(); 
	MMU.setPTBR(currentthread.getTask().getPageTable());
        currentthread.getTask().setCurrentThread(currentthread);
	currentthread.setStatus(ThreadRunning);

	}
        //Wihtout timer it FCFS and with timer it is Round Robin
      
	HTimer.set(2); //sets the timer for the nex dispatch or interrupt after 50ms
	return SUCCESS;
   }*/
   public static int do_dispatch()
    {
        ThreadCB runningthread=null;
        try
       { 
        runningthread=MMU.getPTBR().getTask().getCurrentThread();
       }catch(NullPointerException e)//If there is no running thread
       {
          }
        
        if(readyq.isEmpty()) { //running thread but no ready thread and no running with no empty thread
	return FAILURE; 
	} 
        if(runningthread!=null)    
       {
            int prior=runningthread.getPriority();
            ThreadCB thread=null;
	    Enumeration number=readyq.forwardIterator();
            while(number.hasMoreElements()){
                  thread=(ThreadCB)number.nextElement();       
                  int priority=thread.getPriority();
                  if(priority>=prior)
                       break;
                   }
            MMU.setPTBR(thread.getTask().getPageTable());
            thread.getTask().setCurrentThread(thread);
	    thread.setStatus(ThreadRunning);
            return SUCCESS;
            
        }
         return SUCCESS;
     }
                          
     
               
             
        
         

    

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
     */
    public static void atWarning()
    {
        // your code goes here

    }
    

    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
