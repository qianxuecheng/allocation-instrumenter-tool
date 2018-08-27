package com.google.monitoring.runtime.instrumentation.recorders;

import com.google.monitoring.runtime.instrumentation.InstrumentationProperties;
import com.google.monitoring.runtime.instrumentation.events.AllocationEvent;
import com.google.monitoring.runtime.instrumentation.events.EventParser;

import java.io.*;
import java.time.LocalTime;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pulls AllocationEvent's off of the queue and outputs the data in the desired form.
 *
 * Created by jmaloney on 5/23/2016.
 */
public class FlamePrinter extends Thread {
    private BlockingQueue<AllocationEvent> queue;
    private Writer writer;
    private final EventParser.VerbosityLevel verbosityLevel;
    private AtomicInteger count=new AtomicInteger();
    private LocalTime createStackTime=LocalTime.now();
    private InstrumentationProperties properties;

    public FlamePrinter(final InstrumentationProperties properties) throws FileNotFoundException, UnsupportedEncodingException {
       this.properties=properties;
        writer = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(properties.outputPath()+count.getAndIncrement()), "utf-8"));
        verbosityLevel = properties.verbosityLevel();
    }

    public void close() throws IOException {
        writer.close();
    }

    private void process(final AllocationEvent event){
        final String entry = EventParser.parseEvent(event, verbosityLevel);
        if (!entry.equals("")) {
            try {
                writer.write(entry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void newWriter() {
        try {
            if (createStackTime.plusHours(1).isBefore(LocalTime.now())) {
                writer.close();
                createStackTime = LocalTime.now();
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(properties.outputPath() + count.getAndIncrement()), "utf-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void setQueue(final BlockingQueue<AllocationEvent> queue){
        this.queue = queue;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("FlamePrinter");
        for(;;){
            try {
                final AllocationEvent event = queue.poll(1, TimeUnit.MILLISECONDS);
                if (event != null&&!isIgnore()) {
                    process(event);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isIgnore() {
        try {
            if (new File("/tmp/allocation.flag").exists()) {
                return false;
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }
}
