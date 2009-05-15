/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.common.scheduler;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.servicemix.common.scheduler.Scheduler;
import org.apache.servicemix.common.scheduler.ScheduleIterator;
import org.apache.servicemix.common.scheduler.SchedulerTask;

public class SchedulerTest extends TestCase {

    private static transient Log logger =  LogFactory.getLog(SchedulerTest.class);

    // Tests Scheduling a task
    public void testSchedule() throws Exception {
        Scheduler scheduler = new Scheduler();        
        MySchedulerTask task = new MySchedulerTask(1);
        MyScheduleIterator iter = new MyScheduleIterator();
        MySchedulerTask task2 = new MySchedulerTask(0);
        MyScheduleIterator2 iter2 = new MyScheduleIterator2();
        scheduler.schedule(task,iter);
            
        try {
            scheduler.schedule(task,iter);
            fail();
        } catch (java.lang.IllegalStateException e) {
            //should catch this Fault
        }
                        
        scheduler.schedule(task2,iter2);            
        scheduler.cancel();           
    }
    
    // Tests cancel a task
    public void testCancel() throws Exception {
            
        MySchedulerTask task = new MySchedulerTask(1);     
        MyScheduleIterator iter = new MyScheduleIterator();
        Scheduler scheduler = new Scheduler(false);            
        scheduler.schedule(task,iter);           
    }
    
    // Tests running a task 
    public void testRun() throws Exception {
    	MySchedulerTask task = new MySchedulerTask(2);     
        MyScheduleIterator iter = new MyScheduleIterator();
        Scheduler scheduler = new Scheduler(false);            
        scheduler.schedule(task,iter);        
    }

        
   public class MyScheduleIterator implements ScheduleIterator {

           public Date nextExecution() {                
               DateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd");
               java.util.Date date = new java.util.Date();
               return date;
           }
   }
   
   public class MyScheduleIterator2 implements ScheduleIterator {

       public Date nextExecution() {
    	   return null;           
       }
   }

   // Need to extend abstract SchedulerTask class
   public class MySchedulerTask extends SchedulerTask {
	
	private int counter = 0;	
	long scheduleExecTime = 0;
		
	public MySchedulerTask(int counter) {		
		this.counter = counter;
	}

	@Override
	public void run() {
		switch(counter) {
		case 0:			
			logger.info("Hello");
			scheduleExecTime = this.scheduledExecutionTime();
			break;
		case 1:
			logger.info("Hellow");
			scheduleExecTime = this.scheduledExecutionTime();
			logger.info("schedulExecTime 1 = " + scheduleExecTime);
			this.cancel();			
		case 2:
			logger.info("HelloT");
			this.timerTask = null;
			scheduleExecTime = this.scheduledExecutionTime();
			logger.info("schedulExecTime 2 = " + scheduleExecTime);
			this.cancel();
		}
	}
}
    
    
    
}
