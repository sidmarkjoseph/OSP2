package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;
import java.util.Queue;
import java.util.LinkedList;

public class PageTable extends IflPageTable
{
    int size=0;
    /** 
  
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
      super(ownerTask);
      size=(int) Math.pow(2,MMU.getPageAddressBits());
      pages=new PageTableEntry[size];
      for(int i=0;i<size;i++)
      {
          pages[i]= new PageTableEntry(this,i);
      }
    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
      int s=MMU.getFrameTableSize();
      for(int i=0;i<s;i++)
      {
          FrameTableEntry frame = MMU.getFrame(i);
          PageTableEntry page = frame.getPage();
          if(page!=null && getTask()==page.getTask())
          {
              frame.setPage(null);
              frame.setDirty(false);
              frame.setReferenced(false);
              if(frame.getReserved()==getTask())
              {
                frame.setUnreserved(getTask());
              }
          }
      }
    }
    /*
       Feel free to add methods/fields to improve the readability of your code
    */
}



/*
      Feel free to add local classes to improve the readability of your code
*/
