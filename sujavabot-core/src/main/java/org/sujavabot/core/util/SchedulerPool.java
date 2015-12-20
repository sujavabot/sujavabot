package org.sujavabot.core.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.sujavabot.core.Authorization;

public class SchedulerPool {
	private static SchedulerPool instance = new SchedulerPool();
	
	public static ScheduledExecutorService get() {
		return get(Authorization.getCurrentUser());
	}
	
	public static ScheduledExecutorService get(Object sync) {
		return instance.getScheduler(sync);
	}
	
	private ScheduledExecutorService dispatcher = Executors.newSingleThreadScheduledExecutor();
	private ExecutorService workers = Executors.newCachedThreadPool();
	
	private Map<Object, ScheduledExecutorService> executors = new HashMap<>();
	
	private SchedulerPool() {}
	
	private ScheduledExecutorService getScheduler(Object sync) {
		synchronized(executors) {
			ScheduledExecutorService exec = executors.get(sync);
			if(exec == null)
				executors.put(sync, exec = new SyncExecutor(sync));
			return exec;
		}
	}
	
	private static class PeriodicFutureTask<V> extends FutureTask<V> {
		private boolean periodic;
		private Runnable postReset;
		
		public PeriodicFutureTask(Callable<V> callable, boolean periodic) {
			super(callable);
			this.periodic = periodic;
		}

		public PeriodicFutureTask(Runnable runnable, V result, boolean periodic) {
			super(runnable, result);
			this.periodic = periodic;
		}
		
		public PeriodicFutureTask(Runnable runnable, V result, Runnable postReset) {
			super(runnable, result);
			this.periodic = true;
			this.postReset = postReset;
		}
		
		@Override
		public void run() {
			if(periodic) {
				super.runAndReset();
				if(postReset != null)
					postReset.run();
			} else
				super.run();
		}
		
	}
	
	private class SyncExecutor extends AbstractExecutorService implements ScheduledExecutorService {

		private Object sync;
		private LinkedList<Runnable> tasks = new LinkedList<>();
		private boolean shutdown;
		private boolean terminated;
		
		public SyncExecutor(Object sync) {
			this.sync = sync;
		}
		
		@Override
		public void shutdown() {
			synchronized(tasks) {
				shutdown = true;
			}
		}

		@Override
		public List<Runnable> shutdownNow() {
			synchronized(tasks) {
				shutdown = true;
				terminated = true;
				return tasks;
			}
		}

		@Override
		public boolean isShutdown() {
			synchronized(tasks) {
				return shutdown;
			}
		}

		@Override
		public boolean isTerminated() {
			synchronized(tasks) {
				return terminated;
			}
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			synchronized(tasks) {
				long millis = TimeUnit.MILLISECONDS.convert(timeout, unit);
				long nanos = TimeUnit.NANOSECONDS.convert(timeout, unit) - TimeUnit.NANOSECONDS.convert(millis, TimeUnit.MILLISECONDS);
				if(!terminated)
					tasks.wait(millis, (int) nanos);
				return terminated;
			}
		}

		@Override
		public void execute(Runnable command) {
			synchronized(tasks) {
				if(shutdown)
					throw new RejectedExecutionException();
				tasks.offer(command);
				if(tasks.size() == 1)
					workers.execute(new QueueTask());
			}
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
			return new ScheduledProxyFuture<>(command, delay, unit);
		}

		@Override
		public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
			return new ScheduledProxyFuture<>(callable, delay, unit);
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
			return new ScheduledProxyFuture<>(command, initialDelay, period, unit, true);
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
			return new ScheduledProxyFuture<>(command, initialDelay, delay, unit, true);
		}

		
		private class ScheduledProxyFuture<V> implements ScheduledFuture<V> {

			private ScheduledFuture<?> dispatchFuture;
			private PeriodicFutureTask<V> workerFuture;
			
			public ScheduledProxyFuture(Runnable command, long delay, TimeUnit unit) {
				workerFuture = new PeriodicFutureTask<>(command, null, false);
				dispatchFuture = dispatcher.schedule(() -> execute(workerFuture), delay, unit);
			}
			
			public ScheduledProxyFuture(Callable<V> callable, long delay, TimeUnit unit) {
				workerFuture = new PeriodicFutureTask<>(callable, false);
				dispatchFuture = dispatcher.schedule(() -> execute(workerFuture), delay, unit);
			}
			
			public ScheduledProxyFuture(Runnable command, long initialDelay, long period, TimeUnit unit, boolean fixedRate) {
				if(fixedRate) {
					workerFuture = new PeriodicFutureTask<>(command, null, true);
					dispatchFuture = dispatcher.scheduleAtFixedRate(workerFuture, initialDelay, period, unit);
				} else {
					workerFuture = new PeriodicFutureTask<>(command, null, () -> 
					{ 
						if(!dispatchFuture.isCancelled()) 
							dispatchFuture = dispatcher.schedule(workerFuture, period, unit); 
					});
					dispatchFuture = dispatcher.schedule(workerFuture, initialDelay, unit);
				}
			}
			
			@Override
			public long getDelay(TimeUnit unit) {
				return dispatchFuture.getDelay(unit);
			}

			@Override
			public int compareTo(Delayed o) {
				return dispatchFuture.compareTo(o);
			}

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				dispatchFuture.cancel(mayInterruptIfRunning);
				return workerFuture.cancel(mayInterruptIfRunning);
			}

			@Override
			public boolean isCancelled() {
				return workerFuture.isCancelled();
			}

			@Override
			public boolean isDone() {
				return workerFuture.isDone();
			}

			@Override
			public V get() throws InterruptedException, ExecutionException {
				return workerFuture.get();
			}

			@Override
			public V get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				return workerFuture.get(timeout, unit);
			}
			
			
		}
		
		private class QueueTask implements Runnable {

			private Runnable task;
			
			public QueueTask() {
				synchronized(tasks) {
					this.task = tasks.peek();
				}
			}
			
			@Override
			public void run() {
				String name = Thread.currentThread().getName();
				try {
					Thread.currentThread().setName(name + ": " + sync);
					task.run();
				} finally {
					Thread.currentThread().setName(name);
					synchronized(tasks) {
						tasks.poll();
						if(tasks.size() > 0)
							workers.execute(new QueueTask());
						else if(shutdown)
							terminated = true;
					}
				}
			}
			
		}
		
	}
	
}
