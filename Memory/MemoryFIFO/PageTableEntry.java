package osp.Memory;

import osp.Threads.ThreadCB;
import osp.Devices.IORB;
import osp.IFLModules.IflPageTableEntry;
import java.util.*;
import java.lang.*;
/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.
   
   @OSPProject Memory

*/

public class PageTableEntry extends IflPageTableEntry
{
    boolean isPageFault = false;
    long createTime;
    long refTime;
	
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);
	   
       as its first statement.

       @OSPProject Memory
    */
    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
      super(ownerPageTable, pageNumber);
      this.createTime = System.currentTimeMillis();
      this.refTime = 0;
    }

    /**
       This method increases the lock count on the page by one. 

	The method must FIRST increment lockCount, THEN  
	check if the page is valid, and if it is not and no 
	page validation event is present for the page, start page fault 
	by calling PageFaultHandler.handlePageFault().

	@return SUCCESS or FAILURE
	FAILURE happens when the pagefault due to locking fails or the 
	that created the IORB thread gets killed.

	@OSPProject Memory
     */
    public int do_lock(IORB iorb)
    {
        if(!isValid())
        {
        	if(getValidatingThread() != null)
          {
        		if(getValidatingThread() != iorb.getThread())
            {
              iorb.getThread().suspend(this);
              if(iorb.getThread().getStatus() == ThreadKill)
              {
                return FAILURE;
              }
              // PageFaultHandler.handlePageFault(iorb.getThread(), MemoryLock, this);       
            }
        	}
          else
          {
            PageFaultHandler.handlePageFault(iorb.getThread(), MemoryLock, this);
        	}
        }
		    
        if(this.getFrame()!=null)
        { 
		      this.getFrame().incrementLockCount();
          return SUCCESS;
        }
        else
        {
          return FAILURE;
        }   
    }

    /** This method decreases the lock count on the page by one. 

	This method must decrement lockCount, but not below zero.

	@OSPProject Memory
    */
    public void do_unlock()
    {
    	 if(this.getFrame().getLockCount() > 0)
       {
    		  this.getFrame().decrementLockCount();
    	 }
    }
}