package osp.Memory;
import java.util.*;
import osp.IFLModules.Event;
import osp.IFLModules.IflPageFaultHandler;
import osp.IFLModules.SystemEvent;
import osp.Tasks.TaskCB;
import osp.Threads.ThreadCB;
import osp.Utilities.*;
import java.lang.*;
/**
The page fault handler is responsible for handling a page
fault.  If a swap in or swap out operation is required, the page fault
handler must request the operation.

@OSPProject Memory
 */
public class PageFaultHandler extends IflPageFaultHandler
{
       
	/**
    This method handles a page fault. 

    It must check and return if the page is valid, 

    It must check if the page is already being brought in by some other
thread, i.e., if the page's has already pagefaulted
(for instance, using getValidatingThread()).
    If that is the case, the thread must be suspended on that page.

    If none of the above is true, a new frame must be chosen 
    and reserved until the swap in of the requested 
    page into this frame is complete. 

Note that you have to make sure that the validating thread of
a page is set correctly. To this end, you must set the page's
validating thread using setValidatingThread() when a pagefault
happens and you must set it back to null when the pagefault is over.

    If a swap-out is necessary (because the chosen frame is
    dirty), the victim page must be dissasociated 
    from the frame and marked invalid. After the swap-in, the 
    frame must be marked clean. The swap-ins and swap-outs 
    must are preformed using regular calls read() and write().

    The student implementation should define additional methods, e.g, 
    a method to search for an available frame.

Note: multiple threads might be waiting for completion of the
page fault. The thread that initiated the pagefault would be
waiting on the IORBs that are tasked to bring the page in (and
to free the frame during the swapout). However, while
pagefault is in progress, other threads might request the same
page. Those threads won't cause another pagefault, of course,
but they would enqueue themselves on the page (a page is also
an Event!), waiting for the completion of the original
pagefault. It is thus important to call notifyThreads() on the
page at the end -- regardless of whether the pagefault
succeeded in bringing the page in or not.

    @param thread the thread that requested a page fault
    @param referenceType whether it is memory read or write
    @param page the memory page 

@return SUCCESS is everything is fine; FAILURE if the thread
dies while waiting for swap in or swap out or if the page is
already in memory and no page fault was necessary (well, this
shouldn't happen, but...). In addition, if there is no frame
that can be allocated to satisfy the page fault, then it
should return NotEnoughMemory

    @OSPProject Memory
	 */
  static int count=0;
  static int flag=0;
	public static int do_handlePageFault(ThreadCB thread, int referenceType, PageTableEntry page)
	{
      TaskCB Task = thread.getTask();
		  if(page.isValid())
		  {
			   return FAILURE;
		  }

      count++;
      MyOut.print("osp.Memory.PageFaultHadler","Total number of Page Faults  " + count);
      MyOut.print("osp.Memory.PageFaultHadler","Total frames" + MMU.getFrameTableSize());
		  FrameTableEntry frame = null;
      for(int i=0; i<MMU.getFrameTableSize(); i++)
      {
        frame=MMU.getFrame(i);
        if((frame.getPage() == null) && (!frame.isReserved()) && (frame.getLockCount() == 0) ||
					(!frame.isDirty()) && (!frame.isReserved()) && (frame.getLockCount() == 0) ||
					(!frame.isReserved()) && (frame.getLockCount() == 0))
        {
          flag = 1;
          break;
        }
                         
      }
		  //No Replacement Algorithm
      // if(flag==0) 
      // {
      //   frame=MMU.getFrame(0);
      // }
      //FIFO Replacement Algorithm
	    // if(flag==0)
     //  {
     //    frame=getFrameFifo();
     //  }
      //LRU Replacement Algorithm
      if(flag==0)
      {
        frame=getFrameLRU();
      } 
		  if(frame==null)
		  {
        return NotEnoughMemory;
		  }
      SystemEvent event = new SystemEvent("PageFault");
		  thread.suspend(event);
      page.setValidatingThread(thread);
		  frame.setReserved(thread.getTask());

		  if (frame.getPage() != null)
		  {
			   PageTableEntry nPage = frame.getPage();
			   if (frame.isDirty())
			   {
            frame.getPage().getTask().getSwapFile().write(frame.getPage().getID(),
						frame.getPage(), thread);

				    if (thread.getStatus() == ThreadKill) 
            {
					     page.notifyThreads();
					     event.notifyThreads();
					     ThreadCB.dispatch();
					     return FAILURE;
				    }
				    frame.setDirty(false);
			   }
			   frame.setReferenced(false);
			   frame.setPage(null);
			   nPage.setValid(false);
			   nPage.setFrame(null);
		  }

		  page.setFrame(frame);
		  thread.getTask().getSwapFile().read(frame.getID(), page, thread); 
		  if(thread.getStatus() == ThreadKill)
		  {
			   page.notifyThreads();
			   page.setValidatingThread(null);
			   page.setFrame(null);
			   event.notifyThreads();
			   ThreadCB.dispatch();
			   return FAILURE;
		  }

		  frame.setPage(page);
		  page.setValid(true);
      if(frame.getReserved() == Task)
		  {
			   frame.setUnreserved(Task);    		
		  }
      
      page.notifyThreads();
		  event.notifyThreads();
		  ThreadCB.dispatch();
		  return SUCCESS;
	}
  
  //FIFO Replacement Algorithm
  // private static FrameTableEntry getFrameFifo()
  // {
  //     long max=0;
  //     FrameTableEntry frame=null;
  //     for(int i=0;i<MMU.getFrameTableSize();i++)
  //     { 
  //         PageTableEntry page = MMU.getFrame(i).getPage();
  //         long time = System.currentTimeMillis() - page.createTime;
  //         if(time>max)
  //         {
  //           max = time;
  //           frame=MMU.getFrame(i);
  //         }
  //     }
  //     return frame;
  // }

  //LRU Replacement Algorithm
  private static FrameTableEntry getFrameLRU()
  {
      long max=0;
      FrameTableEntry frame=null;
                   
      for(int i=0;i<MMU.getFrameTableSize();i++)
      {
        PageTableEntry page = MMU.getFrame(i).getPage();
        long time = System.currentTimeMillis() - page.refTime;
        if(time>max)
        {
            max = time;
            frame=MMU.getFrame(i);
        }
      }
      return frame;
  }
}

